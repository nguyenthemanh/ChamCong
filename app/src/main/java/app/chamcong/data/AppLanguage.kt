package app.chamcong.data

enum class AppLanguage(val code: String, val label: String, val countryLabel: String) {
    VI("vi", "Tiếng Việt", "Việt Nam"),
    JA("ja", "日本語", "日本"),
    KO("ko", "한국어", "한국"),
    ZH_TW("zh-TW", "繁體中文", "台灣"),
    EN("en", "English", "United States");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VI
    }
}
