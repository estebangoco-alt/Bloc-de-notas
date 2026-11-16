package io.github.estebangocoalt.blocdenotas.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Modo compra: marca y desmarca artículos de una lista con un toque; se guarda al instante. */
class ShoppingViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository
    private val noteId: String = handle["noteId"] ?: ""

    val note: StateFlow<NoteWithItems?> =
        repository.notes
            .map { list -> list.firstOrNull { it.note.id == noteId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setChecked(itemId: String, checked: Boolean) =
        update { items -> items.map { if (it.id == itemId) it.copy(checked = checked) else it } }

    fun uncheckAll() = update { items -> items.map { it.copy(checked = false) } }

    fun removeChecked() = update { items ->
        items.filter { !it.checked }.mapIndexed { index, item -> item.copy(position = index) }
    }

    private fun update(change: (List<ChecklistItemEntity>) -> List<ChecklistItemEntity>) {
        val current = note.value ?: return
        val items = change(current.items)
        if (items == current.items) return
        viewModelScope.launch {
            repository.save(current.note.copy(updatedAt = System.currentTimeMillis()), items)
        }
    }
}
