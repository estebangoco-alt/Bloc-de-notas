package io.github.estebangocoalt.blocdenotas.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    val tasks: StateFlow<List<NoteWithItems>> =
        repository.notes
            .map { all -> all.filter { it.note.kind == NoteKind.TASK } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCompleted(id: String, done: Boolean) {
        viewModelScope.launch { repository.setCompleted(id, done) }
    }
}
