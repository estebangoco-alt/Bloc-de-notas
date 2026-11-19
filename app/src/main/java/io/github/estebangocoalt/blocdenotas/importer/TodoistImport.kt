package io.github.estebangocoalt.blocdenotas.importer

import io.github.estebangocoalt.blocdenotas.data.BoardColumnEntity
import io.github.estebangocoalt.blocdenotas.data.BoardEntity
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID

class TodoistResult(
    val board: BoardEntity,
    val columns: List<BoardColumnEntity>,
    val notes: List<NoteEntity>,
    val items: List<ChecklistItemEntity>,
    /** Tareas cuya fecha no se ha podido entender (se importan sin fecha). */
    val unparsedDates: Int,
)

/** Fecha y repetición de una tarea de Todoist. */
data class TodoistDate(val dueDay: Long?, val repeat: RepeatRule)

/**
 * Importa el CSV que Todoist exporta por proyecto (TYPE, CONTENT, DESCRIPTION, PRIORITY, INDENT, DATE…).
 * Cada proyecto se convierte en un tablero; las secciones, en columnas; las subtareas, en casillas
 * de su tarea; los comentarios, en el texto de la tarea. Lógica pura (sin Android) para tests de JVM.
 */
object TodoistImport {

    /** Lee CSV (RFC 4180): comillas, comillas dobles escapadas y saltos de línea dentro de un campo. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("﻿")
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.setLength(0) }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
                    row.add(field.toString()); field.setLength(0)
                    if (row.any { it.isNotEmpty() }) rows.add(row)
                    row = ArrayList()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotEmpty() }) rows.add(row)
        }
        return rows
    }

    /** Todoist: 1 = urgente (p1) … 4 = sin prioridad. */
    fun priority(raw: String): Int = when (raw.trim()) {
        "1" -> Priority.HIGH
        "2" -> Priority.MEDIUM
        "3" -> Priority.LOW
        else -> Priority.NONE
    }

