package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.CustomTemplate
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.SavedView
import io.github.estebangocoalt.blocdenotas.data.TaskQuery
import io.github.estebangocoalt.blocdenotas.data.ViewScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupViewsTest {
    @Test fun viewsTemplatesAndAislesSurviveARoundTrip() {
        val note = NoteEntity(id = "n1", title = "Compra", kind = NoteKind.SHOPPING, createdAt = 1, updatedAt = 1)
        val items = mapOf("n1" to listOf(ChecklistItemEntity(id = "i1", noteId = "n1", text = "pilas", position = 0, aisle = "OTHER")))
        val views = listOf(
            SavedView(id = "v1", name = "Urgentes", scope = ViewScope.BOARD, boardId = "b1", tasks = TaskQuery(minPriority = 3)),
            SavedView(id = "v2", name = "Agenda casa", scope = ViewScope.CALENDAR, agenda = true, tasks = TaskQuery(tag = "casa")),
        )
        val templates = listOf(CustomTemplate(id = "t1", name = "Lunes", kind = "SHOPPING", items = listOf("pan", "leche")))
        val file = BackupMapper.toBackup(listOf(note), items, "t", 1L).copy(views = views, templates = templates)

        val back = BackupCodec.decode(BackupCodec.encode(file))
        assertEquals(views, back.views)
        assertEquals(templates, back.templates)
        assertEquals("OTHER", back.notes.single().items.single().aisle)
        assertEquals("OTHER", BackupMapper.toEntities(back).second.single().aisle)
    }

    @Test fun oldBackupWithoutViewsOrTemplatesStillLoads() {
        val json = """{"format":"$BACKUP_FORMAT","version":1,"notes":[]}"""
        val back = BackupCodec.decode(json.toByteArray())
        assertTrue(back.views.isEmpty())
        assertTrue(back.templates.isEmpty())
    }

    @Test fun invalidAisleInBackupIsDropped() {
        val file = BackupFile(
            notes = listOf(
                BackupNote(id = "n", title = "x", items = listOf(BackupItem(id = "i", text = "a", aisle = "NOPE"))),
            ),
        )
        assertEquals("", BackupMapper.toEntities(file).second.single().aisle)
    }
}

class BackupAttachmentsTest {
    @Test fun attachmentFilesTravelInsideTheZip() {
        val file = BackupFile(notes = emptyList())
        val files = mapOf("a.jpg" to byteArrayOf(1, 2, 3), "b.m4a" to byteArrayOf(9))
        val bytes = BackupCodec.encode(file, files)
        val back = BackupCodec.decodeAttachments(bytes)
        assertEquals(setOf("a.jpg", "b.m4a"), back.keys)
        assertTrue(back.getValue("a.jpg").contentEquals(byteArrayOf(1, 2, 3)))
        assertEquals(file.notes, BackupCodec.decode(bytes).notes)
    }

    @Test fun unsafeEntryNamesAreIgnored() {
        val zip = BackupZip.packAll(
            mapOf(
                BACKUP_ENTRY to "{}".toByteArray(),
                "attachments/../evil.jpg" to byteArrayOf(1),
                "attachments/sub/x.jpg" to byteArrayOf(1),
                "otra/ok.jpg" to byteArrayOf(1),
                "attachments/ok.jpg" to byteArrayOf(2),
            ),
        )
        assertEquals(setOf("ok.jpg"), BackupCodec.decodeAttachments(zip).keys)
    }

    @Test fun onlyUsableAttachmentsAreImported() {
        val file = BackupFile(
            notes = listOf(BackupNote(id = "n1", title = "x")),
            attachments = listOf(
                BackupAttachment(id = "1", noteId = "n1", kind = "IMAGE", fileName = "ok.jpg"),
                BackupAttachment(id = "2", noteId = "n1", kind = "IMAGE", fileName = "missing.jpg"),
                BackupAttachment(id = "3", noteId = "zz", kind = "IMAGE", fileName = "ok.jpg"),
                BackupAttachment(id = "4", noteId = "n1", kind = "VIDEO", fileName = "ok.jpg"),
                BackupAttachment(id = "5", noteId = "n1", kind = "AUDIO", fileName = "../x.m4a"),
            ),
        )
        val result = BackupMapper.attachmentsOf(file, setOf("n1"), setOf("ok.jpg", "../x.m4a"))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test fun oldBackupWithoutAttachmentsHasNone() {
        val back = BackupCodec.decode("""{"format":"$BACKUP_FORMAT","version":1,"notes":[]}""".toByteArray())
        assertTrue(back.attachments.isEmpty())
    }
}
