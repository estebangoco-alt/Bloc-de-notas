package io.github.estebangocoalt.blocdenotas.data

import java.text.Normalizer

/**
 * Pasillos del supermercado, en el orden en que suele recorrerse la tienda.
 * [keywords] sirven para adivinar el pasillo por el nombre del artículo (castellano, catalán e inglés).
 * Lógica pura (sin Android) para probarla con tests de JVM.
 */
enum class Aisle(val keywords: List<String>) {
    PRODUCE(listOf(
        "manzana", "pera", "platano", "naranja", "limon", "fresa", "uva", "melon", "sandia", "kiwi", "melocoton",
        "tomate", "lechuga", "cebolla", "ajo", "patata", "zanahoria", "pimiento", "calabacin", "pepino", "berenjena",
        "espinaca", "brocoli", "champi", "seta", "aguacate", "fruta", "verdura", "ensalada", "apple", "banana",
        "potato", "onion", "garlic", "lettuce", "carrot", "pomme", "poma",
    )),
    BAKERY(listOf("pan ", "pan", "barra", "baguette", "bollo", "croissant", "magdalena", "tostada", "bread", "pa ")),
    MEAT(listOf(
        "pollo", "ternera", "cerdo", "carne", "filete", "pavo", "salchicha", "hamburguesa", "jamon", "chorizo",
        "embutido", "bacon", "beicon", "lomo", "costilla", "chicken", "beef", "pork", "ham", "pernil", "vedella",
    )),
    FISH(listOf(
        "pescado", "merluza", "salmon", "atun", "bacalao", "gamba", "langostino", "mejillon", "calamar", "sepia",
        "pulpo", "sardina", "anchoa", "fish", "tuna", "shrimp", "peix",
    )),
    DAIRY(listOf(
        "leche", "yogur", "queso", "mantequilla", "nata", "huevo", "huevos", "flan", "natilla", "margarina",
        "milk", "cheese", "butter", "yogurt", "egg", "llet", "formatge", "ous", "mantega",
    )),
    PANTRY(listOf(
        "arroz", "pasta", "macarron", "espagueti", "fideo", "harina", "azucar", "sal ", "aceite", "vinagre",
        "legumbre", "lenteja", "garbanzo", "alubia", "conserva", "lata", "salsa", "cereal", "galleta", "cacao",
        "tomate frito", "mermelada", "miel", "cafe", "te ", "infusion", "frutos secos", "almendra", "nuez", "especia", "caldo",
        "rice", "flour", "sugar", "oil", "coffee", "tea", "cookie", "sauce", "arros", "sucre", "oli", "farina",
    )),
    DRINKS(listOf(
        "agua", "refresco", "cola", "zumo", "cerveza", "vino", "cava", "licor", "ginebra", "ron", "whisky",
        "bebida", "gaseosa", "tonica", "water", "juice", "beer", "wine", "suc", "cervesa", "vi ",
    )),
    FROZEN(listOf("congelado", "helado", "pizza", "croqueta", "nugget", "guisante congelado", "hielo", "frozen", "ice cream", "gelat")),
    CLEANING(listOf(
        "detergente", "lejia", "fregona", "estropajo", "limpia", "suavizante", "lavavajillas", "bolsa de basura",
        "basura", "papel de cocina", "servilleta", "cleaner", "bleach", "sponge", "detergent", "rentaplats",
    )),
    HYGIENE(listOf(
        "champu", "gel", "jabon", "pasta de dientes", "dentifrico", "cepillo", "desodorante", "papel higienico",
        "compresa", "panal", "toallita", "colonia", "crema", "algodon", "shampoo", "soap", "toothpaste", "xampu", "sabo",
    )),
    OTHER(emptyList());

    companion object {
        private val byName = entries.associateBy { it.name }

        /** Pasillo guardado, o null si está vacío o es desconocido. */
        fun fromName(name: String?): Aisle? = name?.let(byName::get)

        /** Saneado de lo que viene de una copia de seguridad: un nombre válido o vacío. */
        fun normalize(raw: String): String = fromName(raw.trim().uppercase())?.name.orEmpty()

        private fun fold(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")

        /**
         * Adivina el pasillo por el nombre. Primero se buscan las expresiones de varias palabras
         * («pasta de dientes» antes que «pasta») y después las palabras sueltas, en orden de pasillo.
         */
        fun guess(text: String): Aisle {
            val padded = " " + fold(text).trim() + " "
            if (padded.isBlank()) return OTHER
            for (phrases in listOf(true, false)) {
                for (aisle in entries) {
                    for (raw in aisle.keywords) {
                        val k = fold(raw).trim()
                        if ((' ' in k) != phrases) continue
                        // Las palabras cortas (≤3 letras) solo valen enteras: «te» no casa con «tomate».
                        val hit = if (k.length <= 3) padded.contains(" $k ") else padded.contains(k)
                        if (hit) return aisle
                    }
                }
            }
            return OTHER
        }

        /** El pasillo elegido a mano, o el adivinado si no se eligió ninguno. */
        fun resolve(text: String, stored: String): Aisle = fromName(stored) ?: guess(text)
    }
}

/** Agrupa los artículos de la compra por pasillo. */
object AisleLogic {
    /**
     * Devuelve los artículos agrupados por pasillo en el orden de [Aisle.entries]; dentro de cada
     * pasillo, los pendientes primero y luego los marcados, respetando la posición original.
     */
    fun group(items: List<ChecklistItemEntity>): List<Pair<Aisle, List<ChecklistItemEntity>>> =
        items.groupBy { Aisle.resolve(it.text, it.aisle) }
            .toSortedMap(compareBy<Aisle> { it.ordinal })
            .map { (aisle, list) -> aisle to list.sortedWith(compareBy<ChecklistItemEntity> { it.checked }.thenBy { it.position }) }
}
