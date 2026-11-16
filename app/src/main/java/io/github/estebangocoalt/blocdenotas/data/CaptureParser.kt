package io.github.estebangocoalt.blocdenotas.data

/** Resultado de convertir un texto capturado en título, cuerpo y elementos de lista. */
data class ParsedCapture(
    val title: String,
    val body: String,
    val items: List<String>,
)

/**
 * Reparte un texto capturado rápido según el tipo de nota de destino.
 * Es lógica pura (sin Android) para poder probarla con tests de JVM.
 */
object CaptureParser {
    const val MAX_TITLE = 80

    private val bullet = Regex("^[-*•]\\s*")

    fun parse(raw: String, kind: NoteKind): ParsedCapture {
        val lines = raw
            .replace("\r\n", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedCapture("", "", emptyList())

        if (kind == NoteKind.SHOPPING) {
            val items = lines.map { it.replace(bullet, "").trim() }.filter { it.isNotEmpty() }
            return ParsedCapture("", "", items)
        }

        val first = lines.first()
        return if (first.length <= MAX_TITLE) {
            ParsedCapture(first, lines.drop(1).joinToString("\n"), emptyList())
        } else {
            // Primera línea demasiado larga para ser un título: todo va al cuerpo, sin perder texto.
            ParsedCapture("", lines.joinToString("\n"), emptyList())
        }
    }
}
