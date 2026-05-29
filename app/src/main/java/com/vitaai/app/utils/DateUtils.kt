package com.vitaai.app.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val DATE_PATTERN = "yyyy-MM-dd"

object DateUtils {
    private val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)

    fun todayKey(): String = formatter.format(Date())

    fun dateKey(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return formatter.format(cal.time)
    }

    fun daysBetween(date1: String, date2: String): Long {
        if (date1.isBlank() || date2.isBlank()) return -1L
        
        var result = -1L
        try {
            val d1 = formatter.parse(date1)
            val d2 = formatter.parse(date2)
            if (d1 != null && d2 != null) {
                val diff = d2.time - d1.time
                result = TimeUnit.MILLISECONDS.toDays(diff)
            }
        } catch (e: Exception) {
            // Error en parseo, se mantiene el -1L
        }
        return result
    }

    fun isYesterday(date: String): Boolean {
        val days = daysBetween(date, todayKey())
        return days == 1L
    }

    fun isToday(date: String): Boolean = date == todayKey()
}
