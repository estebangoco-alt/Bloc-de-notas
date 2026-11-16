package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLogicTest {

    private val todo = BoardColumnEntity(id = "todo", title = "Por hacer", position = 0)
    private val done = BoardColumnEntity(id = "done", title = "Hecho", position = 1)
    private val columns = listOf(todo, done)

    private fun card(id: String, column: String?, position: Int, created: Long = 0) = NoteEntity(
        id = id, title = id, kind = NoteKind.TASK, columnId = column, columnPosition = position,
        createdAt = created, updatedAt = created,
    )

    @Test
    fun cardsWithoutOrWithUnknownColumnGoToTheFirstColumn() {
        val grouped = BoardLogic.group(columns, listOf(card("a", null, 0), card("b", "borrada", 1), card("c", "done", 0)))
        assertEquals(listOf("a", "b"), grouped.getValue("todo").map { it.id })
        assertEquals(listOf("c"), grouped.getValue("done").map { it.id })
    }

    @Test
    fun noColumnsMeansNoGroups() {
        assertTrue(BoardLogic.group(emptyList(), listOf(card("a", null, 0))).isEmpty())
    }

    @Test
    fun movingToAnotherColumnRenumbersBothColumns() {
        val cards = listOf(card("a", "todo", 0), card("b", "todo", 1), card("c", "todo", 2), card("x", "done", 0))
        val changed = BoardLogic.move(columns, cards, "b", "done", 0).associateBy { it.id }
        assertEquals("done", changed.getValue("b").columnId)
        assertEquals(0, changed.getValue("b").columnPosition)
        assertEquals(1, changed.getValue("x").columnPosition) // baja un puesto
        assertEquals(1, changed.getValue("c").columnPosition) // la columna de origen se cierra el hueco
        assertTrue("a" !in changed) // no cambia
    }

    @Test
    fun moveToUnknownColumnOrCardDoesNothing() {
        val cards = listOf(card("a", "todo", 0))
        assertTrue(BoardLogic.move(columns, cards, "a", "nada", 0).isEmpty())
        assertTrue(BoardLogic.move(columns, cards, "zzz", "done", 0).isEmpty())
    }

    @Test
    fun shiftSwapsNeighboursAndStopsAtTheEdges() {
        val cards = listOf(card("a", "todo", 0), card("b", "todo", 1))
        val down = BoardLogic.shift(columns, cards, "a", 1).associateBy { it.id }
        assertEquals(1, down.getValue("a").columnPosition)
        assertEquals(0, down.getValue("b").columnPosition)
        assertTrue(BoardLogic.shift(columns, cards, "a", -1).isEmpty())
        assertTrue(BoardLogic.shift(columns, cards, "b", 1).isEmpty())
    }

    @Test
    fun shiftColumnReordersAndRenumbers() {
        val changed = BoardLogic.shiftColumn(columns, "todo", 1).associateBy { it.id }
        assertEquals(1, changed.getValue("todo").position)
        assertEquals(0, changed.getValue("done").position)
        assertTrue(BoardLogic.shiftColumn(columns, "todo", -1).isEmpty())
        assertTrue(BoardLogic.shiftColumn(columns, "nada", 1).isEmpty())
    }
}
