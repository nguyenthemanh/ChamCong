package app.chamcong.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chamcong_v1")

class AttendanceRepository(private val context: Context) {

    private val nameKey = stringPreferencesKey("employee_name")
    private val kanaKey = stringPreferencesKey("employee_kana")
    private val salaryEnabledKey = booleanPreferencesKey("salary_enabled")
    private val flagEnabledKey = booleanPreferencesKey("flag_enabled")
    private val wageKey = longPreferencesKey("daily_wage")
    private val otKey = longPreferencesKey("overtime_hour")
    private val flagBonusKey = longPreferencesKey("flag_bonus")
    private val deductionKey = longPreferencesKey("monthly_deduction")
    private val flagOnKey = stringPreferencesKey("flag_label_on")
    private val flagOffKey = stringPreferencesKey("flag_label_off")
    private val monthsKey = stringPreferencesKey("known_months")
    private val languageKey = stringPreferencesKey("language")
    private val sheetLanguageKey = stringPreferencesKey("sheet_language")
    private val sheetTitleKey = stringPreferencesKey("sheet_title_custom")
    private val setupDoneKey = booleanPreferencesKey("setup_done")

    /** False until first-run setup form is completed. */
    val setupDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[setupDoneKey] ?: false
    }

    val settings: Flow<PaySettings> = context.dataStore.data.map { prefs ->
        val defaults = PaySettings()
        PaySettings(
            employeeName = prefs[nameKey] ?: defaults.employeeName,
            employeeNameKana = prefs[kanaKey] ?: defaults.employeeNameKana,
            salaryEnabled = prefs[salaryEnabledKey] ?: defaults.salaryEnabled,
            flagEnabled = prefs[flagEnabledKey] ?: defaults.flagEnabled,
            dailyWageYen = prefs[wageKey] ?: defaults.dailyWageYen,
            overtimeHourYen = prefs[otKey] ?: defaults.overtimeHourYen,
            flagBonusYen = prefs[flagBonusKey] ?: defaults.flagBonusYen,
            monthlyDeductionYen = prefs[deductionKey] ?: defaults.monthlyDeductionYen,
            flagLabelOn = prefs[flagOnKey] ?: defaults.flagLabelOn,
            flagLabelOff = prefs[flagOffKey] ?: defaults.flagLabelOff,
            language = AppLanguage.fromCode(prefs[languageKey]),
            sheetLanguage = AppLanguage.fromCode(prefs[sheetLanguageKey] ?: "ja"),
            sheetTitleCustom = prefs[sheetTitleKey] ?: "",
        )
    }

    val knownMonths: Flow<List<MonthKey>> = context.dataStore.data.map { prefs ->
        parseMonths(prefs[monthsKey]).ifEmpty { listOf(MonthKey.current()) }
    }

    fun entriesFor(month: MonthKey): Flow<Map<Int, DayEntry>> =
        context.dataStore.data.map { prefs ->
            parseEntries(prefs[monthEntriesKey(month)])
        }

    suspend fun ensureMonth(month: MonthKey) {
        context.dataStore.edit { prefs ->
            val months = parseMonths(prefs[monthsKey]).toMutableSet()
            if (months.add(month)) {
                prefs[monthsKey] = encodeMonths(months.sortedDescending())
            }
        }
    }

    /** Create the month after [from] (real calendar). Returns the new key. */
    suspend fun createNextMonth(from: MonthKey): MonthKey {
        val key = MonthKey.from(from.toYearMonth().plusMonths(1))
        ensureMonth(key)
        return key
    }

    suspend fun updateEntry(month: MonthKey, entry: DayEntry) {
        context.dataStore.edit { prefs ->
            val map = parseEntries(prefs[monthEntriesKey(month)]).toMutableMap()
            val cleaned = entry.copy(
                site = entry.site.trim(),
                hasFlag = entry.hasFlag && entry.site.isNotBlank() && !WorkRules.isRestSite(entry.site.trim()),
            )
            if (cleaned.site.isBlank() && !cleaned.hasFlag) {
                map.remove(cleaned.day)
            } else {
                map[cleaned.day] = cleaned
            }
            prefs[monthEntriesKey(month)] = encodeEntries(map)
            val months = parseMonths(prefs[monthsKey]).toMutableSet()
            months.add(month)
            prefs[monthsKey] = encodeMonths(months.sortedDescending())
        }
    }

    suspend fun updateSettings(settings: PaySettings) {
        context.dataStore.edit { prefs ->
            val d = PaySettings()
            prefs[nameKey] = settings.employeeName.trim().ifBlank { d.employeeName }
            prefs[kanaKey] = settings.employeeNameKana.trim()
            prefs[salaryEnabledKey] = settings.salaryEnabled
            prefs[flagEnabledKey] = settings.flagEnabled
            prefs[wageKey] = settings.dailyWageYen.coerceAtLeast(0L)
            prefs[otKey] = settings.overtimeHourYen.coerceAtLeast(0L)
            prefs[flagBonusKey] = settings.flagBonusYen.coerceAtLeast(0L)
            prefs[deductionKey] = settings.monthlyDeductionYen.coerceAtLeast(0L)
            val region = SheetLabelFactory.of(settings.sheetLanguage)
            prefs[flagOnKey] = settings.flagLabelOn.trim().ifBlank { region.drivingOn }
            prefs[flagOffKey] = settings.flagLabelOff.trim().ifBlank { region.driving }
            prefs[languageKey] = settings.language.code
            prefs[sheetLanguageKey] = settings.sheetLanguage.code
            prefs[sheetTitleKey] = settings.sheetTitleCustom.trim()
        }
    }

    suspend fun completeFirstSetup(settings: PaySettings) {
        updateSettings(settings)
        context.dataStore.edit { prefs ->
            prefs[setupDoneKey] = true
        }
    }

    suspend fun addAdjacentMonth(from: MonthKey, next: Boolean): MonthKey {
        val ym = from.toYearMonth().let { if (next) it.plusMonths(1) else it.minusMonths(1) }
        val key = MonthKey.from(ym)
        ensureMonth(key)
        return key
    }

    private fun monthEntriesKey(month: MonthKey) =
        stringPreferencesKey("entries_${month.year}_${month.month}")

    private fun parseMonths(raw: String?): List<MonthKey> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(MonthKey(o.getInt("y"), o.getInt("m")))
                }
            }.sortedDescending()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeMonths(months: List<MonthKey>): String {
        val arr = JSONArray()
        months.forEach { m ->
            arr.put(JSONObject().put("y", m.year).put("m", m.month))
        }
        return arr.toString()
    }

    private fun parseEntries(raw: String?): Map<Int, DayEntry> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val day = o.getInt("d")
                    put(
                        day,
                        DayEntry(
                            day = day,
                            site = o.optString("s", ""),
                            hasFlag = o.optBoolean("drv", false) || o.optBoolean("flag", false),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun encodeEntries(map: Map<Int, DayEntry>): String {
        val arr = JSONArray()
        map.values.sortedBy { it.day }.forEach { e ->
            arr.put(
                JSONObject()
                    .put("d", e.day)
                    .put("s", e.site)
                    .put("flag", e.hasFlag)
                    .put("drv", e.hasFlag),
            )
        }
        return arr.toString()
    }
}
