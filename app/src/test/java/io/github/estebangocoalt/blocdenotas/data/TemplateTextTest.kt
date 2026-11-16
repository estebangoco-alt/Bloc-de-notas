package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTextTest {
    @Test
    fun oneItemPerNonBlankLine() {
        assertEquals(listOf("Fruta", "Pan"), TemplateText.lines("Fruta\r\n\n  Pan  \n"))
    }

    @Test
    fun blankTextHasNoItems() {
        assertTrue(TemplateText.lines("  \n ").isEmpty())
    }
}
