package uz.sentinel.core.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    val supportedLanguages = listOf(
        "uz" to "O'zbekcha",
        "ru" to "Русский",
        "en" to "English",
        "tr" to "Türkçe"
    )

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences("sentinel_locale_sync", Context.MODE_PRIVATE)
            .getString("lang", "uz") ?: "uz"
    }

    fun applyLocale(context: Context, languageCode: String = getSavedLanguage(context)): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
