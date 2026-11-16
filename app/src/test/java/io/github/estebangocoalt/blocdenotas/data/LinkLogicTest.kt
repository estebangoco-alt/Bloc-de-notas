package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkLogicTest {
    private fun note(id: String, title: String, body: String = "") =
        NoteEntity(id = id, title = title, body = body, createdAt = 0, updatedAt = 0)

    @Test
    fun findsLinkedTitlesOnce() {
        val text = "Ver [[Receta]] y [[receta]], también [[ Compra semanal ]] y [[]] y [[a\nb]]"
        assertEquals(listOf("Receta", "Compra semanal"), LinkLogic.titles(text))
    }

    @Test
    fun resolvesTitlesWithoutCase() {
        val notes = listOf(note("1", "Receta"), note("2", "Compra"))
        assertEquals("1", LinkLogic.find("receta", notes)?.id)
        assertNull(LinkLogic.find("nada", notes))
        assertNull(LinkLogic.find("receta", notes, except = "1"))
    }

    @Test
    fun backlinksListNotesThatMentionThisOne() {
        val target = note("1", "Receta")
        val all = listOf(
            target,
            note("2", "Menú", "Hoy: [[receta]]"),
            note("3", "Otra", "sin enlaces"),
            note("4", "Yo mismo", "[[Receta]]"),
        )
        assertEquals(listOf("2", "4"), LinkLogic.backlinks(target, all).map { it.id })
        assertEquals(emptyList<NoteEntity>(), LinkLogic.backlinks(note("9", ""), all))
    }

    @Test
    fun slashQueryOnlyAtTheEndAfterSpaceOrStart() {
        assertEquals("", LinkLogic.slashQuery("/"))
        assertEquals("fec", LinkLogic.slashQuery("hola /fec"))
        assertEquals("tit", LinkLogic.slashQuery("línea\n/tit"))
        assertNull(LinkLogic.slashQuery("http://sitio"))
        assertNull(LinkLogic.slashQuery("1/2"))
        assertNull(LinkLogic.slashQuery("/cmd seguido"))
    }

    @Test
    fun removeSlashKeepsTheRestOfTheText() {
        assertEquals("hola ", LinkLogic.removeSlash("hola /fec"))
        assertEquals("", LinkLogic.removeSlash("/"))
        assertEquals("a\n", LinkLogic.removeSlash("a\n/x"))
        assertEquals("sin cambios", LinkLogic.removeSlash("sin cambios"))
    }
}
