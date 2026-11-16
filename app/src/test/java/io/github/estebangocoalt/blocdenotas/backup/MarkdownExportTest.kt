package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipInputStream

class MarkdownExportTest {
    private fun note(id: String, title: String, kind: NoteKind = NoteKind.NOTE, body: String = "", parent: String? = null) =
        NoteEntity(id = id, title = title, body = body, kind = kind, parentId = parent, createdAt = 0, updatedAt = 0)

    @Test
    fun fileNamesAreSafe() {
        assertEquals("a b", MarkdownExport.fileName("a/b", "x"))
        assertEquals("x", MarkdownExport.fileName(" ../ ", "x"))
        assertEquals(80, MarkdownExport.fileName("y".repeat(200), "x").length)
    }

    @Test
    fun rendersFrontMatterBodyAndChecklist() {
        val n = note("1", "Compra \"semanal\"", NoteKind.SHOPPING, body = "Para el finde").copy(tags = "casa,curso", priority = 3)
        val items = listOf(
            ChecklistItemEntity(id = "a", noteId = "1", text = "Leche", checked = true, position = 0),
            ChecklistItemEntity(id = "b", noteId = "1", text = "Pan", checked = false, position = 1),
        )
        val md = MarkdownExport.render(n, items, parentTitle = "Casa", untitled = "Sin título")
        assertTrue(md.startsWith("---\n"))
        assertTrue(md.contains("title: \"Compra \\\"semanal\\\"\""))
        assertTrue(md.contains("kind: shopping"))
        assertTrue(md.contains("tags: [\"casa\", \"curso\"]"))
        assertTrue(md.contains("priority: high"))
        assertTrue(md.contains("parent: \"[[Casa]]\""))
        assertTrue(md.contains("# Compra \"semanal\""))
        assertTrue(md.contains("Para el finde"))
        assertTrue(md.contains("- [x] Leche\n- [ ] Pan"))
        assertTrue(md.endsWith("\n"))
    }

    @Test
    fun filesGoToFoldersWithUniqueNamesAndSkipTrash() {
        val notes = listOf(
            note("1", "Receta"),
            note("2", "Receta"),
            note("3", "Idea", NoteKind.IDEA),
            note("4", "Borrada").copy(deletedAt = 5L),
            note("5", "", body = "primera línea\nresto"),
            note("6", "Hija", parent = "1"),
        )
        val files = MarkdownExport.files(notes, emptyList(), untitled = "Sin título")
        assertEquals(
            listOf("notes/Receta.md", "notes/Receta (2).md", "ideas/Idea.md", "notes/primera línea.md", "notes/Hija.md"),
            files.map { it.path },
        )
        assertTrue(files.last().text.contains("parent: \"[[Receta]]\""))
        assertFalse(files.any { it.path.contains("Borrada") })
    }

    @Test
    fun packsEveryFileIntoTheZip() {
        val bytes = MarkdownExport.pack(listOf(MarkdownExport.MdFile("notes/a.md", "hola"), MarkdownExport.MdFile("tasks/b.md", "adiós")))
        val names = mutableListOf<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) names += (zip.nextEntry ?: break).name
        }
        assertEquals(listOf("notes/a.md", "tasks/b.md"), names)
    }
}
