package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AisleTest {
    @Test fun guessesCommonItems() {
        assertEquals(Aisle.DAIRY, Aisle.guess("Leche entera"))
        assertEquals(Aisle.PRODUCE, Aisle.guess("Plátanos"))
        assertEquals(Aisle.BAKERY, Aisle.guess("Pan"))
        assertEquals(Aisle.MEAT, Aisle.guess("pechuga de pollo"))
        assertEquals(Aisle.FISH, Aisle.guess("Merluza"))
        assertEquals(Aisle.DRINKS, Aisle.guess("Agua"))
        assertEquals(Aisle.CLEANING, Aisle.guess("Lejía"))
        assertEquals(Aisle.HYGIENE, Aisle.guess("Champú"))
    }

    @Test fun shortKeywordsMatchOnlyWholeWords() {
        // "te" (infusión) no debe casar con "tomate"; "tomate" es verdura.
        assertEquals(Aisle.PRODUCE, Aisle.guess("tomate"))
        assertEquals(Aisle.PANTRY, Aisle.guess("té verde"))
    }

    @Test fun unknownGoesToOther() {
        assertEquals(Aisle.OTHER, Aisle.guess("pilas AA"))
        assertEquals(Aisle.OTHER, Aisle.guess(""))
    }

    @Test fun storedAisleWinsOverGuess() {
        assertEquals(Aisle.FROZEN, Aisle.resolve("leche", "FROZEN"))
        assertEquals(Aisle.DAIRY, Aisle.resolve("leche", ""))
        assertEquals(Aisle.DAIRY, Aisle.resolve("leche", "NOPE"))
    }

    @Test fun normalizeKeepsOnlyValidNames() {
        assertEquals("MEAT", Aisle.normalize(" meat "))
        assertEquals("", Aisle.normalize("xx"))
        assertNull(Aisle.fromName(""))
    }

    @Test fun groupsInStoreOrderUncheckedFirst() {
        fun item(id: String, text: String, checked: Boolean = false, pos: Int = 0, aisle: String = "") =
            ChecklistItemEntity(id = id, noteId = "n", text = text, checked = checked, position = pos, aisle = aisle)
        val groups = AisleLogic.group(listOf(
            item("1", "pollo", pos = 0),
            item("2", "leche", checked = true, pos = 1),
            item("3", "yogur", pos = 2),
            item("4", "manzanas", pos = 3),
        ))
        assertEquals(listOf(Aisle.PRODUCE, Aisle.MEAT, Aisle.DAIRY), groups.map { it.first })
        assertEquals(listOf("3", "2"), groups.last().second.map { it.id })
    }
}
