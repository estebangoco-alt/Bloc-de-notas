package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val note = NoteEntity(
        id = "n1",
        title = "Compra",
        body = "",
        kind = NoteKind.SHOPPING,
        colorIndex = 1,
        pinned = true,
        createdAt = 100,
        updatedAt = 200,
        dueAt = 300,
        completedAt = 400,
        reminderMinute = 540,
        columnId = "c1",
        columnPosition = 2,
    )
    private val items = listOf(
        ChecklistItemEntity(id = "i2", noteId = "n1", text = "pan", checked = true, position = 1),
        ChecklistItemEntity(id = "i1", noteId = "n1", text = "leche", position = 0),
    )

    @Test
    fun exportThenImportKeepsEverything() {
        val backup = BackupMapper.toBackup(listOf(note), mapOf("n1" to items), "0.1.0", 999)
        val decoded = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup, decoded)

        val (notes, restoredItems) = BackupMapper.toEntities(decoded)
        assertEquals(listOf(note), notes)
        // Los elementos se exportan ordenados por posición.
        assertEquals(listOf("leche", "pan"), restoredItems.map { it.text })
        assertTrue(restoredItems.first { it.text == "pan" }.checked)
    }

    @Test
    fun plainJsonIsAccepted() {
        val json = """{"format":"bloc-de-notas-backup","version":1,"notes":[{"id":"a","title":"Hola"}]}"""
        val decoded = BackupCodec.decode(json.toByteArray())
        assertEquals("Hola", decoded.notes.single().title)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = """{"format":"bloc-de-notas-backup","version":1,"futuro":123,"notes":[]}"""
        assertTrue(BackupCodec.decode(json.toByteArray()).notes.isEmpty())
    }

    @Test
    fun otherFormatsAreRejected() {
        expect(BackupException.Reason.NOT_A_BACKUP) {
            BackupCodec.decode("""{"format":"otra-cosa","version":1}""".toByteArray())
        }
        expect(BackupException.Reason.NOT_A_BACKUP) {
            BackupCodec.decode("esto no es json".toByteArray())
        }
    }

    @Test
    fun newerVersionIsRejected() {
        expect(BackupException.Reason.UNSUPPORTED_VERSION) {
            BackupCodec.decode("""{"format":"bloc-de-notas-backup","version":99,"notes":[]}""".toByteArray())
        }
    }

    @Test
    fun mapperCleansUpBadData() {
        val file = BackupFile(
            notes = listOf(
                BackupNote(id = "", title = "sin id"),
                BackupNote(id = "a", kind = "RARO", colorIndex = 99, items = listOf(BackupItem("x", "uno"))),
                BackupNote(id = "a", title = "repetida"),
                BackupNote(id = "b", items = listOf(BackupItem("x", "mismo id de elemento"))),
            ),
        )
        val (notes, items) = BackupMapper.toEntities(file)
        assertEquals(listOf("a", "b"), notes.map { it.id })
        assertEquals(NoteKind.NOTE, notes.first().kind)
        assertEquals(5, notes.first().colorIndex)
        assertEquals(2, items.size)
        assertEquals(2, items.map { it.id }.toSet().size) // los ids de elemento no se repiten
    }

    private fun expect(reason: BackupException.Reason, block: () -> Unit) {
        try {
            block()
            fail("Debía lanzar BackupException($reason)")
        } catch (e: BackupException) {
            assertEquals(reason, e.reason)
        }
    }
}
