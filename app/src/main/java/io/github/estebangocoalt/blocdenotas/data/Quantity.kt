package io.github.estebangocoalt.blocdenotas.data

/** Un artículo de la lista de la compra separado en cantidad («2 kg») y nombre («arroz»). */
data class ShoppingEntry(val quantity: String?, val name: String)

/**
 * Detecta la cantidad escrita al principio o al final del artículo: «2 kg arroz», «3x leche»,
 * «leche x2», «6 huevos». Sin cambios en la base de datos: la cantidad sigue siendo parte del texto,
 * el modo compra solo la destaca. Lógica pura (sin Android) para probarla con tests de JVM.
 */
object QuantityLogic {
    private const val UNITS = "kg|kgs|g|gr|grs|mg|l|lt|lts|ml|cl|ud|uds|u|unidad|unidades|paquete|paquetes|pack|packs|bote|botes|lata|latas|bolsa|bolsas|botella|botellas|caja|cajas|docena|docenas|piece|pieces|pcs|oz|lb"
    private val LEADING = Regex("^(\\d+(?:[.,]\\d+)?)\\s*(?:[x×]\\s*)?($UNITS)?\\.?\\s+(?:de\\s+|d')?(.+)$", RegexOption.IGNORE_CASE)
    private val LEADING_X = Regex("^(\\d+)\\s*[x×]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val TRAILING = Regex("^(.+?)\\s+[x×]\\s*(\\d+(?:[.,]\\d+)?)$", RegexOption.IGNORE_CASE)

    fun parse(text: String): ShoppingEntry {
        val t = text.trim()
        LEADING_X.matchEntire(t)?.let { return ShoppingEntry("×" + it.groupValues[1], it.groupValues[2].trim()) }
        TRAILING.matchEntire(t)?.let { return ShoppingEntry("×" + it.groupValues[2], it.groupValues[1].trim()) }
        LEADING.matchEntire(t)?.let { m ->
            val number = m.groupValues[1]
            val unit = m.groupValues[2]
            val name = m.groupValues[3].trim()
            if (name.isNotEmpty()) return ShoppingEntry(if (unit.isEmpty()) number else "$number $unit", name)
        }
        return ShoppingEntry(null, t)
    }
}
