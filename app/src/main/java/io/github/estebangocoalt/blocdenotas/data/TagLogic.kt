package io.github.estebangocoalt.blocdenotas.data

/** Prioridades de una tarea. Se guardan como número (0 = sin prioridad). */
object Priority {
    const val NONE = 0
    const val LOW = 1
    const val MEDIUM = 2
    const val HIGH = 3

    fun clamp(value: Int): Int = value.coerceIn(NONE, HIGH)
}

/**
 * Etiquetas de una nota. Se guardan en un solo texto separado por comas; por eso una etiqueta
 * no puede llevar comas. Lógica pura (sin Android) para probarla con tests de JVM.
 */
object TagLogic {
    const val MAX_LENGTH = 24

    /** Limpia lo que escribe el usuario: sin «#», sin comas ni saltos de línea y con espacios simples. */
    fun normalize(raw: String): String =
        raw.replace(Regex("[,\\n\\r]+"), " ")
            .trim()
            .trimStart('#')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_LENGTH)

    fun parse(stored: String): List<String> =
        stored.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** Une las etiquetas en el texto guardado, sin repetidas (sin distinguir mayúsculas). */
    fun join(tags: List<String>): String =
        tags.map(::normalize).filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(",")

    fun add(stored: String, raw: String): String = join(parse(stored) + raw)

    fun remove(stored: String, tag: String): String =
        join(parse(stored).filterNot { it.equals(tag, ignoreCase = true) })

    /** Todas las etiquetas distintas de una lista de notas, ordenadas alfabéticamente. */
    fun all(stored: List<String>): List<String> =
        stored.flatMap(::parse).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
}
