package io.github.estebangocoalt.blocdenotas.ui.board

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.BoardColumnEntity
import io.github.estebangocoalt.blocdenotas.data.BoardLogic
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BoardState(
    val columns: List<BoardColumnEntity> = emptyList(),
    val cards: Map<String, List<NoteWithItems>> = emptyMap(),
)

class BoardViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    val state: StateFlow<BoardState> =
        combine(repository.columns, repository.notes) { columns, notes ->
            val tasks = notes.filter { it.note.kind == NoteKind.TASK }
            val byId = tasks.associateBy { it.note.id }
            val grouped = BoardLogic.group(columns, tasks.map { it.note })
            BoardState(columns, grouped.mapValues { (_, list) -> list.mapNotNull { byId[it.id] } })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardState())

    init {
        // Primera vez: crea las columnas por defecto en el idioma actual.
        viewModelScope.launch {
            repository.seedColumns(
                listOf(
                    app.getString(R.string.board_col_todo),
                    app.getString(R.string.board_col_doing),
                    app.getString(R.string.board_col_done),
                ),
            )
        }
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

    fun shiftCard(cardId: String, delta: Int) {
        val changed = BoardLogic.shift(state.value.columns, allCards(), cardId, delta)
        if (changed.isNotEmpty()) viewModelScope.launch { repository.saveCards(changed) }
    }

    fun addColumn(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val position = (state.value.columns.maxOfOrNull { it.position } ?: -1) + 1
        viewModelScope.launch { repository.addColumn(clean, position) }
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
        viewModelScope.launch { repository.deleteColumn(columnId) }
    }
}
