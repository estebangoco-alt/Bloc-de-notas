package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLogicTest {
    private val today = 20_000L // un día cualquiera

    private fun due(offsetDays: Long) = TaskLogic.dueAtOfDay(today + offsetDays)

    private fun task(id: String, dueAt: Long?, completedAt: Long? = null, created: Long = 0) = NoteEntity(
        id = id, kind = NoteKind.TASK, dueAt = dueAt, completedAt = completedAt,
        createdAt = created, updatedAt = created,
    )

    @Test
    fun bucketsByDueDay() {
        assertEquals(TaskBucket.OVERDUE, TaskLogic.bucket(due(-1), null, today))
        assertEquals(TaskBucket.TODAY, TaskLogic.bucket(due(0), null, today))
        assertEquals(TaskBucket.UPCOMING, TaskLogic.bucket(due(1), null, today))
        assertEquals(TaskBucket.UPCOMING, TaskLogic.bucket(due(7), null, today))
        assertEquals(TaskBucket.LATER, TaskLogic.bucket(due(8), null, today))
        assertEquals(TaskBucket.NO_DATE, TaskLogic.bucket(null, null, today))
    }

    @Test
    fun completedAlwaysGoesToDone() {
        assertEquals(TaskBucket.DONE, TaskLogic.bucket(due(-5), 123, today))
        assertEquals(TaskBucket.DONE, TaskLogic.bucket(null, 123, today))
    }

    @Test
    fun normalizeKeepsTheSameUtcDay() {
        val noon = due(3) + 12 * 3_600_000L
        assertEquals(due(3), TaskLogic.normalize(noon))
        assertEquals(due(3), TaskLogic.normalize(due(3)))
        // Fechas anteriores a 1970 también se redondean hacia abajo.
        assertEquals(-TaskLogic.MILLIS_PER_DAY, TaskLogic.normalize(-1L))
    }

    @Test
    fun groupSortsAndSkipsEmptyGroups() {
        val tasks = listOf(
            task("late", due(20)),
            task("b", due(2), created = 2),
            task("a", due(2), created = 1),
            task("d1", null, completedAt = 10),
            task("d2", null, completedAt = 20),
        )
        val groups = TaskLogic.group(tasks, today)
        assertEquals(listOf(TaskBucket.UPCOMING, TaskBucket.LATER, TaskBucket.DONE), groups.keys.toList())
        assertEquals(listOf("a", "b"), groups.getValue(TaskBucket.UPCOMING).map { it.id })
        assertEquals(listOf("d2", "d1"), groups.getValue(TaskBucket.DONE).map { it.id })
        assertTrue(TaskLogic.group(emptyList(), today).isEmpty())
    }
}
