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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteFilter { ALL, NOTES, IDEAS, SHOPPING, TASKS, PINNED }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(NoteFilter.ALL)

    val notes: StateFlow<List<NoteWithItems>> =
        combine(repository.notes, query, filter) { all, q, f ->
            all.filter { matchesFilter(it, f) && matchesQuery(it, q) }
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
            item.items.any { it.text.contains(needle, ignoreCase = true) }
    }
}
