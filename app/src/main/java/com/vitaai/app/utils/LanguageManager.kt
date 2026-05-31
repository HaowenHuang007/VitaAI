package com.vitaai.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat

object LanguageManager {

    const val LANG_EN = "en"
    const val LANG_ES = "es"
    const val LANG_PT = "pt"
    const val LANG_FR = "fr"
    const val LANG_ZH = "zh"

    val currentLanguage = mutableStateOf("es")

    fun load(context: Context) {
        val applied = AppCompatDelegate.getApplicationLocales()
        if (!applied.isEmpty) {
            currentLanguage.value = applied.toLanguageTags().substringBefore('-').ifEmpty { "es" }
        } else {
            val lang = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
                .getString("language", "es") ?: "es"
            currentLanguage.value = lang
            setLocale(lang)
        }
    }

    // Older call sites delegate here.
    fun loadSavedLanguage(context: Context) = load(context)

    fun setLanguage(context: Context, langCode: String) {
        currentLanguage.value = langCode
        context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
            .edit().putString("language", langCode).apply()
        setLocale(langCode)
    }

    private fun setLocale(langCode: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
    }
}
