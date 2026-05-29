package com.vitaai.app.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * Manager residual para manejar la persistencia del idioma preferido.
 * Las traducciones ahora se gestionan mediante el sistema estándar de Android (res/values/strings.xml).
 */
object LanguageManager {

    val currentLanguage = mutableStateOf("es")

    /**
     * Lista de idiomas soportados para el selector de configuración.
     * Se eliminan los emojis de banderas para cumplir con la directriz de profesionalización.
     */
    val languages = listOf(
        Triple("es", "", "Español"),
        Triple("en", "", "English"),
        Triple("zh", "", "中文"),
        Triple("fr", "", "Français"),
        Triple("pt", "", "Português")
    )

    fun setLanguage(context: Context, langCode: String) {
        currentLanguage.value = langCode
        val prefs = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language", langCode).apply()
        
        // Aplicar el cambio de Locale de forma programática si es necesario
        updateResources(context, langCode)
    }

    fun loadSavedLanguage(context: Context) {
        val prefs = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", Locale.getDefault().language) ?: "es"
        currentLanguage.value = lang
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
