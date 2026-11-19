package io.github.estebangocoalt.blocdenotas.importer

import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ImportTest {
    private val today = LocalDate.of(2026, 11, 19)

    @Test fun keepNoteWithListAndLabels() {
        val json = """{"title":"Compra","color":"GREEN","isPinned":true,"isArchived":true,"isTrashed":false,
          "listContent":[{"text":"Leche","isChecked":true},{"text":"Pan","isChecked":false}],
          "labels":[{"name":"casa"}],"createdTimestampUsec":1700000000000000,"userEditedTimestampUsec":1700000100000000,
          "attachments":[{"filePath":"a.jpg","mimetype":"image/jpeg"},{"filePath":"b.pdf","mimetype":"application/pdf"}]}"""
        val k = KeepImport.parse(json, 5L)!!
        assertEquals(1, k.note.colorIndex)
        assertTrue(k.note.pinned)
        assertEquals(2, k.items.size)
        assertTrue(k.items[0].checked)
        assertEquals(1700000100000L, k.note.updatedAt)
        assertEquals(1, k.attachments.size)
        assertTrue(k.note.tags.contains("casa") && k.note.tags.contains("archivada"))
        assertEquals(k.note.id, KeepImport.parse(json, 99L)!!.note.id)
    }

    @Test fun keepSkipsTrashedAndForeignJson() {
        assertNull(KeepImport.parse("""{"title":"x","textContent":"y","isTrashed":true}""", 1L))
        assertNull(KeepImport.parse("""{"foo":1}""", 1L))
        assertNull(KeepImport.parse("no json", 1L))
    }

    @Test fun csvHandlesQuotesAndNewlines() {
        val rows = TodoistImport.parseCsv("﻿A,B\r\n\"x, \"\"y\"\"\",\"l1\nl2\"\r\n")
        assertEquals(listOf("A", "B"), rows[0])
        assertEquals(listOf("x, \"y\"", "l1\nl2"), rows[1])
    }

    @Test fun datesAndRepeat() {
        assertEquals(LocalDate.of(2026, 12, 1).toEpochDay(), TodoistImport.parseDate("2026-12-01", today).dueDay)
        assertEquals(LocalDate.of(2026, 3, 5).toEpochDay(), TodoistImport.parseDate("5/3/2026", today).dueDay)
        assertEquals(RepeatRule.WEEKLY, TodoistImport.parseDate("every week", today).repeat)
        assertEquals(RepeatRule.DAILY, TodoistImport.parseDate("every day", today).repeat)
        assertEquals(RepeatRule.MONTHLY, TodoistImport.parseDate("cada mes", today).repeat)
        assertNull(TodoistImport.parseDate("???", today).dueDay)
        assertNull(TodoistImport.parseDate("", today).dueDay)
    }

    @Test fun todoistBuildsBoard() {
        val csv = "TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,DATE\n" +
            "task,Sin sección,,4,1,\n" +
            "section,Hoy,,,,\n" +
            "task,Llamar,Detalle,1,1,2026-11-20\n" +
            "task,Subtarea,,4,2,\n" +
            "note,Un comentario,,,,\n"
        val r = TodoistImport.parse(csv, "Casa", "Tareas", 2, 1L, today)!!
        assertEquals(2, r.columns.size)
        assertEquals(2, r.notes.size)
        val call = r.notes[1]
        assertEquals(Priority.HIGH, call.priority)
        assertTrue(call.body.contains("Un comentario") && call.body.startsWith("Detalle"))
        assertEquals(1, r.items.size)
        assertEquals(call.id, r.items[0].noteId)
        assertEquals(r.columns[1].id, call.columnId)
        assertEquals(2, r.board.position)
        assertNull(TodoistImport.parse("A,B\n1,2", "x", "T", 0, 1L, today))
        assertNotNull(r.board)
    }
}
