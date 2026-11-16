package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderLogicTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val day = LocalDate.of(2026, 11, 20)
    private val dueAt = TaskLogic.dueAtOfDay(day.toEpochDay())

    private fun task(
        minute: Int? = 9 * 60,
        due: Long? = dueAt,
        done: Long? = null,
        kind: NoteKind = NoteKind.TASK,
    ) = NoteEntity(
        kind = kind, dueAt = due, completedAt = done, reminderMinute = minute,
        createdAt = 0, updatedAt = 0,
    )

    private fun millisOf(hour: Int, minute: Int) =
        day.atTime(hour, minute).atZone(madrid).toInstant().toEpochMilli()

    @Test
    fun firesAtTheChosenLocalTimeOnTheDueDay() {
        assertEquals(millisOf(9, 0), ReminderLogic.triggerAt(task(), 0, madrid))
        assertEquals(millisOf(18, 30), ReminderLogic.triggerAt(task(minute = 18 * 60 + 30), 0, madrid))
    }

    @Test
    fun sameWallClockTimeInAnotherZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val expected = day.atTime(9, 0).atZone(tokyo).toInstant().toEpochMilli()
        assertEquals(expected, ReminderLogic.triggerAt(task(), 0, tokyo))
    }

    @Test
    fun nothingToScheduleWhenNotApplicable() {
        assertNull(ReminderLogic.triggerAt(task(minute = null), 0, madrid))
        assertNull(ReminderLogic.triggerAt(task(due = null), 0, madrid))
        assertNull(ReminderLogic.triggerAt(task(done = 5), 0, madrid))
        assertNull(ReminderLogic.triggerAt(task(kind = NoteKind.NOTE), 0, madrid))
        assertNull(ReminderLogic.triggerAt(task(minute = 2000), 0, madrid))
    }

    @Test
    fun pastTimesAreNotScheduled() {
        val after = millisOf(9, 0) + 1
        assertNull(ReminderLogic.triggerAt(task(), after, madrid))
        assertNull(ReminderLogic.triggerAt(task(), millisOf(9, 0), madrid)) // justo ahora ya no es futuro
    }
}
