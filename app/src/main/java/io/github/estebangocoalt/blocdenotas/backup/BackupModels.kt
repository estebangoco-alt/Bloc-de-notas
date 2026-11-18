package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.data.Aisle
import io.github.estebangocoalt.blocdenotas.data.AttachmentEntity
import io.github.estebangocoalt.blocdenotas.data.AttachmentKind
import io.github.estebangocoalt.blocdenotas.data.AttachmentLogic
import io.github.estebangocoalt.blocdenotas.data.CustomTemplate
import io.github.estebangocoalt.blocdenotas.data.SavedView
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import io.github.estebangocoalt.blocdenotas.data.BoardColumnEntity
import io.github.estebangocoalt.blocdenotas.data.BoardEntity
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import kotlinx.serialization.Serializable
import java.util.UUID

const val BACKUP_FORMAT = "bloc-de-notas-backup"
const val BACKUP_VERSION = 1
const val BACKUP_ENTRY = "backup.json"

/** Formato documentado en docs/BACKUP_FORMAT.md. Si cambia, sube [BACKUP_VERSION]. */
@Serializable
data class BackupFile(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val notes: List<BackupNote> = emptyList(),
    /** Columnas del tablero (campo opcional añadido sin cambiar la versión del formato). */
    val columns: List<BackupColumn> = emptyList(),
    /** Tableros (uno por proyecto; campo opcional). Sin él, todas las columnas son del único tablero. */
    val boards: List<BackupBoard> = emptyList(),
    /** Vistas guardadas de Notas, Tareas, Tablero y Calendario (campo opcional). */
    val views: List<SavedView> = emptyList(),
    /** Plantillas propias (campo opcional). */
    val templates: List<CustomTemplate> = emptyList(),
    /** Imágenes y notas de voz (campo opcional). Los archivos van en la carpeta `attachments/` del ZIP. */
    val attachments: List<BackupAttachment> = emptyList(),
)

@Serializable
data class BackupAttachment(
    val id: String,
    val noteId: String,
    val kind: String = AttachmentKind.IMAGE.name,
    val fileName: String,
    val position: Int = 0,
    val durationMs: Long = 0,
    val createdAt: Long = 0,
)

@Serializable
data class BackupNote(
    val id: String,
    val title: String = "",
    val body: String = "",
    val kind: String = NoteKind.NOTE.name,
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val inbox: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val columnId: String? = null,
    val columnPosition: Int = 0,
    val completedAt: Long? = null,
    val reminderMinute: Int? = null,
    val repeat: String? = null,
    val priority: Int = 0,
    val tags: List<String> = emptyList(),
    val parentId: String? = null,
    val deletedAt: Long? = null,
    val items: List<BackupItem> = emptyList(),
)

@Serializable
data class BackupColumn(
    val id: String,
    val title: String = "",
    val position: Int = 0,
    val boardId: String = "",
)

@Serializable
data class BackupBoard(
    val id: String,
    val title: String = "",
    val position: Int = 0,
)

@Serializable
data class BackupItem(
    val id: String,
    val text: String = "",
    val checked: Boolean = false,
    val position: Int = 0,
    /** Pasillo (campo opcional). Vacío = se deduce del nombre. */
    val aisle: String = "",
)

object BackupMapper {
    private const val MAX_COLOR_INDEX = 5

    fun toBackup(
        notes: List<NoteEntity>,
        itemsByNote: Map<String, List<ChecklistItemEntity>>,
        appVersion: String,
        exportedAt: Long,
        columns: List<BoardColumnEntity> = emptyList(),
        boards: List<BoardEntity> = emptyList(),
        attachments: List<AttachmentEntity> = emptyList(),
    ): BackupFile = BackupFile(
        attachments = attachments.map {
            BackupAttachment(it.id, it.noteId, it.kind, it.fileName, it.position, it.durationMs, it.createdAt)
        },
        exportedAt = exportedAt,
        appVersion = appVersion,
        columns = columns.map { BackupColumn(it.id, it.title, it.position, it.boardId) },
        boards = boards.map { BackupBoard(it.id, it.title, it.position) },
        notes = notes.map { note ->
            BackupNote(
                id = note.id,
                title = note.title,
                body = note.body,
                kind = note.kind.name,
                colorIndex = note.colorIndex,
                pinned = note.pinned,
                inbox = note.inbox,
                dueAt = note.dueAt,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                columnId = note.columnId,
                columnPosition = note.columnPosition,
                completedAt = note.completedAt,
                reminderMinute = note.reminderMinute,
                repeat = note.repeatRule,
                priority = note.priority,
                tags = TagLogic.parse(note.tags),
                parentId = note.parentId,
                deletedAt = note.deletedAt,
                items = itemsByNote[note.id].orEmpty().sortedBy { it.position }.map { item ->
                    BackupItem(item.id, item.text, item.checked, item.position, item.aisle)
                },
            )
        },
    )

