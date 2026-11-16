package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RepeatLogicTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun noRepeatHasNoNextDay() {
        assertNull(RepeatLogic.nextDay(day(2026, 11, 16), RepeatRule.NONE, day(2026, 11, 16)))
    }

    @Test
    fun simpleSteps() {
        val due = day(2026, 11, 16)
        assertEquals(day(2026, 11, 17), RepeatLogic.nextDay(due, RepeatRule.DAILY, due))
        assertEquals(day(2026, 11, 23), RepeatLogic.nextDay(due, RepeatRule.WEEKLY, due))
        assertEquals(day(2026, 12, 16), RepeatLogic.nextDay(due, RepeatRule.MONTHLY, due))
        assertEquals(day(2027, 11, 16), RepeatLogic.nextDay(due, RepeatRule.YEARLY, due))
    }

    @Test
    fun overdueTasksJumpToTheFirstOccurrenceFromToday() {
        val due = day(2026, 11, 1)
        val today = day(2026, 11, 16)
        assertEquals(today, RepeatLogic.nextDay(due, RepeatRule.DAILY, today))
        assertEquals(day(2026, 11, 22), RepeatLogic.nextDay(due, RepeatRule.WEEKLY, today))
        assertEquals(day(2026, 12, 1), RepeatLogic.nextDay(due, RepeatRule.MONTHLY, today))
    }

    @Test
    fun monthlyKeepsTheOriginalDayAfterShortMonths() {
        val due = day(2026, 1, 31)
        assertEquals(day(2026, 2, 28), RepeatLogic.nextDay(due, RepeatRule.MONTHLY, due))
        assertEquals(day(2026, 3, 31), RepeatLogic.nextDay(due, RepeatRule.MONTHLY, day(2026, 3, 1)))
    }

    @Test
    fun yearlyHandlesLeapDay() {
        val due = day(2028, 2, 29)
        assertEquals(day(2029, 2, 28), RepeatLogic.nextDay(due, RepeatRule.YEARLY, due))
        assertEquals(day(2032, 2, 29), RepeatLogic.nextDay(due, RepeatRule.YEARLY, day(2032, 1, 1)))
    }

    @Test
    fun completingARepeatingTaskReschedulesIt() {
        val due = TaskLogic.dueAtOfDay(day(2026, 11, 16))
        val note = NoteEntity(
            kind = NoteKind.TASK, dueAt = due, completedAt = 5L, repeatRule = "WEEKLY",
            createdAt = 0, updatedAt = 0,
        )
        val next = RepeatLogic.advance(note, day(2026, 11, 16), now = 99L)!!
        assertEquals(TaskLogic.dueAtOfDay(day(2026, 11, 23)), next.dueAt)
        assertNull(next.completedAt)
        assertEquals(99L, next.updatedAt)
    }

    @Test
    fun nonRepeatingOrUndatedTasksAreNotRescheduled() {
        val plain = NoteEntity(kind = NoteKind.TASK, dueAt = 0L, createdAt = 0, updatedAt = 0)
        val undated = plain.copy(dueAt = null, repeatRule = "DAILY")
        val note = plain.copy(kind = NoteKind.NOTE, repeatRule = "DAILY")
        assertNull(RepeatLogic.advance(plain, 0, 0))
        assertNull(RepeatLogic.advance(undated, 0, 0))
        assertNull(RepeatLogic.advance(note, 0, 0))
    }
}
