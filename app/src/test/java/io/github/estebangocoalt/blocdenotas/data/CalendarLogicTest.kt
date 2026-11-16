package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {

    @Test
    fun novemberTwentyTwentySixFromMonday() {
        // 1 nov 2026 es domingo: con el lunes como primer día hay 6 días de relleno y el mes ocupa 6 filas.
        val grid = CalendarLogic.monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(42, grid.size)
        assertEquals(LocalDate.of(2026, 10, 26), grid.first())
        assertTrue(grid.contains(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun sundayFirstWeekStartsOnTheFirstOfTheMonth() {
        val grid = CalendarLogic.monthGrid(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), grid.first())
        assertEquals(35, grid.size)
    }

    @Test
    fun februaryOfALeapAndCommonYear() {
        assertEquals(28, CalendarLogic.monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY).size) // feb 2026 empieza en domingo: 4 filas
        assertEquals(35, CalendarLogic.monthGrid(YearMonth.of(2024, 2), DayOfWeek.MONDAY).size) // bisiesto
    }

    @Test
    fun everyGridContainsEveryDayOfTheMonthOnce() {
        for (month in 1..12) {
            val ym = YearMonth.of(2026, month)
            val inMonth = CalendarLogic.monthGrid(ym, DayOfWeek.MONDAY).filter { YearMonth.from(it) == ym }
            assertEquals(ym.lengthOfMonth(), inMonth.size)
        }
    }

    @Test
    fun weekDaysStartWhereAsked() {
        assertEquals(DayOfWeek.MONDAY, CalendarLogic.weekDays(DayOfWeek.MONDAY).first())
        assertEquals(DayOfWeek.SUNDAY, CalendarLogic.weekDays(DayOfWeek.MONDAY).last())
        assertEquals(DayOfWeek.SATURDAY, CalendarLogic.weekDays(DayOfWeek.SUNDAY).last())
    }

    @Test
    fun tasksAreGroupedByDueDayIgnoringUndated() {
        val day = LocalDate.of(2026, 11, 20).toEpochDay()
        fun task(id: String, due: Long?) = NoteEntity(id = id, kind = NoteKind.TASK, dueAt = due, createdAt = 0, updatedAt = 0)
        val grouped = CalendarLogic.tasksByDay(
            listOf(
                task("a", TaskLogic.dueAtOfDay(day)),
                task("b", TaskLogic.dueAtOfDay(day)),
                task("c", TaskLogic.dueAtOfDay(day + 1)),
                task("d", null),
            ),
        )
        assertEquals(listOf("a", "b"), grouped.getValue(day).map { it.id })
        assertEquals(listOf("c"), grouped.getValue(day + 1).map { it.id })
        assertEquals(2, grouped.size)
    }
}
