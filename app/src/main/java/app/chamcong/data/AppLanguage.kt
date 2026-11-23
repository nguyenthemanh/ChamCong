package app.chamcong.data

enum class AppLanguage(val code: String, val label: String) {
    VI("vi", "Tiếng Việt"),
    JA("ja", "日本語"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: VI
    }
}