    private fun fold(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private val WEEKDAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo", "dilluns", "dimarts", "dimecres", "dijous", "divendres", "dissabte", "diumenge")
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", "ene", "abr", "ago", "dic", "gen", "des")

    /** Repetición a partir del texto («every day», «cada semana», «every 1st»…). */
    fun repeatOf(text: String): RepeatRule {
        val t = fold(text).trim().removeSuffix("!").replace("every!", "every")
        val starts = listOf("every", "cada", "tots", "tots els", "todos", "daily", "weekly", "monthly", "yearly", "diari", "setman", "mensual", "anual")
        if (starts.none { t.startsWith(it) }) return RepeatRule.NONE
        return when {
            Regex("\\b(year|ano|any|annual|anual|yearly)\\b").containsMatchIn(t) -> RepeatRule.YEARLY
            Regex("\\b(month|mes|monthly|mensual)\\b").containsMatchIn(t) -> RepeatRule.MONTHLY
            Regex("\\b(week|semana|setmana|weekly|semanal)\\b").containsMatchIn(t) || WEEKDAYS.any { t.contains(it) } -> RepeatRule.WEEKLY
            Regex("\\b(day|dia|daily|diario|diari|weekday|laborable)").containsMatchIn(t) -> RepeatRule.DAILY
            // «every 1st», «every jan 5»…: mensual o anual según haya un mes nombrado.
            MONTHS.any { Regex("\\b$it").containsMatchIn(t) } -> RepeatRule.YEARLY
            Regex("\\d").containsMatchIn(t) -> RepeatRule.MONTHLY
            else -> RepeatRule.WEEKLY
        }
    }

    private val PATTERNS = listOf("uuuu-MM-dd", "d/M/uuuu", "d-M-uuuu", "d MMM uuuu", "d MMMM uuuu", "MMM d uuuu", "MMMM d uuuu", "MMM d, uuuu", "MMMM d, uuuu", "d MMM", "d MMMM", "MMM d", "MMMM d")

    private fun parseSingle(text: String, today: LocalDate): LocalDate? {
        val t = text.trim().replace(Regex("\\s+"), " ").replace(Regex("(?<=\\d)(st|nd|rd|th)\\b"), "")
        // Se quita la hora si la hay («2026-11-20 14:00», «2026-11-20T14:00:00»).
        val noTime = t.replace(Regex("[T ]\\d{1,2}:\\d{2}(:\\d{2})?(Z|[+-]\\d{2}:?\\d{2})?$"), "").trim()
        for (locale in listOf(Locale.ENGLISH, Locale("es"), Locale("ca"))) {
            for (pattern in PATTERNS) {
                val formatter = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
                    .parseDefaulting(ChronoField.YEAR, today.year.toLong()).toFormatter(locale)
                val parsed = runCatching { LocalDate.parse(noTime.replace(".", ""), formatter) }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    /** Fecha límite (día desde 1970) y repetición de la columna DATE. */
    fun parseDate(raw: String, today: LocalDate): TodoistDate {
        val text = raw.trim()
        if (text.isEmpty()) return TodoistDate(null, RepeatRule.NONE)
        val repeat = repeatOf(text)
        if (repeat != RepeatRule.NONE) {
            // Si trae una fecha de inicio («every day starting Nov 20») se usa; si no, empieza hoy.
            val start = Regex("(?:starting|desde|a partir del?)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val day = start?.let { parseSingle(it, today) } ?: today
            return TodoistDate(day.toEpochDay(), repeat)
        }
        return TodoistDate(parseSingle(text, today)?.toEpochDay(), RepeatRule.NONE)
    }

    private fun id(vararg parts: Any): String =
        UUID.nameUUIDFromBytes(("todoist:" + parts.joinToString(":")).toByteArray()).toString()

    /**
     * [project] es el nombre del proyecto (el del archivo); [defaultColumn] el nombre de la columna
     * para tareas sin sección; [boardPosition] el lugar del tablero nuevo entre los existentes.
     * Devuelve null si el CSV no tiene las columnas esperadas.
     */
    fun parse(
        csv: String,
        project: String,
        defaultColumn: String,
        boardPosition: Int,
        nowMs: Long,
        today: LocalDate,
    ): TodoistResult? {
        val rows = parseCsv(csv)
        val header = rows.firstOrNull()?.map { it.trim().uppercase() } ?: return null
        val iType = header.indexOf("TYPE")
        val iContent = header.indexOf("CONTENT")
        if (iType < 0 || iContent < 0) return null
        val iDesc = header.indexOf("DESCRIPTION")
        val iPriority = header.indexOf("PRIORITY")
        val iIndent = header.indexOf("INDENT")
        val iDate = header.indexOf("DATE")
        fun List<String>.at(i: Int) = if (i >= 0 && i < size) this[i].trim() else ""

        val board = BoardEntity(id = id("board", project), title = project.ifBlank { "Todoist" }, position = boardPosition)
        val columns = ArrayList<BoardColumnEntity>()
        val notes = ArrayList<NoteEntity>()
        val items = ArrayList<ChecklistItemEntity>()
        val perColumn = HashMap<String, Int>()
        var unparsed = 0
        var currentColumn: BoardColumnEntity? = null
        var lastTask: NoteEntity? = null

        fun newColumn(title: String): BoardColumnEntity =
            BoardColumnEntity(id = id("col", project, columns.size, title), title = title, position = columns.size, boardId = board.id)
                .also { columns += it }

        fun replaceLast(task: NoteEntity) {
            notes[notes.lastIndex] = task
            lastTask = task
        }

        rows.drop(1).forEachIndexed { index, row ->
            val type = row.at(iType).lowercase()
            val content = row.at(iContent)
            when (type) {
                "section" -> if (content.isNotEmpty()) currentColumn = newColumn(content)
                "task" -> {
                    if (content.isEmpty()) return@forEachIndexed
                    val indent = row.at(iIndent).toIntOrNull() ?: 1
                    if (indent > 1 && lastTask != null) {
                        val owner = lastTask!!
                        items += ChecklistItemEntity(
                            id = id("item", project, index), noteId = owner.id, text = content,
                            position = items.count { it.noteId == owner.id },
                        )
                    } else {
                        val column = currentColumn ?: newColumn(defaultColumn).also { currentColumn = it }
                        val date = parseDate(row.at(iDate), today)
                        if (row.at(iDate).isNotEmpty() && date.dueDay == null) unparsed++
                        val position = perColumn.merge(column.id, 1, Int::plus)!! - 1
                        val task = NoteEntity(
                            id = id("task", project, index, content),
                            title = content,
                            body = row.at(iDesc),
                            kind = NoteKind.TASK,
                            colorIndex = NoteKind.TASK.defaultColor,
                            priority = priority(row.at(iPriority)),
                            dueAt = date.dueDay?.let { TaskLogic.dueAtOfDay(it) },
                            repeatRule = if (date.dueDay != null && date.repeat != RepeatRule.NONE) date.repeat.name else null,
                            columnId = column.id,
                            columnPosition = position,
                            createdAt = nowMs,
                            updatedAt = nowMs,
                        )
                        notes += task
                        lastTask = task
                    }
                }
                "note" -> {
                    val owner = lastTask
                    if (owner != null && content.isNotEmpty()) {
                        replaceLast(owner.copy(body = if (owner.body.isBlank()) content else owner.body + "\n\n" + content))
                    }
                }
            }
        }
        if (notes.isEmpty()) return null
        if (columns.isEmpty()) newColumn(defaultColumn)
        return TodoistResult(board, columns, notes, items, unparsed)
    }
}
