package io.github.estebangocoalt.blocdenotas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteFilter { ALL, NOTES, IDEAS, SHOPPING, TASKS, PINNED }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(NoteFilter.ALL)

    val notes: StateFlow<List<NoteWithItems>> =
        combine(repository.notes, query, filter) { all, q, f ->
            val ids = all.map { it.note.id }.toSet()
            // Las subpáginas se ven dentro de su página; en la búsqueda aparecen todas.
            all.filter {
                matchesFilter(it, f) && matchesQuery(it, q) &&
                    (q.isNotBlank() || it.note.parentId == null || it.note.parentId !in ids)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inbox: StateFlow<List<NoteEntity>> =
        repository.inbox.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inboxCount: StateFlow<Int> =
        repository.inboxCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: NoteFilter) {
        filter.value = value
    }

    private val store = app.blocApp.savedViews
    val savedViews: StateFlow<List<io.github.estebangocoalt.blocdenotas.data.SavedView>> =
        store.views
            .map { list -> list.filter { it.scope == io.github.estebangocoalt.blocdenotas.data.ViewScope.NOTES } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Aplica una vista guardada (su filtro y su búsqueda). */
    fun applyView(view: io.github.estebangocoalt.blocdenotas.data.SavedView) {
        filter.value = NoteFilter.entries.firstOrNull { it.name == view.filter } ?: NoteFilter.ALL
        query.value = view.query
    }

    fun saveCurrentView(name: String) = store.add(name, filter.value.name, query.value)

    fun deleteView(id: String) = store.remove(id)

    fun classify(id: String, kind: NoteKind) {
        viewModelScope.launch { repository.classify(id, kind) }
    }

    fun deleteInboxNote(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    private fun matchesFilter(item: NoteWithItems, filter: NoteFilter): Boolean = when (filter) {
        NoteFilter.ALL -> true
        NoteFilter.NOTES -> item.note.kind == NoteKind.NOTE
        NoteFilter.IDEAS -> item.note.kind == NoteKind.IDEA
        NoteFilter.SHOPPING -> item.note.kind == NoteKind.SHOPPING
        NoteFilter.TASKS -> item.note.kind == NoteKind.TASK
        NoteFilter.PINNED -> item.note.pinned
    }

    private fun matchesQuery(item: NoteWithItems, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return item.note.title.contains(needle, ignoreCase = true) ||
            item.note.body.contains(needle, ignoreCase = true) ||
            item.note.tags.contains(needle.trimStart('#'), ignoreCase = true) ||
            item.items.any { it.text.contains(needle, ignoreCase = true) }
    }
}
