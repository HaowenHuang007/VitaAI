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

    val currentLanguage = mutableStateOf(LANG_EN)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_lang", LANG_EN) ?: LANG_EN
        currentLanguage.value = lang
        setLocale(lang)
    }

    fun setLanguage(context: Context, langCode: String) {
        currentLanguage.value = langCode
        context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_lang", langCode).apply()
        setLocale(langCode)
    }

    private fun setLocale(langCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
