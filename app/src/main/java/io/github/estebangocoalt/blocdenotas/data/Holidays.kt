package io.github.estebangocoalt.blocdenotas.data

import java.time.LocalDate

/**
 * Zonas con festivos incluidos: toda España (solo nacionales) y cada comunidad autónoma, Ceuta y Melilla.
 * Los festivos locales de cada municipio no están. [CATALONIA] conserva su nombre por compatibilidad.
 */
enum class HolidayRegion {
    NONE, SPAIN,
    ANDALUCIA, ARAGON, ASTURIAS, BALEARES, CANARIAS, CANTABRIA, CASTILLA_LA_MANCHA, CASTILLA_Y_LEON,
    CATALONIA, EXTREMADURA, GALICIA, MADRID, MURCIA, NAVARRA, PAIS_VASCO, LA_RIOJA, VALENCIA, CEUTA, MELILLA,
}

/** Nombre de un festivo en castellano y en inglés. */
data class Holiday(val es: String, val en: String)

/**
 * Festivos que no dependen de internet: los fijos se escriben a mano y los que dependen de la Pascua
 * se calculan. Lógica pura (sin Android) para probarla con tests de JVM.
 */
object HolidayLogic {

    /** Festivos propios de una zona, además de los comunes a toda España. */
    private class Rules(
        val holyThursday: Boolean = true,
        val easterMonday: Boolean = false,
        val corpusChristi: Boolean = false,
        val fixed: List<Triple<Int, Int, Holiday>> = emptyList(),
    )

    private fun day(month: Int, day: Int, es: String, en: String) = Triple(month, day, Holiday(es, en))

    private val SAN_JOSE = day(3, 19, "San José", "St Joseph's Day")
    private val SANTIAGO = day(7, 25, "Santiago Apóstol", "St James' Day")
    private val SAN_JUAN = day(6, 24, "San Juan", "St John's Day")

    /** España solo tiene los comunes (sin Jueves Santo, que depende de la comunidad). */
    private val RULES: Map<HolidayRegion, Rules> = mapOf(
        HolidayRegion.SPAIN to Rules(holyThursday = false),
        HolidayRegion.ANDALUCIA to Rules(fixed = listOf(day(2, 28, "Día de Andalucía", "Andalusia Day"))),
        HolidayRegion.ARAGON to Rules(fixed = listOf(day(4, 23, "San Jorge, Día de Aragón", "St George's Day, Aragon Day"))),
        HolidayRegion.ASTURIAS to Rules(fixed = listOf(day(9, 8, "Día de Asturias", "Asturias Day"))),
        HolidayRegion.BALEARES to Rules(
            easterMonday = true,
            fixed = listOf(day(3, 1, "Día de les Illes Balears", "Balearic Islands Day"), day(12, 26, "San Esteban", "St Stephen's Day")),
        ),
        HolidayRegion.CANARIAS to Rules(fixed = listOf(day(5, 30, "Día de Canarias", "Canary Islands Day"))),
        HolidayRegion.CANTABRIA to Rules(
            fixed = listOf(
                day(7, 28, "Día de las Instituciones de Cantabria", "Cantabria Institutions Day"),
                day(9, 15, "La Bien Aparecida", "La Bien Aparecida"),
            ),
        ),
        HolidayRegion.CASTILLA_LA_MANCHA to Rules(
            corpusChristi = true,
            fixed = listOf(day(5, 9, "Día de Castilla-La Mancha", "Castilla-La Mancha Day")),
        ),
        HolidayRegion.CASTILLA_Y_LEON to Rules(fixed = listOf(day(4, 23, "Fiesta de Castilla y León", "Castile and León Day"))),
        HolidayRegion.CATALONIA to Rules(
            holyThursday = false,
            easterMonday = true,
            fixed = listOf(
                day(6, 24, "San Juan (Sant Joan)", "St John's Day"),
                day(9, 11, "Diada Nacional de Cataluña", "National Day of Catalonia"),
                day(12, 26, "San Esteban (Sant Esteve)", "St Stephen's Day"),
            ),
        ),
        HolidayRegion.EXTREMADURA to Rules(fixed = listOf(day(9, 8, "Día de Extremadura", "Extremadura Day"))),
        HolidayRegion.GALICIA to Rules(fixed = listOf(day(7, 25, "Día Nacional de Galicia (Santiago)", "Galicia National Day"))),
        HolidayRegion.MADRID to Rules(fixed = listOf(day(5, 2, "Fiesta de la Comunidad de Madrid", "Community of Madrid Day"))),
        HolidayRegion.MURCIA to Rules(fixed = listOf(SAN_JOSE, day(6, 9, "Día de la Región de Murcia", "Region of Murcia Day"))),
        HolidayRegion.NAVARRA to Rules(
            easterMonday = true,
            fixed = listOf(SAN_JOSE, day(12, 3, "San Francisco Javier", "St Francis Xavier's Day")),
        ),
        HolidayRegion.PAIS_VASCO to Rules(easterMonday = true, fixed = listOf(SAN_JOSE, SANTIAGO)),
        HolidayRegion.LA_RIOJA to Rules(easterMonday = true, fixed = listOf(day(6, 9, "Día de La Rioja", "La Rioja Day"))),
        // Comunitat Valenciana no tiene Jueves Santo; su Lunes de Pascua es el de San Vicente Ferrer.
        HolidayRegion.VALENCIA to Rules(
            holyThursday = false,
            easterMonday = true,
            fixed = listOf(SAN_JOSE, SAN_JUAN, day(10, 9, "Día de la Comunitat Valenciana", "Valencian Community Day")),
        ),
        // Las fiestas islámicas de Ceuta y Melilla cambian cada año y no se incluyen.
        HolidayRegion.CEUTA to Rules(
            fixed = listOf(day(8, 5, "Nuestra Señora de África", "Our Lady of Africa"), day(9, 2, "Día de Ceuta", "Ceuta Day")),
        ),
        HolidayRegion.MELILLA to Rules(fixed = listOf(day(9, 17, "Día de Melilla", "Melilla Day"))),
    )