    /**
     * Convierte la copia en entidades de base de datos, saneando datos raros:
     * ids vacíos o repetidos, colores fuera de rango, tipos desconocidos.
     */
    /**
     * Adjuntos de la copia que se pueden usar: de una nota que se importa, con nombre de archivo
     * seguro, de un tipo conocido y con el archivo ya restaurado ([availableFiles]).
     */
    fun attachmentsOf(file: BackupFile, noteIds: Set<String>, availableFiles: Set<String>): List<AttachmentEntity> {
        val seen = HashSet<String>()
        return file.attachments.mapNotNull { a ->
            val kind = AttachmentKind.fromName(a.kind) ?: return@mapNotNull null
            if (a.noteId !in noteIds || a.fileName !in availableFiles || !AttachmentLogic.isSafeName(a.fileName)) return@mapNotNull null
            val id = if (a.id.isBlank() || !seen.add(a.id)) UUID.randomUUID().toString().also { seen.add(it) } else a.id
            AttachmentEntity(id, a.noteId, kind.name, a.fileName, a.position, a.durationMs, a.createdAt)
        }
    }

    /** Columnas del tablero de la copia, sin ids vacíos ni repetidos. */
    fun columnsOf(file: BackupFile): List<BoardColumnEntity> {
        val seen = HashSet<String>()
        return file.columns
            .filter { it.id.isNotBlank() && seen.add(it.id) }
            .map { BoardColumnEntity(id = it.id, title = it.title, position = it.position, boardId = it.boardId) }
    }

    /** Tableros de la copia, sin ids vacíos ni repetidos. */
    fun boardsOf(file: BackupFile): List<BoardEntity> {
        val seen = HashSet<String>()
        return file.boards
            .filter { it.id.isNotBlank() && seen.add(it.id) }
            .map { BoardEntity(id = it.id, title = it.title, position = it.position) }
    }

    fun toEntities(file: BackupFile): Pair<List<NoteEntity>, List<ChecklistItemEntity>> {
        val notes = ArrayList<NoteEntity>()
        val items = ArrayList<ChecklistItemEntity>()
        val seenNotes = HashSet<String>()
        val seenItems = HashSet<String>()

        for (n in file.notes) {
            if (n.id.isBlank() || !seenNotes.add(n.id)) continue
            notes += NoteEntity(
                id = n.id,
                title = n.title,
                body = n.body,
                kind = NoteKind.fromName(n.kind),
                colorIndex = n.colorIndex.coerceIn(0, MAX_COLOR_INDEX),
                pinned = n.pinned,
                inbox = n.inbox,
                dueAt = n.dueAt,
                createdAt = n.createdAt,
                updatedAt = n.updatedAt,
                columnId = n.columnId,
                columnPosition = n.columnPosition,
                completedAt = n.completedAt,
                reminderMinute = n.reminderMinute?.takeIf { it in 0..1439 },
                priority = Priority.clamp(n.priority),
                tags = TagLogic.join(n.tags),
                parentId = n.parentId?.takeIf { it.isNotBlank() && it != n.id },
                deletedAt = n.deletedAt,
                repeatRule = RepeatRule.fromName(n.repeat).takeIf { it != RepeatRule.NONE }?.name,
            )
            for (i in n.items) {
                // Si el id del elemento falta o ya existe en otra nota, se genera uno nuevo.
                val itemId = if (i.id.isBlank() || !seenItems.add(i.id)) {
                    UUID.randomUUID().toString().also { seenItems.add(it) }
                } else {
                    i.id
                }
                items += ChecklistItemEntity(
                    id = itemId,
                    noteId = n.id,
                    text = i.text,
                    checked = i.checked,
                    position = i.position,
                    aisle = Aisle.normalize(i.aisle),
                )
            }
        }
        return notes to items
    }
}
