package com.vitaai.app.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object ThemeManager {
    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    val mode = mutableStateOf(MODE_SYSTEM)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
        mode.value = prefs.getInt("theme_mode", MODE_SYSTEM)
    }

    fun setMode(context: Context, newMode: Int) {
        mode.value = newMode
        context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
            .edit().putInt("theme_mode", newMode).apply()
    }
}
