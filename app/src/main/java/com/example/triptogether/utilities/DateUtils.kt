package com.example.triptogether.utilities

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private const val DATE_FORMAT_DISPLAY: String = "MMM dd, yyyy"

    fun formatDate(timestamp: Long, format: String = DATE_FORMAT_DISPLAY): String {
        if (timestamp == 0L) return ""
        val date = Date(timestamp)
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(date)
    }

    fun formatDateRange(startDate: Long, endDate: Long): String {
        if (startDate == 0L || endDate == 0L) return ""
        val start = formatDate(startDate, DATE_FORMAT_DISPLAY)
        val end = formatDate(endDate, DATE_FORMAT_DISPLAY)
        return "$start - $end"
    }

    fun getDaysBetween(startDate: Long, endDate: Long): Int {
        val diff = endDate - startDate
        return TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }
}
