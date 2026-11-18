package io.github.estebangocoalt.blocdenotas.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Tipos de adjunto. Se guardan por nombre. */
enum class AttachmentKind(val extension: String) {
    IMAGE("jpg"),
    AUDIO("m4a"),
    ;

    companion object {
        fun fromName(name: String?): AttachmentKind? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Imagen o nota de voz de una nota. El archivo vive en el almacenamiento privado de la app
 * (carpeta `attachments`) con el nombre [fileName]; aquí solo se guarda la referencia.
 */
@Entity(
    tableName = "attachments",
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
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    /** Nombre de [AttachmentKind]. */
    val kind: String,
    val fileName: String,
    val position: Int = 0,
    @ColumnInfo(defaultValue = "0") val durationMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0,
) {
    val attachmentKind: AttachmentKind? get() = AttachmentKind.fromName(kind)
}

/** Nota de voz grabada en la captura rápida, pendiente de guardarse con su nota. */
data class CaptureAudio(val fileName: String, val durationMs: Long) {
    fun toEntity(noteId: String, now: Long) = AttachmentEntity(
        noteId = noteId, kind = AttachmentKind.AUDIO.name, fileName = fileName,
        durationMs = durationMs, createdAt = now,
    )
}

/** Reglas de los adjuntos. Lógica pura (sin Android) para probarla con tests de JVM. */
object AttachmentLogic {
    /** Lado mayor máximo de las imágenes guardadas: más que suficiente para leer y ocupa poco. */
    const val MAX_IMAGE_SIDE = 1600
    const val FOLDER = "attachments"

    private val SAFE_NAME = Regex("[A-Za-z0-9._-]{1,100}")

    /** Un nombre de archivo es seguro si no puede salirse de la carpeta de adjuntos. */
    fun isSafeName(name: String): Boolean =
        SAFE_NAME.matches(name) && !name.startsWith(".") && !name.contains("..")

    fun newFileName(kind: AttachmentKind): String = UUID.randomUUID().toString() + "." + kind.extension

    /** Tamaño final de una imagen de [width]×[height] para que su lado mayor no pase de [max]. */
    fun scaledSize(width: Int, height: Int, max: Int = MAX_IMAGE_SIDE): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 1 to 1
        val longest = maxOf(width, height)
        if (longest <= max) return width to height
        val scale = max.toDouble() / longest
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    /** Mm:ss de una duración. */
    fun formatDuration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
    }
}
