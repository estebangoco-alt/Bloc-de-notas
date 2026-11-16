package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exporta las notas a archivos Markdown (un `.md` por nota) con metadatos en formato YAML al
 * principio, compatibles con Obsidian y similares. Lógica pura (sin Android).
 */
object MarkdownExport {
    class MdFile(val path: String, val text: String)

    private val UNSAFE = Regex("[\\\\/:*?\"<>|\\r\\n\\t]+")

    /** Nombre de archivo seguro (sin ruta ni caracteres prohibidos). */
    fun fileName(title: String, fallback: String): String {
        val clean = title.replace(UNSAFE, " ").replace(Regex("\\s+"), " ").trim().trim('.').take(80).trim()
        return clean.ifEmpty { fallback }
    }

    private fun folder(note: NoteEntity): String = when {
        note.inbox -> "inbox"
        note.kind == NoteKind.IDEA -> "ideas"
        note.kind == NoteKind.SHOPPING -> "shopping"
        note.kind == NoteKind.TASK -> "tasks"
        else -> "notes"
    }

    private fun yaml(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** Texto Markdown de una nota. [parentTitle] es el título de su página madre, si la tiene. */
    fun render(
        note: NoteEntity,
        items: List<ChecklistItemEntity>,
        parentTitle: String?,
        untitled: String,
    ): String = buildString {
        val title = note.title.trim()
        appendLine("---")
        appendLine("title: ${yaml(title.ifEmpty { untitled })}")
        appendLine("kind: ${if (note.inbox) "inbox" else note.kind.name.lowercase()}")
        val tags = TagLogic.parse(note.tags)
        if (tags.isNotEmpty()) appendLine("tags: [${tags.joinToString(", ") { yaml(it) }}]")
        if (note.pinned) appendLine("pinned: true")
        if (note.priority != Priority.NONE) {
            appendLine("priority: ${listOf("", "low", "medium", "high")[Priority.clamp(note.priority)]}")
        }
        note.dueAt?.let { appendLine("due: ${Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()}") }
        note.reminderMinute?.let { appendLine("reminder: ${LocalTime.of(it / 60, it % 60)}") }
        val repeat = RepeatRule.fromName(note.repeatRule)
        if (repeat != RepeatRule.NONE) appendLine("repeat: ${repeat.name.lowercase()}")
        if (note.kind == NoteKind.TASK) appendLine("done: ${note.completedAt != null}")
        if (parentTitle != null) appendLine("parent: ${yaml("[[$parentTitle]]")}")
        appendLine("created: ${Instant.ofEpochMilli(note.createdAt)}")
        appendLine("updated: ${Instant.ofEpochMilli(note.updatedAt)}")
        appendLine("---")
        appendLine()
        if (title.isNotEmpty()) {
            appendLine("# $title")
            appendLine()
        }
        val body = note.body.trim()
        if (body.isNotEmpty()) {
            appendLine(body)
            appendLine()
        }
        items.sortedBy { it.position }.forEach { item ->
            appendLine("- [${if (item.checked) "x" else " "}] ${item.text}")
        }
    }.trimEnd() + "\n"

    /** Un archivo por nota (sin las de la papelera), en carpetas por tipo y con nombres únicos. */
    fun files(
        notes: List<NoteEntity>,
        items: List<ChecklistItemEntity>,
        untitled: String,
    ): List<MdFile> {
        val live = notes.filter { it.deletedAt == null }
        val titleOf = live.associate { it.id to it.title.trim() }
        val itemsByNote = items.groupBy { it.noteId }
        val used = HashSet<String>()
        return live.map { note ->
            val dir = folder(note)
            val base = fileName(note.title.ifBlank { note.body.lineSequence().firstOrNull().orEmpty() }, untitled)
            var name = base
            var n = 2
            while (!used.add("$dir/${name.lowercase()}")) name = "$base ($n)".also { n++ }
            val parent = note.parentId?.let { titleOf[it] }?.takeIf { it.isNotEmpty() }
            MdFile("$dir/$name.md", render(note, itemsByNote[note.id].orEmpty(), parent, untitled))
        }
    }

    fun pack(files: List<MdFile>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.path))
                zip.write(file.text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
