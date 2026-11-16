package io.github.estebangocoalt.blocdenotas.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {

    @Query("SELECT * FROM notes WHERE inbox = 0 ORDER BY pinned DESC, updatedAt DESC")
    abstract fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE inbox = 1 ORDER BY createdAt DESC")
    abstract fun observeInbox(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE inbox = 1")
    abstract fun observeInboxCount(): Flow<Int>

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

    @Query("UPDATE notes SET columnId = NULL, columnPosition = 0 WHERE columnId = :id")
    abstract suspend fun detachCards(id: String)

    /** Borra la columna; sus tarjetas no se pierden: pasan a la primera columna. */
    @Transaction
    open suspend fun removeColumn(id: String) {
        detachCards(id)
        deleteColumnRow(id)
    }

    /** Crea las columnas por defecto solo si todavía no hay ninguna. */
    @Transaction
    open suspend fun seedColumns(columns: List<BoardColumnEntity>) {
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

    @Transaction
    open suspend fun saveWithItems(note: NoteEntity, items: List<ChecklistItemEntity>) {
        upsertNote(note)
        clearItems(note.id)
        upsertItems(items)
    }

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
    ): ImportResult {
        if (replace) {
            deleteAllNotes()
            deleteAllColumns()
        }
        if (columns.isNotEmpty()) upsertColumns(columns)
        val local: Map<String, Long> =
            if (replace) emptyMap() else allStamps().associate { it.id to it.updatedAt }
        val itemsByNote = items.groupBy { it.noteId }

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
        }
        return ImportResult(added, updated, skipped)
    }
}
