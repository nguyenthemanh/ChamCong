package app.chamcong.data

enum class AppLanguage(val code: String, val label: String) {
    VI("vi", "Tiếng Việt"),
    JA("ja", "日本語 · Nhật"),
    KO("ko", "한국어 · Hàn Quốc"),
    ZH_TW("zh-TW", "繁體中文 · Đài Loan"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VI
    }
}
