package io.github.estebangocoalt.blocdenotas.data

/** Texto de las plantillas. Lógica pura (sin Android) para probarla con tests de JVM. */
object TemplateText {
    /** Una plantilla guarda sus elementos de lista como un único texto con un elemento por línea. */
    fun lines(raw: String): List<String> =
        raw.replace("\r\n", "\n").lines().map { it.trim() }.filter { it.isNotEmpty() }
}
