package io.github.estebangocoalt.blocdenotas.importer

import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.util.UUID

/** Archivo adjunto de una nota de Keep: [name] es el nombre del archivo dentro del ZIP de Takeout. */
data class KeepAttachment(val name: String, val isAudio: Boolean)

class KeepNote(
    val note: NoteEntity,
    val items: List<ChecklistItemEntity>,
    val attachments: List<KeepAttachment>,
)

/**
 * Importa notas de Google Keep desde Google Takeout (un archivo `.json` por nota).
 * Lógica pura (sin Android) para probarla con tests de JVM. Los ids salen del contenido, así que
 * importar dos veces el mismo Takeout no duplica nada.
 */
object KeepImport {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Etiqueta que se pone a las notas archivadas en Keep. */
    const val ARCHIVED_TAG = "archivada"

    /** Color de Keep → índice de los colores de la app (0 amarillo, 1 menta, 2 melocotón, 3 lila, 4 cielo, 5 neutro). */
    fun colorIndex(keepColor: String?): Int = when (keepColor?.uppercase()) {
        "YELLOW" -> 0
        "GREEN", "TEAL" -> 1
        "RED", "ORANGE", "BROWN" -> 2
        "PURPLE", "PINK" -> 3
        "BLUE", "CERULEAN", "DARK BLUE" -> 4
        else -> 5
    }

    /** True si el texto parece una nota de Keep (para ignorar otros `.json` del ZIP). */
    fun looksLikeNote(text: String): Boolean = runCatching {
        val o = json.parseToJsonElement(text) as? JsonObject ?: return false
        o.containsKey("userEditedTimestampUsec") || o.containsKey("textContent") || o.containsKey("listContent")
    }.getOrDefault(false)

    /** Convierte una nota. Devuelve null si está en la papelera de Keep, está vacía o no es una nota. */
    fun parse(text: String, nowMs: Long): KeepNote? {
        val o = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        if (!looksLikeNote(text)) return null
        if (o.bool("isTrashed")) return null

        val title = o.str("title").trim()
        val body = o.str("textContent").trim()
        val list = (o["listContent"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val attachments = (o["attachments"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.mapNotNull { a ->
            val path = a.str("filePath").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val mime = a.str("mimetype")
            when {
                mime.startsWith("image/") -> KeepAttachment(path, isAudio = false)
                mime.startsWith("audio/") -> KeepAttachment(path, isAudio = true)
                else -> null
            }
        }
        if (title.isEmpty() && body.isEmpty() && list.isEmpty() && attachments.isEmpty()) return null

        val created = (o.num("createdTimestampUsec") / 1000).takeIf { it > 0 } ?: nowMs
        val edited = (o.num("userEditedTimestampUsec") / 1000).takeIf { it > 0 } ?: created

        val tags = buildList {
            (o["labels"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.str("name") }.forEach { add(it) }
            if (o.bool("isArchived")) add(ARCHIVED_TAG)
        }
        val id = UUID.nameUUIDFromBytes("keep:$created:$title:${body.take(60)}:${list.firstOrNull()?.str("text").orEmpty()}".toByteArray()).toString()

        val note = NoteEntity(
            id = id,
            title = title,
            body = body,
            kind = NoteKind.NOTE,
            colorIndex = colorIndex(o.str("color").ifBlank { null }),
            pinned = o.bool("isPinned"),
            tags = TagLogic.join(tags),
            createdAt = created,
            updatedAt = edited,
        )
        val items = list.mapIndexedNotNull { index, item ->
            val t = item.str("text").trim()
            if (t.isEmpty()) null else ChecklistItemEntity(
                id = UUID.nameUUIDFromBytes("$id:item:$index".toByteArray()).toString(),
                noteId = id, text = t, checked = item.bool("isChecked"), position = index,
            )
        }
        return KeepNote(note, items, attachments)
    }

    private fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false
    private fun JsonObject.num(key: String): Long = (this[key] as? JsonPrimitive)?.doubleOrNull?.toLong() ?: 0L
}
