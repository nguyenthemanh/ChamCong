package app.chamcong.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayEntry(
    val day: Int,
    val site: String = "",
    /** Extra flag on the day (default label 運転あり — customizable in settings). */
    val hasFlag: Boolean = false,
) {
    val isRest: Boolean get() = WorkRules.isRestSite(site)
    val isWorked: Boolean get() = site.isNotBlank() && !isRest
    val overtimeHours: Int get() = WorkRules.parseOvertimeHours(site)
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
    val overtimeHours: Int,
    val flagDays: Int,
    val grossYen: Long,
    val deductionYen: Long,
    val netYen: Long,
) {
    /** Excel D36 style: `16日` or `16日 +2h` */
    fun workDaysText(suffix: String = "日"): String {
        val base = "$workDays$suffix"
        return if (overtimeHours > 0) "$base +${overtimeHours}h" else base
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
        val overtimeHours = rows.sumOf { if (it.entry.isWorked) it.entry.overtimeHours else 0 }
        val flagDays = rows.count { it.entry.isWorked && it.entry.hasFlag }
        val gross = workDays * settings.dailyWageYen +
            overtimeHours * settings.overtimeHourYen +
            flagDays * settings.flagBonusYen
        val deduction = settings.monthlyDeductionYen
        return MonthSummary(
            workDays = workDays,
            overtimeHours = overtimeHours,
            flagDays = flagDays,
            grossYen = gross,
            deductionYen = deduction,
            netYen = gross - deduction,
        )
    }
}
