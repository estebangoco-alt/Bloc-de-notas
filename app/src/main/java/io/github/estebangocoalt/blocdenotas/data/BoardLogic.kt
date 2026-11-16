package io.github.estebangocoalt.blocdenotas.data

/**
 * Reglas del tablero estilo Trello. Lógica pura (sin Android) para probarla con tests de JVM.
 *
 * Las tarjetas son notas de tipo tarea. Una tarjeta sin columna (o con una columna que ya
 * no existe) se muestra en la primera columna, así nunca se pierde ninguna.
 */
object BoardLogic {

    /**
     * Columnas del tablero [boardId]. Las columnas sin tablero (o de un tablero desconocido)
     * pertenecen al primer tablero de [boards]. Sin tableros, devuelve todas.
     */
    fun columnsOf(boards: List<BoardEntity>, columns: List<BoardColumnEntity>, boardId: String): List<BoardColumnEntity> {
        if (boards.isEmpty()) return columns
        val known = boards.map { it.id }.toSet()
        val isFirst = boardId == boards.first().id
        return columns.filter { if (it.boardId in known) it.boardId == boardId else isFirst }
    }

    /**
     * Tarjetas que se ven en un tablero: las que están en una de sus columnas y, solo en el
     * primer tablero, las que no tienen columna (o la perdieron).
     */
    fun cardsOf(
        allColumns: List<BoardColumnEntity>,
        boardColumns: List<BoardColumnEntity>,
        isFirstBoard: Boolean,
        cards: List<NoteEntity>,
    ): List<NoteEntity> {
        val all = allColumns.map { it.id }.toSet()
        val mine = boardColumns.map { it.id }.toSet()
        return cards.filter { card ->
            val column = card.columnId
            if (column != null && column in all) column in mine else isFirstBoard
        }
    }

    /** Agrupa las tarjetas por columna y las ordena por posición (a igual posición, la más nueva primero). */
    fun group(columns: List<BoardColumnEntity>, cards: List<NoteEntity>): Map<String, List<NoteEntity>> {
        if (columns.isEmpty()) return emptyMap()
        val known = columns.map { it.id }.toSet()
        val first = columns.first().id
        val byColumn = cards.groupBy { card -> card.columnId?.takeIf { it in known } ?: first }
        val order = compareBy<NoteEntity> { it.columnPosition }.thenByDescending { it.createdAt }
        return columns.associate { column -> column.id to byColumn[column.id].orEmpty().sortedWith(order) }
    }

    /**
     * Mueve [cardId] a la columna [targetColumnId], en la posición [index] (0 = arriba).
     * Devuelve solo las tarjetas que cambian (la movida y las que se renumeran).
     */
    fun move(
        columns: List<BoardColumnEntity>,
        cards: List<NoteEntity>,
        cardId: String,
        targetColumnId: String,
        index: Int,
    ): List<NoteEntity> {
        val grouped = group(columns, cards)
        val moving = cards.firstOrNull { it.id == cardId } ?: return emptyList()
        if (targetColumnId !in grouped) return emptyList()

        val changed = LinkedHashMap<String, NoteEntity>()

        val target = grouped.getValue(targetColumnId).filter { it.id != cardId }.toMutableList()
        target.add(index.coerceIn(0, target.size), moving)
        target.forEachIndexed { i, card ->
            if (card.columnId != targetColumnId || card.columnPosition != i) {
                changed[card.id] = card.copy(columnId = targetColumnId, columnPosition = i)
            }
        }

        val sourceId = grouped.entries.firstOrNull { entry -> entry.value.any { it.id == cardId } }?.key
        if (sourceId != null && sourceId != targetColumnId) {
            grouped.getValue(sourceId).filter { it.id != cardId }.forEachIndexed { i, card ->
                if (card.columnId != sourceId || card.columnPosition != i) {
                    changed[card.id] = card.copy(columnId = sourceId, columnPosition = i)
                }
            }
        }
        return changed.values.toList()
    }

    /** Sube o baja una tarjeta dentro de su columna ([delta] = -1 sube, +1 baja). */
    fun shift(
        columns: List<BoardColumnEntity>,
        cards: List<NoteEntity>,
        cardId: String,
        delta: Int,
    ): List<NoteEntity> {
        val grouped = group(columns, cards)
        val (columnId, list) = grouped.entries.firstOrNull { e -> e.value.any { it.id == cardId } }
            ?: return emptyList()
        val current = list.indexOfFirst { it.id == cardId }
        val next = current + delta
        if (next < 0 || next > list.lastIndex) return emptyList()
        return move(columns, cards, cardId, columnId, next)
    }

    /** Mueve una columna a izquierda/derecha. Devuelve las columnas renumeradas que cambian. */
    fun shiftColumn(columns: List<BoardColumnEntity>, columnId: String, delta: Int): List<BoardColumnEntity> {
        val current = columns.indexOfFirst { it.id == columnId }
        val next = current + delta
        if (current < 0 || next < 0 || next > columns.lastIndex) return emptyList()
        val reordered = columns.toMutableList()
        reordered.add(next, reordered.removeAt(current))
        return reordered.mapIndexedNotNull { i, column ->
            if (column.position != i) column.copy(position = i) else null
        }
    }
}
