package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureParserTest {

    @Test
    fun emptyTextGivesEmptyResult() {
        val parsed = CaptureParser.parse("  \n \n", NoteKind.NOTE)
        assertEquals("", parsed.title)
        assertEquals("", parsed.body)
        assertTrue(parsed.items.isEmpty())
    }

    @Test
    fun firstLineBecomesTitleAndRestBecomesBody() {
        val parsed = CaptureParser.parse("Comprar pilas\nAAA\nbombillas", NoteKind.NOTE)
        assertEquals("Comprar pilas", parsed.title)
        assertEquals("AAA\nbombillas", parsed.body)
        assertTrue(parsed.items.isEmpty())
    }

    @Test
    fun shoppingTurnsEachLineIntoAnItemAndStripsBullets() {
        val parsed = CaptureParser.parse("- leche\n* pan\n\n• pasta\r\nhuevos", NoteKind.SHOPPING)
        assertEquals("", parsed.title)
        assertEquals("", parsed.body)
        assertEquals(listOf("leche", "pan", "pasta", "huevos"), parsed.items)
    }

    @Test
    fun veryLongFirstLineGoesToBodyWithoutLosingText() {
        val long = "a".repeat(CaptureParser.MAX_TITLE + 1)
        val parsed = CaptureParser.parse("$long\nsegunda", NoteKind.IDEA)
        assertEquals("", parsed.title)
        assertEquals("$long\nsegunda", parsed.body)
    }

    @Test
    fun titleAtTheLimitIsKept() {
        val exact = "b".repeat(CaptureParser.MAX_TITLE)
        val parsed = CaptureParser.parse(exact, NoteKind.TASK)
        assertEquals(exact, parsed.title)
        assertEquals("", parsed.body)
    }
}
