package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineLogicTest {
    private fun day(n: Long) = n * TaskLogic.MILLIS_PER_DAY

    private fun task(id: String, due: Long?, created: Long = 0, deps: String = "", done: Boolean = false) = NoteEntity(
        id = id, title = id, kind = NoteKind.TASK, dueAt = due?.let(::day), dependsOn = deps,
        completedAt = if (done) 1L else null, createdAt = day(created), updatedAt = 0,
    )

    @Test fun undatedTasksAreCountedNotDrawn() {
        val t = TimelineLogic.build(listOf(task("a", null), task("b", 100)), todayEpochDay = 100)
        assertEquals(1, t.undated)
        assertEquals(listOf("b"), t.rows.map { it.id })
    }

    @Test fun startsAtCreationButAtMost14DaysBeforeTheEnd() {
        val recent = TimelineLogic.build(listOf(task("a", 110, created = 105)), 100).rows.single()
        assertEquals(105L, recent.startDay)
        val old = TimelineLogic.build(listOf(task("a", 110, created = 10)), 100).rows.single()
        assertEquals(97L, old.startDay)
        assertEquals(14, old.days)
    }

    @Test fun createdAfterDueStillShowsADayBar() {
        val row = TimelineLogic.build(listOf(task("a", 100, created = 120)), 100).rows.single()
        assertEquals(100L, row.startDay)
        assertEquals(100L, row.endDay)
    }

    @Test fun startsTheDayAfterItsLastDependencyEnds() {
        val a = task("a", 105)
        val b = task("b", 108)
        val c = task("c", 120, deps = "a,b")
        val row = TimelineLogic.build(listOf(a, b, c), 100).rows.first { it.id == "c" }
        assertEquals(109L, row.startDay)
        assertEquals(120L, row.endDay)
        assertEquals(listOf("a", "b"), row.dependsOn)
        assertTrue(row.blocked)
    }

    @Test fun dependencyEndingAfterTheTaskDoesNotInvertTheBar() {
        val a = task("a", 130)
        val c = task("c", 120, deps = "a")
        val row = TimelineLogic.build(listOf(a, c), 100).rows.first { it.id == "c" }
        assertEquals(120L, row.startDay)
    }

    @Test fun finishedDependencyDoesNotBlock() {
        val a = task("a", 105, done = true)
        val c = task("c", 120, deps = "a")
        val row = TimelineLogic.build(listOf(a, c), 100).rows.first { it.id == "c" }
        assertFalse(row.blocked)
    }

    @Test fun rowsAreSortedByStart() {
        val rows = TimelineLogic.build(listOf(task("late", 130, created = 125), task("early", 105, created = 101)), 100).rows
        assertEquals(listOf("early", "late"), rows.map { it.id })
    }

    @Test fun windowCoversTodayAndHasAMinimumLength() {
        val empty = TimelineLogic.build(emptyList(), 500)
        assertTrue(empty.firstDay <= 500 && empty.lastDay >= 500)
        assertTrue(empty.totalDays >= TimelineLogic.MIN_WINDOW_DAYS)
        val t = TimelineLogic.build(listOf(task("a", 105, created = 102)), 100)
        assertTrue(t.firstDay < 100 && t.lastDay > 105)
    }
}
