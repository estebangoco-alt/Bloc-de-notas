package io.github.estebangocoalt.blocdenotas.data

/** Una barra de la línea de tiempo: una tarea con fecha, de [startDay] a [endDay] (ambos incluidos, días desde 1970). */
data class TimelineRow(
    val id: String,
    val title: String,
    val startDay: Long,
    val endDay: Long,
    val done: Boolean,
    /** Tiene dependencias pendientes. */
    val blocked: Boolean,
    val priority: Int,
    val colorIndex: Int,
    /** Títulos de las tareas de las que depende (las que existen). */
    val dependsOn: List<String>,
) {
    val days: Int get() = (endDay - startDay + 1).toInt()
}

data class Timeline(
    val firstDay: Long,
    val lastDay: Long,
    val rows: List<TimelineRow>,
    /** Tareas sin fecha: no salen en la línea. */
    val undated: Int,
) {
    val totalDays: Int get() = (lastDay - firstDay + 1).toInt()
}

/**
 * Línea de tiempo de tareas. Cada tarea acaba en su fecha límite y empieza:
 * - el día siguiente al final de la última tarea de la que depende (si tiene fecha), o
 * - el día en que se creó, como máximo [MAX_DEFAULT_DAYS] días antes del final.
 * Lógica pura (sin Android) para probarla con tests de JVM.
 */
object TimelineLogic {
    const val MAX_DEFAULT_DAYS = 14
    const val MIN_WINDOW_DAYS = 14

    fun build(tasks: List<NoteEntity>, todayEpochDay: Long): Timeline {
        val byId = tasks.associateBy { it.id }
        val dated = tasks.filter { it.dueAt != null }
        val rows = dated.map { task ->
            val end = TaskLogic.dayOf(task.dueAt!!)
            val deps = DependencyLogic.dependencies(task, byId)
            val depEnd = deps.mapNotNull { it.dueAt?.let(TaskLogic::dayOf) }.maxOrNull()
            val start = if (depEnd != null) {
                minOf(end, depEnd + 1)
            } else {
                val created = TaskLogic.dayOf(task.createdAt)
                minOf(end, maxOf(created, end - (MAX_DEFAULT_DAYS - 1)))
            }
            TimelineRow(
                id = task.id,
                title = task.title,
                startDay = start,
                endDay = end,
                done = task.completedAt != null,
                blocked = DependencyLogic.blockers(task, byId).isNotEmpty(),
                priority = task.priority,
                colorIndex = task.colorIndex,
                dependsOn = deps.map { it.title },
            )
        }.sortedWith(compareBy<TimelineRow> { it.startDay }.thenBy { it.endDay }.thenBy { it.title.lowercase() })

        var first = minOf(rows.minOfOrNull { it.startDay } ?: todayEpochDay, todayEpochDay) - 1
        var last = maxOf(rows.maxOfOrNull { it.endDay } ?: todayEpochDay, todayEpochDay) + 2
        if (last - first + 1 < MIN_WINDOW_DAYS) last = first + MIN_WINDOW_DAYS - 1
        return Timeline(first, last, rows, tasks.size - dated.size)
    }
}
