package com.sora.medsman

import com.sora.medsman.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NotificationSchedulingTest {

    private fun date(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }

    private fun assertDate(expected: Calendar, actualMillis: Long) {
        val actual = Calendar.getInstance().apply { timeInMillis = actualMillis }
        assertEquals(expected.get(Calendar.YEAR), actual.get(Calendar.YEAR))
        assertEquals(expected.get(Calendar.MONTH), actual.get(Calendar.MONTH))
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), actual.get(Calendar.DAY_OF_MONTH))
        assertEquals(expected.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.HOUR_OF_DAY))
        assertEquals(expected.get(Calendar.MINUTE), actual.get(Calendar.MINUTE))
    }

    @Test
    fun laterTodayIsScheduledWhenDoseIsNotHandled() {
        val now = date(2026, Calendar.NOVEMBER, 23, 7, 0) // Monday
        val actual = NotificationHelper.calculateNextTriggerTime(
            timeOfDay = "08:00",
            daysOfWeek = emptyList(),
            skipToday = false,
            now = now
        )
        assertDate(date(2026, Calendar.NOVEMBER, 23, 8, 0), actual)
    }

    @Test
    fun nextOccurrenceSkipsTodayWhenDoseIsHandled() {
        val now = date(2026, Calendar.NOVEMBER, 23, 7, 0) // Monday
        val actual = NotificationHelper.calculateNextTriggerTime(
            timeOfDay = "08:00",
            daysOfWeek = emptyList(),
            skipToday = true,
            now = now
        )
        assertDate(date(2026, Calendar.NOVEMBER, 24, 8, 0), actual)
    }

    @Test
    fun skipTodayRespectsSelectedWeekdays() {
        val now = date(2026, Calendar.NOVEMBER, 23, 7, 0) // Monday
        val actual = NotificationHelper.calculateNextTriggerTime(
            timeOfDay = "08:00",
            daysOfWeek = listOf(1), // Monday only
            skipToday = true,
            now = now
        )
        assertDate(date(2026, Calendar.NOVEMBER, 30, 8, 0), actual)
    }

    @Test
    fun passedTimeRollsToNextSelectedWeekday() {
        val now = date(2026, Calendar.NOVEMBER, 23, 9, 0) // Monday, after 08:00
        val actual = NotificationHelper.calculateNextTriggerTime(
            timeOfDay = "08:00",
            daysOfWeek = listOf(1), // Monday only
            skipToday = false,
            now = now
        )
        assertDate(date(2026, Calendar.NOVEMBER, 30, 8, 0), actual)
    }
}
