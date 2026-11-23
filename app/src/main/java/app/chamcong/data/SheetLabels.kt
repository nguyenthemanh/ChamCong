package app.chamcong.data

import java.time.DayOfWeek

/** Labels for the timesheet grid — independent from app UI language. */
data class SheetLabels(
    val title: String,
    val namePrefix: String,
    val colDay: String,
    val colWeekday: String,
    val colSite: String,
    val colDriving: String,
    val sitePlaceholder: String,
    val driving: String,
    val drivingOn: String,
    val workDaysSuffix: String,
    val monthLabel: (MonthKey) -> String,
    val weekdayShort: (DayOfWeek) -> String,
    val weekdayFull: (DayOfWeek) -> String,
)

object SheetLabelFactory {
    fun of(lang: AppLanguage): SheetLabels = when (lang) {
        AppLanguage.JA -> SheetLabels(
            title = "月報報告書",
            namePrefix = "氏名: ",
            colDay = "日",
            colWeekday = "曜",
            colSite = "ゲンバ",
            colDriving = "運転",
            sitePlaceholder = "ゲンバ",
            driving = "運転",
            drivingOn = "運転あり",
            workDaysSuffix = "日",
            monthLabel = { it.labelJa() },
            weekdayShort = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "月"
                    DayOfWeek.TUESDAY -> "火"
                    DayOfWeek.WEDNESDAY -> "水"
                    DayOfWeek.THURSDAY -> "木"
                    DayOfWeek.FRIDAY -> "金"
                    DayOfWeek.SATURDAY -> "土"
                    DayOfWeek.SUNDAY -> "日"
                }
            },
            weekdayFull = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "月曜日"
                    DayOfWeek.TUESDAY -> "火曜日"
                    DayOfWeek.WEDNESDAY -> "水曜日"
                    DayOfWeek.THURSDAY -> "木曜日"
                    DayOfWeek.FRIDAY -> "金曜日"
                    DayOfWeek.SATURDAY -> "土曜日"
                    DayOfWeek.SUNDAY -> "日曜日"
                }
            },
        )
        AppLanguage.VI -> SheetLabels(
            title = "Báo cáo tháng",
            namePrefix = "Họ tên: ",
            colDay = "Ngày",
            colWeekday = "Thứ",
            colSite = "Hiện trường",
            colDriving = "Lái xe",
            sitePlaceholder = "Hiện trường",
            driving = "Lái xe",
            drivingOn = "Có lái",
            workDaysSuffix = " ngày",
            monthLabel = { "Tháng ${it.month}/${it.year}" },
            weekdayShort = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "T2"
                    DayOfWeek.TUESDAY -> "T3"
                    DayOfWeek.WEDNESDAY -> "T4"
                    DayOfWeek.THURSDAY -> "T5"
                    DayOfWeek.FRIDAY -> "T6"
                    DayOfWeek.SATURDAY -> "T7"
                    DayOfWeek.SUNDAY -> "CN"
                }
            },
            weekdayFull = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "Thứ Hai"
                    DayOfWeek.TUESDAY -> "Thứ Ba"
                    DayOfWeek.WEDNESDAY -> "Thứ Tư"
                    DayOfWeek.THURSDAY -> "Thứ Năm"
                    DayOfWeek.FRIDAY -> "Thứ Sáu"
                    DayOfWeek.SATURDAY -> "Thứ Bảy"
                    DayOfWeek.SUNDAY -> "Chủ Nhật"
                }
            },
        )
        AppLanguage.EN -> SheetLabels(
            title = "Monthly Report",
            namePrefix = "Name: ",
            colDay = "Day",
            colWeekday = "DoW",
            colSite = "Site",
            colDriving = "Drive",
            sitePlaceholder = "Job site",
            driving = "Drive",
            drivingOn = "Driving",
            workDaysSuffix = " days",
            monthLabel = { "${it.year}-${it.month.toString().padStart(2, '0')}" },
            weekdayShort = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "Mon"
                    DayOfWeek.TUESDAY -> "Tue"
                    DayOfWeek.WEDNESDAY -> "Wed"
                    DayOfWeek.THURSDAY -> "Thu"
                    DayOfWeek.FRIDAY -> "Fri"
                    DayOfWeek.SATURDAY -> "Sat"
                    DayOfWeek.SUNDAY -> "Sun"
                }
            },
            weekdayFull = { dow ->
                when (dow) {
                    DayOfWeek.MONDAY -> "Monday"
                    DayOfWeek.TUESDAY -> "Tuesday"
                    DayOfWeek.WEDNESDAY -> "Wednesday"
                    DayOfWeek.THURSDAY -> "Thursday"
                    DayOfWeek.FRIDAY -> "Friday"
                    DayOfWeek.SATURDAY -> "Saturday"
                    DayOfWeek.SUNDAY -> "Sunday"
                }
            },
        )
    }
}
