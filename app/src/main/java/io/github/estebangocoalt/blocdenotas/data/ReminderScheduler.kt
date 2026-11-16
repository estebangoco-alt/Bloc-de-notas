package io.github.estebangocoalt.blocdenotas.data

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Programa o cancela el aviso de una tarea. La implementación real usa AlarmManager. */
interface ReminderScheduler {
    /** Programa el aviso de [note], o lo cancela si ya no corresponde (hecha, sin fecha, pasado). */
    fun sync(note: NoteEntity)

    fun cancel(noteId: String)

    /** No hace nada: para tests y para cuando no hay Android. */
    object None : ReminderScheduler {
        override fun sync(note: NoteEntity) = Unit
        override fun cancel(noteId: String) = Unit
    }
}

/** Reglas de los recordatorios. Lógica pura (sin Android) para probarla con tests de JVM. */
object ReminderLogic {
    const val DEFAULT_MINUTE = 9 * 60 // 09:00

    /**
     * Instante (en milisegundos) en que debe sonar el aviso, o null si no hay que programar nada:
     * no es tarea, no tiene fecha ni hora de aviso, ya está hecha o la hora ya pasó.
     * El día es el de la fecha límite (UTC, ver [TaskLogic]) y la hora es local.
     */
    fun triggerAt(note: NoteEntity, nowMillis: Long, zone: ZoneId): Long? {
        if (note.kind != NoteKind.TASK || note.completedAt != null) return null
        val due = note.dueAt ?: return null
        val minute = note.reminderMinute ?: return null
        if (minute !in 0..1439) return null
        val date = Instant.ofEpochMilli(due).atZone(ZoneOffset.UTC).toLocalDate()
        val at = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
        return at.takeIf { it > nowMillis }
    }
}
