package io.github.estebangocoalt.blocdenotas.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Tipo de nota. [defaultColor] es el índice de color que se aplica al crearla. */
enum class NoteKind(val defaultColor: Int) {
    NOTE(0),
    IDEA(3),
    SHOPPING(1),
    TASK(4),
    ;

    companion object {
        fun fromName(name: String?): NoteKind = entries.firstOrNull { it.name == name } ?: NOTE
    }
}

/**
 * Los ids son UUID (texto) y no números autoincrementales para poder
 * combinar copias de seguridad de distintos dispositivos sin colisiones.
 *
 * Una nota con [inbox] = true está en la bandeja: guarda el texto bruto en [body]
 * hasta que se clasifica.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val kind: NoteKind = NoteKind.NOTE,
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val inbox: Boolean = false,
    /** Fecha límite (solo tareas): medianoche UTC del día elegido. Ver [TaskLogic]. */
    val dueAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    /** Cuándo se marcó como hecha (solo tareas). null = pendiente. */
    val completedAt: Long? = null,
    /** Hora del recordatorio (minutos desde las 00:00, hora local) el día de la fecha límite. null = sin aviso. */
    val reminderMinute: Int? = null,
    /** Columna del tablero donde está la tarjeta (solo tareas). null = primera columna. */
    val columnId: String? = null,
    @ColumnInfo(defaultValue = "0") val columnPosition: Int = 0,
)

/** Columna del tablero estilo Trello. */
@Entity(tableName = "board_columns")
data class BoardColumnEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val position: Int = 0,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val text: String,
    val checked: Boolean = false,
    val position: Int = 0,
)

data class NoteWithItems(
    val note: NoteEntity,
    val items: List<ChecklistItemEntity>,
)

/** Resultado ligero de `SELECT id, updatedAt` para decidir qué importar. */
data class IdStamp(
    val id: String,
    val updatedAt: Long,
)

data class ImportResult(
    val added: Int,
    val updated: Int,
    val skipped: Int,
)
