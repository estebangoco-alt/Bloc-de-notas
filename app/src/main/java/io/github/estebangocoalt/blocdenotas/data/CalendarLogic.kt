package io.github.estebangocoalt.blocdenotas.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Reglas del calendario mensual. Lógica pura (sin Android) para probarla con tests de JVM. */
object CalendarLogic {

    /**
     * Días que se dibujan para [month]: semanas completas desde [firstDayOfWeek], con los días
     * del mes anterior y siguiente que completan la primera y la última semana (4 a 6 filas).
     */
    fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val weeks = (lead + month.lengthOfMonth() + 6) / 7
        val start = first.minusDays(lead.toLong())
        return List(weeks * 7) { start.plusDays(it.toLong()) }
    }

    /** Tareas con fecha límite agrupadas por día (clave: `LocalDate.toEpochDay()`). */
    fun tasksByDay(tasks: List<NoteEntity>): Map<Long, List<NoteEntity>> =
        tasks.mapNotNull { task -> task.dueAt?.let { TaskLogic.dayOf(it) to task } }
            .groupBy({ it.first }, { it.second })

    /** Cabeceras de los días de la semana empezando por [firstDayOfWeek]. */
    fun weekDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
        List(7) { firstDayOfWeek.plus(it.toLong()) }
}
