package io.github.estebangocoalt.blocdenotas.data

import io.github.estebangocoalt.blocdenotas.backup.BackupFile
import io.github.estebangocoalt.blocdenotas.backup.BackupMapper
import io.github.estebangocoalt.blocdenotas.backup.ImportMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A dónde va un texto capturado rápido. [kind] = null significa "bandeja de entrada". */
enum class CaptureDestination(val kind: NoteKind?) {
    INBOX(null),
    NOTE(NoteKind.NOTE),
    TASK(NoteKind.TASK),
    IDEA(NoteKind.IDEA),
    SHOPPING(NoteKind.SHOPPING),
}

class NoteRepository(
    private val dao: NoteDao,
    private val reminders: ReminderScheduler = ReminderScheduler.None,
) {

    /**
     * Ámbito que sobrevive a las pantallas. Sirve para guardar al salir del editor,
     * desde el widget o al compartir, aunque la actividad se cierre enseguida.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val notes: Flow<List<NoteWithItems>> =
        combine(dao.observeNotes(), dao.observeItems()) { notes, items ->
            val byNote = items.groupBy { it.noteId }
            notes.map { NoteWithItems(it, byNote[it.id].orEmpty()) }
        }

    val columns: Flow<List<BoardColumnEntity>> = dao.observeColumns()

    val inbox: Flow<List<NoteEntity>> = dao.observeInbox()
    val inboxCount: Flow<Int> = dao.observeInboxCount()

    suspend fun get(id: String): NoteWithItems? {
        val note = dao.getNote(id) ?: return null
        return NoteWithItems(note, dao.getItems(id))
    }

    suspend fun save(note: NoteEntity, items: List<ChecklistItemEntity>) {
        dao.saveWithItems(note, items)
        reminders.sync(note)
    }

    suspend fun delete(id: String) {
        dao.deleteNote(id)
        reminders.cancel(id)
    }


    /** Vuelve a programar los avisos pendientes. Solo se usa al reiniciar el móvil, que borra las alarmas. */
    suspend fun rescheduleAll() {
        dao.allNotes().forEach { reminders.sync(it) }
    }

    /** Guarda un texto capturado. Devuelve false si estaba en blanco. */
    suspend fun capture(raw: String, destination: CaptureDestination): Boolean {
        if (raw.isBlank()) return false
        val now = System.currentTimeMillis()
        val kind = destination.kind
        if (kind == null) {
            dao.upsertNote(NoteEntity(body = raw.trim(), inbox = true, createdAt = now, updatedAt = now))
            return true
        }
        saveParsed(NoteEntity(kind = kind, createdAt = now, updatedAt = now), raw)
        return true
    }

    /** Saca una nota de la bandeja y la convierte en una nota normal del tipo elegido. */
    suspend fun classify(id: String, kind: NoteKind) {
        val inboxNote = dao.getNote(id) ?: return
        saveParsed(
            inboxNote.copy(kind = kind, inbox = false, updatedAt = System.currentTimeMillis()),
            inboxNote.body,
        )
    }

    private suspend fun saveParsed(base: NoteEntity, raw: String) {
        val parsed = CaptureParser.parse(raw, base.kind)
        val note = base.copy(
            title = parsed.title,
            body = parsed.body,
            colorIndex = base.kind.defaultColor,
        )
        val items = parsed.items.mapIndexed { index, text ->
            ChecklistItemEntity(noteId = note.id, text = text, position = index)
        }
        dao.saveWithItems(note, items)
    }

    suspend fun setCompleted(id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        dao.setCompleted(id, if (done) now else null, now)
        dao.getNote(id)?.let { reminders.sync(it) }
    }

    // ---- Tablero ----

    suspend fun seedColumns(titles: List<String>) =
        dao.seedColumns(titles.mapIndexed { i, t -> BoardColumnEntity(title = t, position = i) })

    suspend fun addColumn(title: String, position: Int) =
        dao.upsertColumns(listOf(BoardColumnEntity(title = title, position = position)))

    suspend fun saveColumns(columns: List<BoardColumnEntity>) = dao.upsertColumns(columns)

    suspend fun deleteColumn(id: String) = dao.removeColumn(id)

    suspend fun addCard(columnId: String, title: String, position: Int) {
        val now = System.currentTimeMillis()
        dao.upsertNote(
            NoteEntity(
                title = title,
                kind = NoteKind.TASK,
                colorIndex = NoteKind.TASK.defaultColor,
                columnId = columnId,
                columnPosition = position,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** Guarda tarjetas movidas o reordenadas (actualiza su fecha para que la copia de seguridad las vea). */
    suspend fun saveCards(cards: List<NoteEntity>) {
        val now = System.currentTimeMillis()
        dao.upsertNotes(cards.map { it.copy(updatedAt = now) })
    }

    // ---- Copia de seguridad ----

    suspend fun buildBackup(appVersion: String, exportedAt: Long): BackupFile {
        val notes = dao.allNotes()
        val items = dao.allItems().groupBy { it.noteId }
        return BackupMapper.toBackup(notes, items, appVersion, exportedAt, dao.allColumns())
    }

    suspend fun importBackup(file: BackupFile, mode: ImportMode): ImportResult {
        val (notes, items) = BackupMapper.toEntities(file)
        return dao.applyImport(
            notes, items,
            replace = mode == ImportMode.REPLACE,
            columns = BackupMapper.columnsOf(file),
        )
    }
}
