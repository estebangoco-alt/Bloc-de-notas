package io.github.estebangocoalt.blocdenotas.data

enum class TaskBucket { OVERDUE, TODAY, UPCOMING, LATER, NO_DATE, DONE }

/**
 * Reglas de las tareas con fecha. Lógica pura (sin Android) para probarla con tests de JVM.
 *
 * La fecha límite se guarda como la medianoche UTC del día elegido (es lo que devuelve el
 * selector de fechas). Así "12 de noviembre" es el mismo día en cualquier zona horaria y
 * al viajar o cambiar de huso no se corre un día.
 */
object TaskLogic {
    const val MILLIS_PER_DAY = 86_400_000L
    const val UPCOMING_DAYS = 7

    fun dayOf(dueAt: Long): Long = Math.floorDiv(dueAt, MILLIS_PER_DAY)

    fun dueAtOfDay(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    /** Quita la hora de un instante y lo deja en la medianoche UTC de su día. */
    fun normalize(millis: Long): Long = dueAtOfDay(dayOf(millis))

    /** [todayEpochDay] es el día local de hoy (`LocalDate.now().toEpochDay()`). */
    fun bucket(dueAt: Long?, completedAt: Long?, todayEpochDay: Long): TaskBucket {
        if (completedAt != null) return TaskBucket.DONE
        if (dueAt == null) return TaskBucket.NO_DATE
        val day = dayOf(dueAt)
        return when {
            day < todayEpochDay -> TaskBucket.OVERDUE
            day == todayEpochDay -> TaskBucket.TODAY
            day <= todayEpochDay + UPCOMING_DAYS -> TaskBucket.UPCOMING
            else -> TaskBucket.LATER
        }
    }

    /** Agrupa y ordena las tareas. Solo incluye los grupos que tienen alguna. */
    fun group(tasks: List<NoteEntity>, todayEpochDay: Long): Map<TaskBucket, List<NoteEntity>> {
        val byBucket = tasks.groupBy { bucket(it.dueAt, it.completedAt, todayEpochDay) }
        val byDate = compareBy<NoteEntity> { it.dueAt ?: Long.MAX_VALUE }.thenBy { it.createdAt }
        val doneOrder = compareByDescending<NoteEntity> { it.completedAt ?: 0L }
        val result = LinkedHashMap<TaskBucket, List<NoteEntity>>()
        for (bucket in TaskBucket.entries) {
            val list = byBucket[bucket] ?: continue
            result[bucket] = list.sortedWith(if (bucket == TaskBucket.DONE) doneOrder else byDate)
        }
        return result
    }
}
