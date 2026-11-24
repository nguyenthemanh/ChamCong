package app.chamcong.data

enum class AppLanguage(val code: String, val label: String) {
    VI("vi", "Tiếng Việt"),
    JA("ja", "日本語"),
    KO("ko", "한국어"),
    ZH_TW("zh-TW", "繁體中文"),
    EN("en", "English");

    /**
     * Country/region name shown in the sheet-region picker,
     * always in [uiLanguage] (the app UI language).
     */
    fun countryLabel(uiLanguage: AppLanguage): String = when (uiLanguage) {
        VI -> when (this) {
            VI -> "Việt Nam"
            JA -> "Nhật Bản"
            KO -> "Hàn Quốc"
            ZH_TW -> "Đài Loan"
            EN -> "Hoa Kỳ"
        }
        JA -> when (this) {
            VI -> "ベトナム"
            JA -> "日本"
            KO -> "韓国"
            ZH_TW -> "台湾"
            EN -> "アメリカ"
        }
        KO -> when (this) {
            VI -> "베트남"
            JA -> "일본"
            KO -> "한국"
            ZH_TW -> "대만"
            EN -> "미국"
        }
        ZH_TW -> when (this) {
            VI -> "越南"
            JA -> "日本"
            KO -> "韓國"
            ZH_TW -> "台灣"
            EN -> "美國"
        }
        EN -> when (this) {
            VI -> "Vietnam"
            JA -> "Japan"
            KO -> "Korea"
            ZH_TW -> "Taiwan"
            EN -> "United States"
        }
    }

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VI
    }
}
