package io.github.estebangocoalt.blocdenotas.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {

    @Query("SELECT * FROM notes WHERE inbox = 0 AND deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    abstract fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE inbox = 1 AND deletedAt IS NULL ORDER BY createdAt DESC")
    abstract fun observeInbox(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE inbox = 1 AND deletedAt IS NULL")
    abstract fun observeInboxCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    abstract fun observeTrash(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET deletedAt = :stamp, updatedAt = :stamp WHERE id = :id")
    abstract suspend fun trashNote(id: String, stamp: Long)

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :now WHERE id = :id")
    abstract suspend fun restoreNote(id: String, now: Long)

    @Query("SELECT id FROM notes WHERE parentId = :id AND deletedAt IS NULL")
    abstract suspend fun liveChildIds(id: String): List<String>

    @Query("SELECT id FROM notes WHERE parentId = :id AND deletedAt = :stamp")
    abstract suspend fun trashedChildIds(id: String, stamp: Long): List<String>

    @Query("DELETE FROM checklist_items WHERE noteId IN (SELECT id FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :before)")
    abstract suspend fun purgeTrashItems(before: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    abstract suspend fun purgeTrashNotes(before: Long)

    @Query("SELECT * FROM checklist_items ORDER BY noteId, position")
    abstract fun observeItems(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getNote(id: String): NoteEntity?

    @Query("SELECT * FROM checklist_items WHERE noteId = :id ORDER BY position")
    abstract suspend fun getItems(id: String): List<ChecklistItemEntity>

    @Query("SELECT * FROM notes")
    abstract suspend fun allNotes(): List<NoteEntity>

    @Query("SELECT * FROM checklist_items")
    abstract suspend fun allItems(): List<ChecklistItemEntity>

    @Query("SELECT id, updatedAt FROM notes")
    abstract suspend fun allStamps(): List<IdStamp>

    @Upsert
    abstract suspend fun upsertNote(note: NoteEntity)

    @Upsert
    abstract suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("UPDATE notes SET completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    abstract suspend fun setCompleted(id: String, completedAt: Long?, now: Long)

    // ---- Tablero ----

    @Query("SELECT * FROM board_columns ORDER BY position")
    abstract fun observeColumns(): Flow<List<BoardColumnEntity>>

    @Query("SELECT * FROM board_columns ORDER BY position")
    abstract suspend fun allColumns(): List<BoardColumnEntity>

    @Query("SELECT COUNT(*) FROM board_columns")
    abstract suspend fun columnCount(): Int

    @Upsert
    abstract suspend fun upsertColumns(columns: List<BoardColumnEntity>)

    @Query("DELETE FROM board_columns WHERE id = :id")
    abstract suspend fun deleteColumnRow(id: String)

    @Query("DELETE FROM board_columns")
    abstract suspend fun deleteAllColumns()

    @Query("UPDATE notes SET columnId = :to, columnPosition = 0 WHERE columnId = :id")
    abstract suspend fun detachCards(id: String, to: String?)

    /** Borra la columna; sus tarjetas no se pierden: pasan a [to] (null = primera columna del primer tablero). */
    @Transaction
    open suspend fun removeColumn(id: String, to: String?) {
        detachCards(id, to)
        deleteColumnRow(id)
    }

    @Query("SELECT * FROM boards ORDER BY position, id")
    abstract fun observeBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards ORDER BY position, id")
    abstract suspend fun allBoards(): List<BoardEntity>

    @Query("SELECT COUNT(*) FROM boards")
    abstract suspend fun boardCount(): Int

    @Upsert
    abstract suspend fun upsertBoards(boards: List<BoardEntity>)

    @Query("DELETE FROM boards")
    abstract suspend fun deleteAllBoards()

    @Query("DELETE FROM boards WHERE id = :id")
    abstract suspend fun deleteBoardRow(id: String)

    @Query("UPDATE notes SET columnId = NULL, columnPosition = 0 WHERE columnId IN (:ids)")
    abstract suspend fun detachCardsIn(ids: List<String>)

    @Query("DELETE FROM board_columns WHERE id IN (:ids)")
    abstract suspend fun deleteColumnsIn(ids: List<String>)

    /** Borra el tablero y sus columnas; las tarjetas no se pierden: pasan al primer tablero. */
    @Transaction
    open suspend fun removeBoard(id: String, columnIds: List<String>) {
        if (columnIds.isNotEmpty()) {
            detachCardsIn(columnIds)
            deleteColumnsIn(columnIds)
        }
        deleteBoardRow(id)
    }

    /** Crea el tablero por defecto (y sus columnas) solo si todavía no hay ningún tablero. */
    @Transaction
    open suspend fun seedBoard(board: BoardEntity, columns: List<BoardColumnEntity>) {
        if (boardCount() > 0) return
        upsertBoards(listOf(board))
        if (columnCount() == 0) upsertColumns(columns)
    }

    @Upsert
    abstract suspend fun upsertItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :id")
    abstract suspend fun clearItems(id: String)

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun deleteNote(id: String)

    @Query("DELETE FROM notes")
    abstract suspend fun deleteAllNotes()

    @Query("DELETE FROM checklist_items WHERE noteId LIKE 'demo-%'")
    abstract suspend fun deleteDemoItems()

    @Query("DELETE FROM notes WHERE id LIKE 'demo-%'")
    abstract suspend fun deleteDemoNotes()

    @Query("SELECT COUNT(*) FROM notes WHERE id LIKE 'demo-%'")
    abstract suspend fun demoCount(): Int

    @Transaction
    open suspend fun saveWithItems(
        note: NoteEntity,
        items: List<ChecklistItemEntity>,
        attachments: List<AttachmentEntity>? = null,
    ) {
        upsertNote(note)
        clearItems(note.id)
        upsertItems(items)
        // null = no tocar los adjuntos (guardados desde widgets, captura rápida…).
        if (attachments != null) {
            clearAttachments(note.id)
            if (attachments.isNotEmpty()) upsertAttachments(attachments)
        }
    }

    // ---- Adjuntos ----

    @Query("SELECT * FROM attachments ORDER BY noteId, position")
    abstract fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY noteId, position")
    abstract suspend fun allAttachments(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE noteId = :id ORDER BY position")
    abstract suspend fun getAttachments(id: String): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE noteId = :id")
    abstract suspend fun clearAttachments(id: String)

    @Upsert
    abstract suspend fun upsertAttachments(items: List<AttachmentEntity>)

    /**
     * Aplica una importación dentro de una sola transacción.
     *
     * - [replace] = true: borra todo y deja solo lo importado.
     * - [replace] = false (combinar): añade las notas que no existen y actualiza
     *   las existentes solo si la copia es más reciente. Nunca borra nada.
     */
    @Transaction
    open suspend fun applyImport(
        notes: List<NoteEntity>,
        items: List<ChecklistItemEntity>,
        replace: Boolean,
        columns: List<BoardColumnEntity> = emptyList(),
        boards: List<BoardEntity> = emptyList(),
        attachments: List<AttachmentEntity> = emptyList(),
    ): ImportResult {
        if (replace) {
            deleteAllNotes()
            deleteAllColumns()
            deleteAllBoards()
        }
        if (boards.isNotEmpty()) upsertBoards(boards)
        if (columns.isNotEmpty()) upsertColumns(columns)
        val local: Map<String, Long> =
            if (replace) emptyMap() else allStamps().associate { it.id to it.updatedAt }
        val itemsByNote = items.groupBy { it.noteId }
        val attachmentsByNote = attachments.groupBy { it.noteId }

        var added = 0
        var updated = 0
        var skipped = 0
        for (note in notes) {
            val localStamp = local[note.id]
            when {
                localStamp == null -> added++
                note.updatedAt > localStamp -> updated++
                else -> {
                    skipped++
                    continue
                }
            }
            upsertNote(note)
            clearItems(note.id)
            upsertItems(itemsByNote[note.id].orEmpty())
            clearAttachments(note.id)
            attachmentsByNote[note.id]?.let { upsertAttachments(it) }
        }
        return ImportResult(added, updated, skipped)
    }
}
