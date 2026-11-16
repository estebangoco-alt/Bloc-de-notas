package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLogicTest {
    private val today = 20_000L

    private fun note(
        id: String,
        kind: NoteKind = NoteKind.NOTE,
        dueDay: Long? = null,
        done: Boolean = false,
        column: String? = null,
        pinned: Boolean = false,
        inbox: Boolean = false,
        parent: String? = null,
        trashed: Boolean = false,
        updated: Long = 0,
    ) = NoteEntity(
        id = id, title = id, kind = kind, pinned = pinned, inbox = inbox, parentId = parent,
        dueAt = dueDay?.let { TaskLogic.dueAtOfDay(it) },
        completedAt = if (done) 1L else null,
        columnId = column,
        deletedAt = if (trashed) 1L else null,
        createdAt = 0, updatedAt = updated,
    )

    private val board = BoardEntity("b1", "Casa", 0)
    private val cols = listOf(
        BoardColumnEntity("todo", "Por hacer", 0, "b1"),
        BoardColumnEntity("done", "Hecho", 1, "b1"),
    )

    @Test
    fun countsNotesAndIgnoresInboxSubpagesAndTrash() {
        val data = DashboardLogic.compute(
            listOf(
                note("a", pinned = true), note("b", NoteKind.IDEA), note("c", inbox = true),
                note("d", parent = "a"), note("e", trashed = true),
            ),
            listOf(board), cols, today,
        )
        assertEquals(2, data.notes)
        assertEquals(1, data.pinned)
        assertEquals(1, data.inbox)
    }

    @Test
    fun tasksOverdueTodayAndWeek() {
        val tasks = listOf(
            note("late", NoteKind.TASK, dueDay = today - 2),
            note("now", NoteKind.TASK, dueDay = today),
            note("soon", NoteKind.TASK, dueDay = today + 3),
            note("far", NoteKind.TASK, dueDay = today + 30),
            note("finished", NoteKind.TASK, dueDay = today, done = true),
            note("nodate", NoteKind.TASK),
        )
        val data = DashboardLogic.compute(tasks, listOf(board), cols, today)
        assertEquals(5, data.pendingTasks)
        assertEquals(1, data.doneTasks)
        assertEquals(1, data.overdue)
        assertEquals(1, data.dueToday)
        assertEquals(listOf(1, 0, 0, 1, 0, 0, 0), data.week)
        assertEquals(listOf("late", "now"), data.todayList.map { it.id })
    }

    @Test
    fun boardProgressCountsLastColumnAndCompletedCards() {
        val cards = listOf(
            note("a", NoteKind.TASK, column = "todo"),
            note("b", NoteKind.TASK, column = "done"),
            note("c", NoteKind.TASK, column = "todo", done = true),
            note("d", NoteKind.TASK), // sin columna: primer tablero, primera columna
        )
        val data = DashboardLogic.compute(cards, listOf(board), cols, today)
        val progress = data.boards.single()
        assertEquals(4, progress.total)
        assertEquals(2, progress.done)
        assertEquals(50, data.boardPercent)
    }

    @Test
    fun emptyDataIsAllZeros() {
        val data = DashboardLogic.compute(emptyList(), emptyList(), emptyList(), today)
        assertEquals(0, data.notes + data.pendingTasks + data.boardCards + data.weekTotal)
        assertEquals(0, data.boardPercent)
    }
}
