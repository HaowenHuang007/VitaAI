package com.vitaai.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun dateKey(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun daysBetween(date1: String, date2: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d1 = fmt.parse(date1) ?: return 0
        val d2 = fmt.parse(date2) ?: return 0
        return TimeUnit.MILLISECONDS.toDays(d2.time - d1.time)
    }

    fun isYesterday(date: String): Boolean = daysBetween(date, todayKey()) == 1L

    fun isToday(date: String): Boolean = date == todayKey()
}
