package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagLogicTest {
    @Test
    fun normalizeCleansUserInput() {
        assertEquals("curso", TagLogic.normalize("  #curso  "))
        assertEquals("a b", TagLogic.normalize("a,  b"))
        assertEquals("", TagLogic.normalize(" # "))
        assertEquals(TagLogic.MAX_LENGTH, TagLogic.normalize("x".repeat(100)).length)
    }

    @Test
    fun addIgnoresDuplicatesWithoutCase() {
        var tags = ""
        tags = TagLogic.add(tags, "Casa")
        tags = TagLogic.add(tags, "#casa")
        tags = TagLogic.add(tags, "curso")
        assertEquals(listOf("Casa", "curso"), TagLogic.parse(tags))
    }

    @Test
    fun removeIsCaseInsensitive() {
        assertEquals("curso", TagLogic.remove("Casa,curso", "casa"))
        assertEquals("", TagLogic.remove("curso", "curso"))
    }

    @Test
    fun allMergesAndSorts() {
        assertEquals(listOf("casa", "Curso", "ideas"), TagLogic.all(listOf("Curso,casa", "ideas", "", "CASA")))
    }

    @Test
    fun priorityIsClamped() {
        assertEquals(Priority.NONE, Priority.clamp(-4))
        assertEquals(Priority.HIGH, Priority.clamp(99))
    }
}
