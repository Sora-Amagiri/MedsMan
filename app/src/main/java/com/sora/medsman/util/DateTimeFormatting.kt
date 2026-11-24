package com.sora.medsman.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private fun localeOf(context: Context): Locale =
    context.resources.configuration.locales[0]

/** Formats a clock time using the device's locale and 12/24-hour setting. */
fun formatClockTime(context: Context, epochMillis: Long): String =
    DateFormat.getTimeFormat(context).format(Date(epochMillis))

/** Formats a short date and time using locale-aware ordering. */
fun formatShortDateTime(context: Context, epochMillis: Long): String {
    val locale = localeOf(context)
    val skeleton = if (DateFormat.is24HourFormat(context)) "MMMdHm" else "MMMdhm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(Date(epochMillis))
}

/** Formats a full date, including the weekday, for history day headings. */
fun formatDayHeading(context: Context, date: LocalDate): String {
    val locale = localeOf(context)
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMd")
    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return SimpleDateFormat(pattern, locale).format(Date(dateMillis))
}
