package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayLogicTest {
    @Test
    fun easterDates() {
        assertEquals(LocalDate.of(2024, 3, 31), HolidayLogic.easter(2024))
        assertEquals(LocalDate.of(2025, 4, 20), HolidayLogic.easter(2025))
        assertEquals(LocalDate.of(2026, 4, 5), HolidayLogic.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), HolidayLogic.easter(2027))
    }

    @Test
    fun spainHasNationalHolidaysAndGoodFriday() {
        val days = HolidayLogic.holidays(HolidayRegion.SPAIN, 2026)
        assertEquals("Viernes Santo", days.getValue(LocalDate.of(2026, 4, 3)).es)
        assertEquals("Navidad", days.getValue(LocalDate.of(2026, 12, 25)).es)
        assertNull(days[LocalDate.of(2026, 9, 11)])
        assertNull(days[LocalDate.of(2026, 4, 6)])
    }

    @Test
    fun cataloniaAddsItsOwn() {
        val days = HolidayLogic.holidays(HolidayRegion.CATALONIA, 2026)
        assertNotNull(days[LocalDate.of(2026, 9, 11)])
        assertNotNull(days[LocalDate.of(2026, 6, 24)])
        assertNotNull(days[LocalDate.of(2026, 12, 26)])
        assertNotNull(days[LocalDate.of(2026, 4, 6)]) // Lunes de Pascua
    }

    @Test
    fun cataloniaHasNoHolyThursdayButOtherRegionsDo() {
        val holyThursday = LocalDate.of(2026, 4, 2)
        assertNull(HolidayLogic.holidays(HolidayRegion.CATALONIA, 2026)[holyThursday])
        assertNull(HolidayLogic.holidays(HolidayRegion.SPAIN, 2026)[holyThursday])
        listOf(HolidayRegion.MADRID, HolidayRegion.ANDALUCIA, HolidayRegion.GALICIA).forEach {
            assertNotNull(it.name, HolidayLogic.holidays(it, 2026)[holyThursday])
        }
    }

    @Test
    fun regionalDays() {
        fun has(region: HolidayRegion, month: Int, day: Int) =
            HolidayLogic.holidays(region, 2026).containsKey(LocalDate.of(2026, month, day))
        assertTrue(has(HolidayRegion.MADRID, 5, 2))
        assertTrue(has(HolidayRegion.ANDALUCIA, 2, 28))
        assertTrue(has(HolidayRegion.VALENCIA, 10, 9))
        assertTrue(has(HolidayRegion.GALICIA, 7, 25))
        assertTrue(has(HolidayRegion.CASTILLA_LA_MANCHA, 6, 4)) // Corpus Christi 2026
        assertTrue(!has(HolidayRegion.MADRID, 9, 11))
    }

    @Test
    fun everyRegionHasTheCommonHolidays() {
        HolidayRegion.entries.filter { it != HolidayRegion.NONE }.forEach { region ->
            val days = HolidayLogic.holidays(region, 2026)
            listOf(1 to 1, 6 to 1, 1 to 5, 15 to 8, 12 to 10, 8 to 12, 25 to 12).forEach { (d, m) ->
                assertTrue("$region $d/$m", days.containsKey(LocalDate.of(2026, m, d)))
            }
        }
    }

    @Test
    fun noRegionMeansNoHolidays() {
        assertTrue(HolidayLogic.holidays(HolidayRegion.NONE, 2026).isEmpty())
    }

    @Test
    fun everyDayHasASaintIncludingLeapDay() {
        assertEquals(listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31), Santoral.sizes())
        assertEquals("San José", Santoral.of(LocalDate.of(2026, 3, 19)))
        assertEquals("San Osvaldo", Santoral.of(LocalDate.of(2028, 2, 29)))
    }
}
