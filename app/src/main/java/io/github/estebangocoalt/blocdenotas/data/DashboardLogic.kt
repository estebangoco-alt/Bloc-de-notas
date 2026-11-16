package io.github.estebangocoalt.blocdenotas.data

/** Avance de un tablero: tarjetas totales y cuántas están hechas (última columna o marcadas como hechas). */
data class BoardProgress(val id: String, val title: String, val total: Int, val done: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** Datos del resumen (dashboard) con las cuatro secciones: notas, tareas, tablero y calendario. */
data class DashboardData(
    // Notas
    val notes: Int = 0,
    val pinned: Int = 0,
    val inbox: Int = 0,
    // Tareas
    val pendingTasks: Int = 0,
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val doneTasks: Int = 0,
    // Tablero
    val boardCards: Int = 0,
    val boards: List<BoardProgress> = emptyList(),
    // Calendario: tareas pendientes de hoy y de los 6 días siguientes
    val week: List<Int> = List(DashboardLogic.WEEK_DAYS) { 0 },
    // Listas
    val todayList: List<NoteEntity> = emptyList(),
    val recent: List<NoteEntity> = emptyList(),
) {
    val weekTotal: Int get() = week.sum()

    /** Porcentaje (0–100) de tarjetas hechas en todos los tableros. */
    val boardPercent: Int
        get() {
            val total = boards.sumOf { it.total }
            return if (total == 0) 0 else boards.sumOf { it.done } * 100 / total
        }
}

/** Calcula el resumen. Lógica pura (sin Android) para probarla con tests de JVM. */
object DashboardLogic {
    const val WEEK_DAYS = 7
    const val TODAY_LIMIT = 5
    const val RECENT_LIMIT = 4

    fun compute(
        notes: List<NoteEntity>,
        boards: List<BoardEntity>,
        columns: List<BoardColumnEntity>,
        todayEpochDay: Long,
    ): DashboardData {
        val live = notes.filter { it.deletedAt == null }
        val inbox = live.count { it.inbox }
        val regular = live.filter { !it.inbox }
        val plain = regular.filter { it.kind != NoteKind.TASK && it.parentId == null }
        val tasks = regular.filter { it.kind == NoteKind.TASK }
        val pending = tasks.filter { it.completedAt == null }

        fun dayOf(note: NoteEntity): Long? = note.dueAt?.let { TaskLogic.dayOf(it) }

        val week = List(WEEK_DAYS) { offset -> pending.count { dayOf(it) == todayEpochDay + offset } }
        val todayList = pending
            .filter { (dayOf(it) ?: Long.MAX_VALUE) <= todayEpochDay }
            .sortedWith(compareBy<NoteEntity> { it.dueAt }.thenByDescending { it.priority })
            .take(TODAY_LIMIT)

        val progress = boards.map { board ->
            val boardColumns = BoardLogic.columnsOf(boards, columns, board.id)
            val cards = BoardLogic.cardsOf(columns, boardColumns, board.id == boards.first().id, tasks)
            val grouped = BoardLogic.group(boardColumns, cards)
            val lastId = boardColumns.lastOrNull()?.id
            val doneIds = HashSet<String>()
            cards.filter { it.completedAt != null }.forEach { doneIds.add(it.id) }
            if (boardColumns.size > 1 && lastId != null) grouped[lastId].orEmpty().forEach { doneIds.add(it.id) }
            BoardProgress(board.id, board.title, cards.size, doneIds.size)
        }

        return DashboardData(
            notes = plain.size,
            pinned = plain.count { it.pinned },
            inbox = inbox,
            pendingTasks = pending.size,
            overdue = pending.count { (dayOf(it) ?: Long.MAX_VALUE) < todayEpochDay },
            dueToday = pending.count { dayOf(it) == todayEpochDay },
            doneTasks = tasks.size - pending.size,
            boardCards = progress.sumOf { it.total },
            boards = progress,
            week = week,
            todayList = todayList,
            recent = plain.sortedByDescending { it.updatedAt }.take(RECENT_LIMIT),
        )
    }
}
