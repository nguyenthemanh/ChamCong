package app.chamcong.data

import java.time.DayOfWeek

/** Labels for the timesheet grid — independent from app UI language. */
data class SheetLabels(
    val title: String,
    val namePrefix: String,
    /** Example phonetic / reading for the placeholder name. */
    val nameReadingExample: String,
    val colDay: String,
    val colWeekday: String,
    val colSite: String,
    /** Overtime column header (region language). */
    val colOt: String,
    val colDriving: String,
    val sitePlaceholder: String,
    val driving: String,
    val drivingOn: String,
    val workDaysSuffix: String,
    /** Print footer label — total work days. */
    val totalWorkDays: String,
    /** Print footer label — total overtime. */
    val totalOt: String,
    /** Currency unit shown with pay amounts for this region. */
    val currencySuffix: String,
    /** Print/export: 90 → "1時間30分" / "1 giờ 30 phút" (region language). */
    val formatDuration: (minutes: Int) -> String,
    /** Header year-month line (calendar language = region). */
    val monthLabel: (MonthKey) -> String,
    /** Compact month chip on the tab bar (calendar language = region). */
    val tabLabel: (MonthKey) -> String,
    val weekdayShort: (DayOfWeek) -> String,
    val weekdayFull: (DayOfWeek) -> String,
)

object SheetLabelFactory {
    fun of(lang: AppLanguage): SheetLabels = when (lang) {
        AppLanguage.JA -> SheetLabels(
            title = "月報報告書",
            namePrefix = "氏名: ",
            nameReadingExample = "グエン・ヴァン・アー",
            colDay = "日",
            colWeekday = "曜",
            colSite = "ゲンバ",
            colOt = "残業",
            colDriving = "運転",
            sitePlaceholder = "ゲンバ",
            driving = "運転",
            drivingOn = "運転あり",
            workDaysSuffix = "日",
            totalWorkDays = "出勤日数",
            totalOt = "残業合計",
            currencySuffix = "円",
            formatDuration = ::jaDuration,
            monthLabel = { "${it.year} 年 ${it.month} 月" },
            tabLabel = { "${it.month}月_${it.year}" },
            weekdayShort = ::jaShort,
            weekdayFull = ::jaFull,
        )
        AppLanguage.KO -> SheetLabels(
            title = "월간 보고서",
            namePrefix = "성명: ",
            nameReadingExample = "응웬 반 아",
            colDay = "일",
            colWeekday = "요일",
            colSite = "현장",
            colOt = "잔업",
            colDriving = "운전",
            sitePlaceholder = "현장",
            driving = "운전",
            drivingOn = "운전 있음",
            workDaysSuffix = "일",
            totalWorkDays = "근무일수",
            totalOt = "잔업 합계",
            currencySuffix = "원",
            formatDuration = ::koDuration,
            monthLabel = { "${it.year}년 ${it.month}월" },
            tabLabel = { "${it.month}월_${it.year}" },
            weekdayShort = ::koShort,
            weekdayFull = ::koFull,
        )
        AppLanguage.ZH_TW -> SheetLabels(
            title = "月報報告書",
            namePrefix = "姓名: ",
            nameReadingExample = "阮文阿",
            colDay = "日",
            colWeekday = "曜",
            colSite = "工地",
            colOt = "加班",
            colDriving = "駕駛",
            sitePlaceholder = "工地",
            driving = "駕駛",
            drivingOn = "有駕駛",
            workDaysSuffix = "日",
            totalWorkDays = "出勤天數",
            totalOt = "加班合計",
            currencySuffix = "元",
            formatDuration = ::zhDuration,
            monthLabel = { "${it.year}年${it.month}月" },
            tabLabel = { "${it.month}月_${it.year}" },
            weekdayShort = ::zhShort,
            weekdayFull = ::zhFull,
        )
        AppLanguage.VI -> SheetLabels(
            title = "Báo cáo tháng",
            namePrefix = "Họ tên: ",
            nameReadingExample = "NGUYỄN VĂN A",
            colDay = "Ngày",
            colWeekday = "Thứ",
            colSite = "Hiện trường",
            colOt = "Tăng ca",
            colDriving = "Lái xe",
            sitePlaceholder = "Hiện trường",
            driving = "Lái xe",
            drivingOn = "Có lái",
            workDaysSuffix = " ngày",
            totalWorkDays = "Tổng ngày công",
            totalOt = "Tổng giờ tăng ca",
            currencySuffix = "円",
            formatDuration = ::viDuration,
            monthLabel = { "Tháng ${it.month}/${it.year}" },
            tabLabel = { "Tháng${it.month}_${it.year}" },
            weekdayShort = ::viShort,
            weekdayFull = ::viFull,
        )
        AppLanguage.EN -> SheetLabels(
            title = "Monthly Report",
            namePrefix = "Name: ",
            nameReadingExample = "NGUYEN VAN A",
            colDay = "Day",
            colWeekday = "DoW",
            colSite = "Site",
            colOt = "OT",
            colDriving = "Drive",
            sitePlaceholder = "Job site",
            driving = "Drive",
            drivingOn = "Driving",
            workDaysSuffix = " days",
            totalWorkDays = "Work days",
            totalOt = "Total overtime",
            currencySuffix = "¥",
            formatDuration = ::enDuration,
            monthLabel = { "${it.year}-${it.month.toString().padStart(2, '0')}" },
            tabLabel = { "${it.month}/${it.year}" },
            weekdayShort = ::enShort,
            weekdayFull = ::enFull,
        )
    }

