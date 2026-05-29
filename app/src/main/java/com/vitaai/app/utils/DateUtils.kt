package com.vitaai.app.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = formatter.format(Date())

    fun dateKey(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return formatter.format(cal.time)
    }

    fun daysBetween(date1: String, date2: String): Long {
        if (date1.isBlank() || date2.isBlank()) return -1L
        return try {
            val d1 = formatter.parse(date1)
            val d2 = formatter.parse(date2)
            if (d1 != null && d2 != null) {
                val diff = d2.time - d1.time
                TimeUnit.MILLISECONDS.toDays(diff)
            } else -1L
        } catch (e: Exception) {
            -1L
        }
    }

    fun isYesterday(date: String): Boolean = daysBetween(date, todayKey()) == 1L

    fun isToday(date: String): Boolean = date == todayKey()
}
