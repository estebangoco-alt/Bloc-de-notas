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
    val boards: Flow<List<BoardEntity>> = dao.observeBoards()

    val inbox: Flow<List<NoteEntity>> = dao.observeInbox()
    val inboxCount: Flow<Int> = dao.observeInboxCount()

    suspend fun get(id: String): NoteWithItems? {
        val note = dao.getNote(id) ?: return null
        return NoteWithItems(note, dao.getItems(id))
    }

    /** Todos los adjuntos, para mostrarlos y para la copia de seguridad. */
    val attachments: Flow<List<AttachmentEntity>> = dao.observeAttachments()

    suspend fun attachmentsOf(noteId: String): List<AttachmentEntity> = dao.getAttachments(noteId)

    suspend fun allAttachmentFileNames(): Set<String> = dao.allAttachments().map { it.fileName }.toSet()

    /** [attachments] = null deja los adjuntos como están. */
    suspend fun save(note: NoteEntity, items: List<ChecklistItemEntity>, attachments: List<AttachmentEntity>? = null) {
        dao.saveWithItems(note, items, attachments)
        reminders.sync(note)
    }

    /** Envía la nota (y sus subpáginas) a la papelera. Se puede recuperar durante [TRASH_DAYS] días. */
    suspend fun delete(id: String) {
        trashTree(id, System.currentTimeMillis())
    }

    private suspend fun trashTree(id: String, stamp: Long) {
        dao.trashNote(id, stamp)
        reminders.cancel(id)
        dao.liveChildIds(id).forEach { trashTree(it, stamp) }
    }

    /** Borra del todo, sin pasar por la papelera. */
    suspend fun deletePermanently(id: String) {
        dao.deleteNote(id)
        reminders.cancel(id)
    }

    val trash: Flow<List<NoteEntity>> = dao.observeTrash()

    suspend fun restore(id: String) {
        val note = dao.getNote(id) ?: return
        val stamp = note.deletedAt ?: return
        // Si su página madre sigue en la papelera, vuelve como nota de primer nivel.
        val parent = note.parentId?.let { dao.getNote(it) }
        if (note.parentId != null && (parent == null || parent.deletedAt != null)) {
            dao.upsertNote(note.copy(parentId = null))
        }
        restoreTree(id, stamp)
    }

    private suspend fun restoreTree(id: String, stamp: Long) {
        dao.restoreNote(id, System.currentTimeMillis())
        dao.getNote(id)?.let { reminders.sync(it) }
        dao.trashedChildIds(id, stamp).forEach { restoreTree(it, stamp) }
    }

    suspend fun emptyTrash() {
        dao.purgeTrashItems(Long.MAX_VALUE)
        dao.purgeTrashNotes(Long.MAX_VALUE)
    }

    /** Borra del todo lo que lleva más de [TRASH_DAYS] días en la papelera. */
    suspend fun purgeOldTrash(now: Long = System.currentTimeMillis()) {
        val before = now - TRASH_DAYS * 86_400_000L
        dao.purgeTrashItems(before)
        dao.purgeTrashNotes(before)
    }

    companion object {
        const val TRASH_DAYS = 30L
    }


    /** Vuelve a programar los avisos pendientes. Solo se usa al reiniciar el móvil, que borra las alarmas. */
    suspend fun rescheduleAll() {
        dao.allNotes().forEach { reminders.sync(it) }
    }

    /** Guarda un texto capturado. Devuelve false si estaba en blanco. */
    suspend fun capture(
        raw: String,
        destination: CaptureDestination,
        priority: Int = Priority.NONE,
        audio: CaptureAudio? = null,
        audioTitle: String = "",
    ): Boolean {
        if (raw.isBlank() && audio == null) return false
        // Una nota de voz sin texto lleva [audioTitle] como título.
        val text = raw.ifBlank { audioTitle }
        val now = System.currentTimeMillis()
        val kind = destination.kind
        if (kind == null) {
            val note = NoteEntity(body = text.trim(), inbox = true, createdAt = now, updatedAt = now)
            dao.saveWithItems(note, emptyList(), audio?.let { listOf(it.toEntity(note.id, now)) })
            return true
        }
        // La prioridad solo existe en las tareas.
        val level = if (kind == NoteKind.TASK) Priority.clamp(priority) else Priority.NONE
        saveParsed(NoteEntity(kind = kind, priority = level, createdAt = now, updatedAt = now), text, audio)
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

    private suspend fun saveParsed(base: NoteEntity, raw: String, audio: CaptureAudio? = null) {
        val parsed = CaptureParser.parse(raw, base.kind)
        val note = base.copy(
            title = parsed.title,
            body = parsed.body,
            colorIndex = base.kind.defaultColor,
        )
        val items = parsed.items.mapIndexed { index, text ->
            ChecklistItemEntity(noteId = note.id, text = text, position = index)
        }
        dao.saveWithItems(note, items, audio?.let { listOf(it.toEntity(note.id, System.currentTimeMillis())) })
    }

    suspend fun setCompleted(id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        val current = dao.getNote(id)
        // Una tarea que se repite no se queda «hecha»: pasa a su siguiente fecha, pendiente otra vez.
        val next = if (done && current != null) {
            RepeatLogic.advance(current, java.time.LocalDate.now().toEpochDay(), now)
        } else {
            null
        }
        if (next != null) {
            dao.upsertNote(next)
            dao.upsertItems(dao.getItems(id).map { it.copy(checked = false) })
            reminders.sync(next)
            return
        }
        dao.setCompleted(id, if (done) now else null, now)
        dao.getNote(id)?.let { reminders.sync(it) }
    }

    // ---- Tablero ----

    /** Primera vez: crea el tablero por defecto con sus columnas. */
    suspend fun seedBoard(boardTitle: String, columnTitles: List<String>) {
        val board = BoardEntity(title = boardTitle)
        dao.seedBoard(board, columnTitles.mapIndexed { i, t -> BoardColumnEntity(title = t, position = i, boardId = board.id) })
    }

    /** Crea un tablero nuevo con columnas iniciales y devuelve su id. */
    suspend fun addBoard(title: String, columnTitles: List<String>): String {
        val position = (dao.allBoards().maxOfOrNull { it.position } ?: -1) + 1
        val board = BoardEntity(title = title, position = position)
        dao.upsertBoards(listOf(board))
        dao.upsertColumns(columnTitles.mapIndexed { i, t -> BoardColumnEntity(title = t, position = i, boardId = board.id) })
        return board.id
    }

    suspend fun renameBoard(board: BoardEntity, title: String) = dao.upsertBoards(listOf(board.copy(title = title)))

    suspend fun deleteBoard(id: String) {
        val boards = dao.allBoards()
        if (boards.size < 2) return
        val columns = BoardLogic.columnsOf(boards, dao.allColumns(), id)
        dao.removeBoard(id, columns.map { it.id })
    }

    suspend fun addColumn(boardId: String, title: String, position: Int) =
        dao.upsertColumns(listOf(BoardColumnEntity(title = title, position = position, boardId = boardId)))

    suspend fun saveColumns(columns: List<BoardColumnEntity>) = dao.upsertColumns(columns)

    suspend fun deleteColumn(id: String, moveCardsTo: String?) = dao.removeColumn(id, moveCardsTo)

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

    /** Todas las notas y elementos, para exportar a Markdown. */
    suspend fun allForExport(): Pair<List<NoteEntity>, List<ChecklistItemEntity>> =
        dao.allNotes() to dao.allItems()

    // ---- Datos de ejemplo ----

    /** Carga (o recarga) los datos de ejemplo. No programa avisos. */
    suspend fun loadDemo(spanish: Boolean, todayEpochDay: Long) {
        seedBoard(
            if (spanish) "Tablero" else "Board",
            if (spanish) listOf("Por hacer", "En curso", "Hecho") else listOf("To do", "Doing", "Done"),
        )
        val boards = dao.allBoards()
        val columns = BoardLogic.columnsOf(boards, dao.allColumns(), boards.first().id).map { it.id }
        val demo = DemoData.build(spanish, columns, todayEpochDay, System.currentTimeMillis())
        dao.deleteDemoItems()
        dao.deleteDemoNotes()
        dao.upsertNotes(demo.notes)
        dao.upsertItems(demo.items)
    }

    suspend fun removeDemo() {
        dao.deleteDemoItems()
        dao.deleteDemoNotes()
    }

    // ---- Copia de seguridad ----

    suspend fun buildBackup(appVersion: String, exportedAt: Long): BackupFile {
        val notes = dao.allNotes()
        val items = dao.allItems().groupBy { it.noteId }
        return BackupMapper.toBackup(
            notes, items, appVersion, exportedAt, dao.allColumns(), dao.allBoards(),
            attachments = dao.allAttachments(),
        )
    }

    /** [availableFiles] son los nombres de adjuntos cuyo archivo se ha podido restaurar. */
    suspend fun importBackup(file: BackupFile, mode: ImportMode, availableFiles: Set<String> = emptySet()): ImportResult {
        val (notes, items) = BackupMapper.toEntities(file)
        return dao.applyImport(
            notes, items,
            replace = mode == ImportMode.REPLACE,
            columns = BackupMapper.columnsOf(file),
            boards = BackupMapper.boardsOf(file),
            attachments = BackupMapper.attachmentsOf(file, notes.map { it.id }.toSet(), availableFiles),
        )
    }
}
