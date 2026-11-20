package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityLogicTest {
    @Test fun leadingNumberAndUnit() {
        assertEquals(ShoppingEntry("2 kg", "arroz"), QuantityLogic.parse("2 kg arroz"))
        assertEquals(ShoppingEntry("1,5 l", "leche"), QuantityLogic.parse("1,5 l de leche"))
        assertEquals(ShoppingEntry("6", "huevos"), QuantityLogic.parse("6 huevos"))
    }

    @Test fun timesForms() {
        assertEquals(ShoppingEntry("×3", "yogures"), QuantityLogic.parse("3x yogures"))
        assertEquals(ShoppingEntry("×2", "leche"), QuantityLogic.parse("leche x2"))
    }

    @Test fun noQuantity() {
        assertNull(QuantityLogic.parse("Pan").quantity)
        assertEquals("Pan", QuantityLogic.parse("  Pan ").name)
        assertNull(QuantityLogic.parse("7").quantity)
    }
}
