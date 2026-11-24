package app.chamcong.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayEntry(
    val day: Int,
    val site: String = "",
    /** Extra flag on the day (default label 運転あり — customizable in settings). */
    val hasFlag: Boolean = false,
    /** Overtime for this day in minutes (UI shows HH:MM). */
    val overtimeMinutes: Int = 0,
) {
    val isRest: Boolean get() = WorkRules.isRestSite(site)
    val isWorked: Boolean get() = site.isNotBlank() && !isRest
    /** OT minutes for UI / pay / print (legacy `+Nh` in site is migrated on load). */
    val effectiveOvertimeMinutes: Int get() = overtimeMinutes.coerceAtLeast(0)
}

data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    fun toYearMonth(): YearMonth = YearMonth.of(year, month)

    /** Compact tab — prefer [SheetLabels.monthLabel] / region tab via factory. */
    fun labelTab(): String = "${month}/$year"

    fun labelJa(): String = "${year} 年 ${month} 月"

    override fun compareTo(other: MonthKey): Int =
        compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        fun current(): MonthKey {
            val now = YearMonth.now()
            return MonthKey(now.year, now.monthValue)
        }

        fun from(ym: YearMonth) = MonthKey(ym.year, ym.monthValue)
    }
}

data class DayRow(
    val day: Int,
    val weekdayLabel: String,
    val dayOfWeek: DayOfWeek,
    val entry: DayEntry,
) {
    val isSunday: Boolean get() = dayOfWeek == DayOfWeek.SUNDAY
    val isSaturday: Boolean get() = dayOfWeek == DayOfWeek.SATURDAY
}

/**
 * Rates mirror Excel 月報報告書:
 * gross = workDays*dailyWage + otHours*overtimeHourYen + flagDays*flagBonusYen
 * net   = gross - monthlyDeductionYen
 * (Nov 2026 sample: 16*16000 + 10*2000 - 69000 = 207000)
 */
data class PaySettings(
    /** Example placeholder — user replaces with their name. */
    val employeeName: String = "NGUYEN VAN A",
    val employeeNameKana: String = "グエン・ヴァン・アー",
    /** When false, app only counts work days — no pay estimate UI. */
    val salaryEnabled: Boolean = true,
    /** When false, hide overtime column / picker / OT pay. */
    val overtimeEnabled: Boolean = true,
    /** When false, hide day-flag column, chips, and related settings. */
    val flagEnabled: Boolean = true,
    val dailyWageYen: Long = 0L,
    val overtimeHourYen: Long = 0L,
    val flagBonusYen: Long = 0L,
    val monthlyDeductionYen: Long = 0L,
    /** Shown when flag is on — default follows Japan region sample. */
    val flagLabelOn: String = "運転あり",
    /** Chip idle / column short label — default follows Japan region sample. */
    val flagLabelOff: String = "運転",
    /** Menus only — does not change weekday / report title. */
    val language: AppLanguage = AppLanguage.VI,
    /** Region for title, year-month, day, weekday, columns, PDF/JPG. */
    val sheetLanguage: AppLanguage = AppLanguage.JA,
    /** Blank = default title for [sheetLanguage] (e.g. 月報報告書). */
    val sheetTitleCustom: String = "",
) {
    fun resolvedSheetTitle(): String =
        sheetTitleCustom.trim().ifBlank {
            SheetLabelFactory.of(sheetLanguage).title
        }
}

data class MonthSummary(
    val workDays: Int,
    val overtimeMinutes: Int,
    val flagDays: Int,
    val grossYen: Long,
    val deductionYen: Long,
    val netYen: Long,
) {
    /** Excel D36 style: `16日` or `16日 +1時間30分` */
    fun workDaysText(suffix: String = "日", otLabel: String = ""): String {
        val base = "$workDays$suffix"
        return if (otLabel.isNotBlank()) "$base +$otLabel" else base
    }
}

/** Excel COUNTIFS / +Nh parsing rules. */
object WorkRules {
    private val restPrefixes = listOf("やすみ", "ヤスミ", "休み")
    private val otRegex = Regex("""\+(\d+)\s*h""", RegexOption.IGNORE_CASE)

    fun isRestSite(site: String): Boolean {
        val t = site.trim()
        if (t.isEmpty()) return false
        return restPrefixes.any { t.startsWith(it) }
    }

    fun parseOvertimeHours(site: String): Int =
        otRegex.findAll(site).sumOf { it.groupValues[1].toIntOrNull() ?: 0 }
}

object CalendarFactory {
    fun buildRows(
        key: MonthKey,
        entries: Map<Int, DayEntry>,
        sheetLanguage: AppLanguage = AppLanguage.JA,
    ): List<DayRow> {
        val labels = SheetLabelFactory.of(sheetLanguage)
        // Always real calendar length for that year-month (28/29/30/31).
        val ym = key.toYearMonth()
        return (1..ym.lengthOfMonth()).map { day ->
            val date = LocalDate.of(key.year, key.month, day)
            val dow = date.dayOfWeek
            DayRow(
                day = day,
                weekdayLabel = labels.weekdayShort(dow),
                dayOfWeek = dow,
                entry = entries[day] ?: DayEntry(day),
            )
        }
    }

    fun summarize(rows: List<DayRow>, settings: PaySettings): MonthSummary {
        val workDays = rows.count { it.entry.isWorked }
        val overtimeMinutes = if (settings.overtimeEnabled) {
            rows.sumOf { if (it.entry.isWorked) it.entry.effectiveOvertimeMinutes else 0 }
        } else {
            0
        }
        val flagDays = if (settings.flagEnabled) {
            rows.count { it.entry.isWorked && it.entry.hasFlag }
        } else {
            0
        }
        if (!settings.salaryEnabled) {
            return MonthSummary(
                workDays = workDays,
                overtimeMinutes = overtimeMinutes,
                flagDays = flagDays,
                grossYen = 0L,
                deductionYen = 0L,
                netYen = 0L,
            )
        }
        val flagPay = if (settings.flagEnabled) flagDays * settings.flagBonusYen else 0L
        val otPay = if (settings.overtimeEnabled) {
            (overtimeMinutes.toLong() * settings.overtimeHourYen) / 60L
        } else {
            0L
        }
        val gross = workDays * settings.dailyWageYen + otPay + flagPay
        val deduction = settings.monthlyDeductionYen
        return MonthSummary(
            workDays = workDays,
            overtimeMinutes = overtimeMinutes,
            flagDays = flagDays,
            grossYen = gross,
            deductionYen = deduction,
            netYen = gross - deduction,
        )
    }
}
