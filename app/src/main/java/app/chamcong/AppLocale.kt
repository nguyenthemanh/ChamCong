package app.chamcong

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.chamcong.data.AppLanguage

/** Keeps Android resources / launcher label in sync with in-app UI language. */
object AppLocale {
    fun tagOf(language: AppLanguage): String = when (language) {
        AppLanguage.VI -> "vi"
        AppLanguage.JA -> "ja"
        AppLanguage.KO -> "ko"
        AppLanguage.ZH_TW -> "zh-TW"
        AppLanguage.EN -> "en"
    }

    fun apply(language: AppLanguage) {
        val desired = LocaleListCompat.forLanguageTags(tagOf(language))
        if (AppCompatDelegate.getApplicationLocales() != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }
}
