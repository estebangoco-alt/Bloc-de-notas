package io.github.estebangocoalt.blocdenotas.data

/**
 * Enlaces entre notas con la sintaxis `[[Título]]` y comandos con «/». Lógica pura (sin Android)
 * para probarla con tests de JVM.
 */
object LinkLogic {
    private val LINK = Regex("\\[\\[([^\\[\\]\\n]{1,80})]]")
    private val SLASH = Regex("(?:^|\\s)/(\\S{0,20})$")

    /** Títulos enlazados en [text], sin repetir (sin distinguir mayúsculas) y en orden de aparición. */
    fun titles(text: String): List<String> =
        LINK.findAll(text).map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .toList()

    /** La nota que se llama [title] (sin distinguir mayúsculas), ignorando [except]. */
    fun find(title: String, notes: List<NoteEntity>, except: String? = null): NoteEntity? =
        notes.firstOrNull { it.id != except && it.title.trim().equals(title.trim(), ignoreCase = true) }

    /** Notas que enlazan o mencionan a [note] con `[[su título]]`. */
    fun backlinks(note: NoteEntity, all: List<NoteEntity>): List<NoteEntity> {
        val title = note.title.trim()
        if (title.isEmpty()) return emptyList()
        return all.filter { other ->
            other.id != note.id && titles(other.body).any { it.equals(title, ignoreCase = true) }
        }
    }

    fun link(title: String): String = "[[${title.trim()}]]"

    /** Si el texto termina en «/algo» (al principio o tras un espacio), devuelve «algo»; si no, null. */
    fun slashQuery(text: String): String? = SLASH.find(text)?.groupValues?.get(1)

    /** Quita del final el «/algo» que se estaba escribiendo. */
    fun removeSlash(text: String): String {
        val match = SLASH.find(text) ?: return text
        val start = match.range.first + if (match.value.startsWith("/")) 0 else 1
        return text.substring(0, start)
    }
}