    /** Compact HH:MM for in-app cells (not region words). */
    fun formatHhMm(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        val h = m / 60
        val min = m % 60
        return "%02d:%02d".format(h, min)
    }

    private fun jaDuration(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}時間${m}分"
            h > 0 -> "${h}時間"
            else -> "${m}分"
        }
    }

    private fun koDuration(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}시간 ${m}분"
            h > 0 -> "${h}시간"
            else -> "${m}분"
        }
    }

    private fun zhDuration(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}小時${m}分"
            h > 0 -> "${h}小時"
            else -> "${m}分"
        }
    }

    private fun viDuration(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h giờ $m phút"
            h > 0 -> "$h giờ"
            else -> "$m phút"
        }
    }

    private fun enDuration(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        val hPart = when (h) {
            0 -> ""
            1 -> "1 hour"
            else -> "$h hours"
        }
        val mPart = when (m) {
            0 -> ""
            1 -> "1 minute"
            else -> "$m minutes"
        }
        return listOf(hPart, mPart).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun jaShort(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "月"
        DayOfWeek.TUESDAY -> "火"
        DayOfWeek.WEDNESDAY -> "水"
        DayOfWeek.THURSDAY -> "木"
        DayOfWeek.FRIDAY -> "金"
        DayOfWeek.SATURDAY -> "土"
        DayOfWeek.SUNDAY -> "日"
    }

    private fun jaFull(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "月曜日"
        DayOfWeek.TUESDAY -> "火曜日"
        DayOfWeek.WEDNESDAY -> "水曜日"
        DayOfWeek.THURSDAY -> "木曜日"
        DayOfWeek.FRIDAY -> "金曜日"
        DayOfWeek.SATURDAY -> "土曜日"
        DayOfWeek.SUNDAY -> "日曜日"
    }

    private fun koShort(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

    private fun koFull(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

    private fun zhShort(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }

    private fun zhFull(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "星期一"
        DayOfWeek.TUESDAY -> "星期二"
        DayOfWeek.WEDNESDAY -> "星期三"
        DayOfWeek.THURSDAY -> "星期四"
        DayOfWeek.FRIDAY -> "星期五"
        DayOfWeek.SATURDAY -> "星期六"
        DayOfWeek.SUNDAY -> "星期日"
    }

    private fun viShort(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "T2"
        DayOfWeek.TUESDAY -> "T3"
        DayOfWeek.WEDNESDAY -> "T4"
        DayOfWeek.THURSDAY -> "T5"
        DayOfWeek.FRIDAY -> "T6"
        DayOfWeek.SATURDAY -> "T7"
        DayOfWeek.SUNDAY -> "CN"
    }

    private fun viFull(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "Thứ Hai"
        DayOfWeek.TUESDAY -> "Thứ Ba"
        DayOfWeek.WEDNESDAY -> "Thứ Tư"
        DayOfWeek.THURSDAY -> "Thứ Năm"
        DayOfWeek.FRIDAY -> "Thứ Sáu"
        DayOfWeek.SATURDAY -> "Thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ Nhật"
    }

    private fun enShort(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

    private fun enFull(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }
}
