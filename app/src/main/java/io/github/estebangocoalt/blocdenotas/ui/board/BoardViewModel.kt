package io.github.estebangocoalt.blocdenotas.ui.board

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.BoardColumnEntity
import io.github.estebangocoalt.blocdenotas.data.BoardEntity
import io.github.estebangocoalt.blocdenotas.data.BoardLogic
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.SavedView
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import io.github.estebangocoalt.blocdenotas.data.TaskQuery
import io.github.estebangocoalt.blocdenotas.data.TaskQueryLogic
import io.github.estebangocoalt.blocdenotas.data.ViewScope
import kotlinx.coroutines.flow.map
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BoardState(
    val boards: List<BoardEntity> = emptyList(),
    /** Tablero que se está viendo (null mientras se crea el primero). */
    val current: BoardEntity? = null,
    val columns: List<BoardColumnEntity> = emptyList(),
    /** Todas las tarjetas de cada columna (orden manual). Es la base de mover y añadir. */
    val cards: Map<String, List<NoteWithItems>> = emptyMap(),
    /** Las tarjetas que se ven con los filtros aplicados, en el mismo orden. */
    val visible: Map<String, List<NoteWithItems>> = emptyMap(),
    /** Etiquetas usadas en las tarjetas, para el filtro. */
    val tags: List<String> = emptyList(),
)

class BoardViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    private val prefs = app.getSharedPreferences("board", Context.MODE_PRIVATE)
    private val selected = MutableStateFlow(prefs.getString(KEY_SELECTED, null))

    private val _query = MutableStateFlow(TaskQuery())
    val query: StateFlow<TaskQuery> = _query

    fun setQuery(value: TaskQuery) {
        _query.value = value
    }

    private val store = app.blocApp.savedViews
    val views: StateFlow<List<SavedView>> =
        store.views.map { list -> list.filter { it.scope == ViewScope.BOARD } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Guarda el filtro actual junto con el tablero que se está viendo. */
    fun saveView(name: String) {
        store.add(
            SavedView(
                name = name, scope = ViewScope.BOARD, tasks = _query.value,
                boardId = state.value.current?.id.orEmpty(),
            ),
        )
    }

    fun applyView(view: SavedView) {
        _query.value = view.tasks
        if (view.boardId.isNotEmpty() && state.value.boards.any { it.id == view.boardId }) select(view.boardId)
    }

    fun deleteView(id: String) = store.remove(id)

    val state: StateFlow<BoardState> =
        combine(repository.boards, repository.columns, repository.notes, selected, _query) { boards, allColumns, notes, sel, query ->
            val current = boards.firstOrNull { it.id == sel } ?: boards.firstOrNull()
            if (current == null) {
                BoardState()
            } else {
                val columns = BoardLogic.columnsOf(boards, allColumns, current.id)
                val tasks = notes.filter { it.note.kind == NoteKind.TASK }
                val mine = BoardLogic.cardsOf(allColumns, columns, current.id == boards.first().id, tasks.map { it.note })
                val byId = tasks.associateBy { it.note.id }
                val grouped = BoardLogic.group(columns, mine)
                val full = grouped.mapValues { (_, list) -> list.mapNotNull { byId[it.id] } }
                // Con filtros se conserva el orden manual; solo se ocultan las tarjetas que no encajan.
                val shown = if (query.filters) {
                    full.mapValues { (_, list) -> list.filter { TaskQueryLogic.matches(it.note, query) } }
                } else {
                    full
                }
                val tags = TagLogic.all(full.values.flatten().map { it.note.tags })
                BoardState(boards, current, columns, full, shown, tags)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardState())

    private fun defaultColumns() = listOf(
        app.getString(R.string.board_col_todo),
        app.getString(R.string.board_col_doing),
        app.getString(R.string.board_col_done),
    )

    init {
        // Primera vez: crea el tablero por defecto en el idioma actual.
        viewModelScope.launch { repository.seedBoard(app.getString(R.string.board_default_name), defaultColumns()) }
    }

    private fun select(id: String?) {
        selected.value = id
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun selectBoard(id: String) = select(id)

    fun addBoard(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { select(repository.addBoard(clean, defaultColumns())) }
    }

    fun renameBoard(board: BoardEntity, title: String) {
        val clean = title.trim()
        if (clean.isEmpty() || clean == board.title) return
        viewModelScope.launch { repository.renameBoard(board, clean) }
    }

    fun deleteBoard(board: BoardEntity) {
        select(null)
        viewModelScope.launch { repository.deleteBoard(board.id) }
    }

    private fun allCards() = state.value.cards.values.flatten().map { it.note }

    fun addCard(columnId: String, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val position = state.value.cards[columnId]?.size ?: 0
        viewModelScope.launch { repository.addCard(columnId, clean, position) }
    }

    fun moveCard(cardId: String, targetColumnId: String) {
        val target = state.value.cards[targetColumnId]?.size ?: return
        val changed = BoardLogic.move(state.value.columns, allCards(), cardId, targetColumnId, target)
        if (changed.isNotEmpty()) viewModelScope.launch { repository.saveCards(changed) }
    }

    /** Suelta una tarjeta arrastrada en la columna [columnId], en la posición [index] (0 = arriba). */
    fun dropCard(cardId: String, columnId: String, index: Int) {
        // [index] cuenta sobre las tarjetas visibles; se traduce a la posición real entre todas.
        val shown = state.value.visible[columnId].orEmpty().filter { it.note.id != cardId }
        val full = state.value.cards[columnId].orEmpty().filter { it.note.id != cardId }
        val real = if (index >= shown.size) full.size else full.indexOfFirst { it.note.id == shown[index].note.id }.coerceAtLeast(0)
        val changed = BoardLogic.move(state.value.columns, allCards(), cardId, columnId, real)
        if (changed.isNotEmpty()) viewModelScope.launch { repository.saveCards(changed) }
    }

    fun shiftCard(cardId: String, delta: Int) {
        val changed = BoardLogic.shift(state.value.columns, allCards(), cardId, delta)
        if (changed.isNotEmpty()) viewModelScope.launch { repository.saveCards(changed) }
    }

    fun addColumn(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val board = state.value.current ?: return
        val position = (state.value.columns.maxOfOrNull { it.position } ?: -1) + 1
        viewModelScope.launch { repository.addColumn(board.id, clean, position) }
    }

    fun renameColumn(column: BoardColumnEntity, title: String) {
        val clean = title.trim()
        if (clean.isEmpty() || clean == column.title) return
        viewModelScope.launch { repository.saveColumns(listOf(column.copy(title = clean))) }
    }

    fun shiftColumn(columnId: String, delta: Int) {
        val changed = BoardLogic.shiftColumn(state.value.columns, columnId, delta)
        if (changed.isNotEmpty()) viewModelScope.launch { repository.saveColumns(changed) }
    }

    fun deleteColumn(columnId: String) {
        // Las tarjetas pasan a otra columna del mismo tablero.
        val fallback = state.value.columns.firstOrNull { it.id != columnId }?.id
        viewModelScope.launch { repository.deleteColumn(columnId, fallback) }
    }

    private companion object {
        const val KEY_SELECTED = "selected"
    }
}