    /** Domingo de Pascua (calendario gregoriano, algoritmo de Meeus/Jones/Butcher). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun holidays(region: HolidayRegion, year: Int): Map<LocalDate, Holiday> {
        if (region == HolidayRegion.NONE) return emptyMap()
        val easter = easter(year)
        val days = LinkedHashMap<LocalDate, Holiday>()
        fun add(date: LocalDate, es: String, en: String) {
            days[date] = Holiday(es, en)
        }
        fun fixed(month: Int, day: Int, es: String, en: String) = add(LocalDate.of(year, month, day), es, en)

        fixed(1, 1, "Año Nuevo", "New Year's Day")
        fixed(1, 6, "Epifanía del Señor (Reyes)", "Epiphany")
        add(easter.minusDays(2), "Viernes Santo", "Good Friday")
        fixed(5, 1, "Día del Trabajo", "Labour Day")
        fixed(8, 15, "Asunción de la Virgen", "Assumption of Mary")
        fixed(10, 12, "Fiesta Nacional de España", "National Day of Spain")
        fixed(11, 1, "Todos los Santos", "All Saints' Day")
        fixed(12, 6, "Día de la Constitución", "Constitution Day")
        fixed(12, 8, "Inmaculada Concepción", "Immaculate Conception")
        fixed(12, 25, "Navidad", "Christmas Day")

        val rules = RULES[region] ?: Rules()
        if (rules.holyThursday) add(easter.minusDays(3), "Jueves Santo", "Maundy Thursday")
        if (rules.easterMonday) add(easter.plusDays(1), "Lunes de Pascua", "Easter Monday")
        if (rules.corpusChristi) add(easter.plusDays(60), "Corpus Christi", "Corpus Christi")
        rules.fixed.forEach { (month, day, name) -> add(LocalDate.of(year, month, day), name.es, name.en) }
        return days
    }

    /** Festivos de varios años a la vez (un mes visible puede mostrar días del año anterior o siguiente). */
    fun holidays(region: HolidayRegion, years: Iterable<Int>): Map<LocalDate, Holiday> =
        years.distinct().fold(emptyMap()) { acc, year -> acc + holidays(region, year) }
}
