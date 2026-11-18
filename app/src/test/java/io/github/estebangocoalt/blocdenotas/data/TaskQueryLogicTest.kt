package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskQueryLogicTest {
    private fun task(
        id: String,
        title: String = id,
        priority: Int = 0,
        tags: String = "",
        dueAt: Long? = null,
        done: Boolean = false,
        created: Long = 0,
    ) = NoteEntity(
        id = id, title = title, kind = NoteKind.TASK, priority = priority, tags = tags,
        dueAt = dueAt, completedAt = if (done) 1L else null, createdAt = created, updatedAt = created,
    )

    private val all = listOf(
        task("a", "Zeta", priority = 1, tags = "casa", dueAt = 3 * TaskLogic.MILLIS_PER_DAY, created = 1),
        task("b", "alfa", priority = 3, tags = "trabajo,casa", dueAt = TaskLogic.MILLIS_PER_DAY, created = 2),
        task("c", "Beta", priority = 2, done = true, created = 3),
        task("d", "delta", created = 4),
    )

    @Test fun defaultQueryKeepsEverything() {
        assertTrue(TaskQuery().isDefault)
        assertEquals(4, TaskQueryLogic.filter(all, TaskQuery()).size)
    }

    @Test fun sortsByDateWithUndatedLast() {
        val ids = TaskQueryLogic.apply(all, TaskQuery(sort = "DATE")).map { it.id }
        assertEquals(listOf("b", "a", "c", "d"), ids)
    }

    @Test fun sortsByPriorityHighFirst() {
        val ids = TaskQueryLogic.apply(all, TaskQuery(sort = "PRIORITY")).map { it.id }
        assertEquals(listOf("b", "c", "a", "d"), ids)
    }

    @Test fun sortsByTitleIgnoringCase() {
        val ids = TaskQueryLogic.apply(all, TaskQuery(sort = "TITLE")).map { it.id }
        assertEquals(listOf("b", "c", "d", "a"), ids)
    }

    @Test fun sortsByNewestFirst() {
        val ids = TaskQueryLogic.apply(all, TaskQuery(sort = "CREATED")).map { it.id }
        assertEquals(listOf("d", "c", "b", "a"), ids)
    }

    @Test fun filtersByMinimumPriority() {
        val ids = TaskQueryLogic.filter(all, TaskQuery(minPriority = 2)).map { it.id }
        assertEquals(listOf("b", "c"), ids)
    }

    @Test fun filtersByTagIgnoringCase() {
        val ids = TaskQueryLogic.filter(all, TaskQuery(tag = "CASA")).map { it.id }
        assertEquals(listOf("a", "b"), ids)
    }

    @Test fun hidesDone() {
        val ids = TaskQueryLogic.filter(all, TaskQuery(hideDone = true)).map { it.id }
        assertEquals(listOf("a", "b", "d"), ids)
    }

    @Test fun unknownSortFallsBackToDate() {
        assertEquals(TaskSort.DATE, TaskSort.fromName("???"))
        assertEquals(TaskSort.DATE, TaskSort.fromName(null))
    }

    @Test fun groupUsesRequestedSort() {
        val groups = TaskLogic.group(all, todayEpochDay = 0, sort = TaskSort.PRIORITY)
        assertEquals(listOf("b", "a"), groups.getValue(TaskBucket.UPCOMING).map { it.id })
    }
}
