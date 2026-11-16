package io.github.estebangocoalt.blocdenotas.data

import java.time.LocalDate

/** Cada cuánto se repite una tarea. Se guarda por su nombre (texto) en la base de datos y en la copia. */
enum class RepeatRule {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY,
    ;

    companion object {
        fun fromName(name: String?): RepeatRule = entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** Reglas de las tareas que se repiten. Lógica pura (sin Android) para probarla con tests de JVM. */
object RepeatLogic {
    private const val MAX_STEPS = 20_000

    /**
     * Día (desde 1970) de la siguiente ocurrencia tras [dueDay], o null si [rule] es NONE.
     *
     * La siguiente nunca es anterior a hoy: si la tarea llevaba tiempo vencida se salta a la
     * primera ocurrencia desde [todayEpochDay]. Los cálculos parten siempre de la fecha original,
     * así «el 31 de cada mes» vuelve al 31 después de febrero en vez de quedarse en el 28.
     */
    fun nextDay(dueDay: Long, rule: RepeatRule, todayEpochDay: Long): Long? {
        if (rule == RepeatRule.NONE) return null
        val base = LocalDate.ofEpochDay(dueDay)
        for (k in 1L..MAX_STEPS) {
            val candidate = when (rule) {
                RepeatRule.DAILY -> base.plusDays(k)
                RepeatRule.WEEKLY -> base.plusWeeks(k)
                RepeatRule.MONTHLY -> base.plusMonths(k)
                RepeatRule.YEARLY -> base.plusYears(k)
                RepeatRule.NONE -> return null
            }.toEpochDay()
            if (candidate >= todayEpochDay) return candidate
        }
        return null
    }

    /**
     * Si [note] es una tarea que se repite y se acaba de completar, devuelve la tarea ya
     * reprogramada (nueva fecha, pendiente de nuevo). Si no se repite, devuelve null.
     */
    fun advance(note: NoteEntity, todayEpochDay: Long, now: Long): NoteEntity? {
        val rule = RepeatRule.fromName(note.repeatRule)
        val due = note.dueAt ?: return null
        if (note.kind != NoteKind.TASK || rule == RepeatRule.NONE) return null
        val next = nextDay(TaskLogic.dayOf(due), rule, todayEpochDay) ?: return null
        return note.copy(dueAt = TaskLogic.dueAtOfDay(next), completedAt = null, updatedAt = now)
    }
}
