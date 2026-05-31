package com.vitaai.app.utils

import android.content.Context
import android.content.SharedPreferences

object MoodManager {
    private const val PREFS_NAME = "mood_prefs"
    private const val KEY_LAST_MOOD_DATE = "last_mood_date"
    private const val KEY_MOOD_LABEL = "mood_label"
    private const val KEY_MOOD_ID = "mood_id"
    private const val KEY_RECOMMENDATION = "mood_recommendation"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveMoodLocally(context: Context, label: String, moodId: String, recommendation: String) {
        getPrefs(context).edit()
            .putString(KEY_LAST_MOOD_DATE, DateUtils.todayKey())
            .putString(KEY_MOOD_LABEL, label)
            .putString(KEY_MOOD_ID, moodId)
            .putString(KEY_RECOMMENDATION, recommendation)
            .apply()
    }

    fun isMoodSetToday(context: Context): Boolean {
        val lastDate = getPrefs(context).getString(KEY_LAST_MOOD_DATE, "")
        return lastDate == DateUtils.todayKey()
    }

    fun getTodayMood(context: Context): MoodData? {
        if (!isMoodSetToday(context)) return null
        val prefs = getPrefs(context)
        return MoodData(
            label = prefs.getString(KEY_MOOD_LABEL, "") ?: "",
            moodId = prefs.getString(KEY_MOOD_ID, "") ?: "",
            recommendation = prefs.getString(KEY_RECOMMENDATION, "") ?: ""
        )
    }

    data class MoodData(val label: String, val moodId: String, val recommendation: String)
}
