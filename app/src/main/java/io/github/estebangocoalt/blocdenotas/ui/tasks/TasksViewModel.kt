package io.github.estebangocoalt.blocdenotas.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.data.SavedView
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import io.github.estebangocoalt.blocdenotas.data.TaskQuery
import io.github.estebangocoalt.blocdenotas.data.TaskQueryLogic
import io.github.estebangocoalt.blocdenotas.data.ViewScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    private val allTasks: StateFlow<List<NoteWithItems>> =
        repository.notes
            .map { all -> all.filter { it.note.kind == NoteKind.TASK } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow(TaskQuery())
    val query: StateFlow<TaskQuery> = _query

    fun setQuery(value: TaskQuery) {
        _query.value = value
    }

    /** Tareas con el orden y los filtros aplicados. */
    val tasks: StateFlow<List<NoteWithItems>> =
        combine(allTasks, _query) { list, query ->
            val byId = list.associateBy { it.note.id }
            TaskQueryLogic.apply(list.map { it.note }, query).mapNotNull { byId[it.id] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Etiquetas usadas en las tareas, para el filtro. */
    val tags: StateFlow<List<String>> =
        allTasks.map { list -> TagLogic.all(list.map { it.note.tags }) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val store = app.blocApp.savedViews

    /** Vistas guardadas de esta pantalla ([scope] lo fija quien crea el ViewModel: Tareas o Calendario). */
    var scope: String = ViewScope.TASKS
    val views: StateFlow<List<SavedView>> =
        store.views.map { list -> list }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun viewsOf(all: List<SavedView>): List<SavedView> = all.filter { it.scope == scope }

    fun saveView(name: String, agenda: Boolean = false) {
        store.add(SavedView(name = name, scope = scope, tasks = _query.value, agenda = agenda))
    }

    fun deleteView(id: String) = store.remove(id)

    fun setCompleted(id: String, done: Boolean) {
        viewModelScope.launch { repository.setCompleted(id, done) }
    }
}
