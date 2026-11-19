package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** Pantallas que admiten vistas guardadas. */
object ViewScope {
    const val NOTES = "NOTES"
    const val TASKS = "TASKS"
    const val BOARD = "BOARD"
    const val CALENDAR = "CALENDAR"
}

/**
 * Vista guardada con nombre. Según [scope] guarda:
 * - NOTES: [filter] y [query] (búsqueda).
 * - TASKS: [tasks] (orden y filtros).
 * - BOARD: [tasks] y, si se quiere, el tablero [boardId].
 * - CALENDAR: [tasks] y si se ve la [agenda].
 * Las vistas de versiones anteriores no tienen [scope] y cuentan como NOTES.
 */
@Serializable
data class SavedView(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filter: String = "ALL",
    val query: String = "",
    val scope: String = ViewScope.NOTES,
    val tasks: TaskQuery = TaskQuery(),
    val boardId: String = "",
    val agenda: Boolean = false,
    /** CALENDAR: si se ve la línea de tiempo. */
    val timeline: Boolean = false,
)

/** Fusión de listas por identificador. Lógica pura para probarla con tests de JVM. */
object MergeLogic {
    /** Añade a [current] lo de [incoming] cuyo id no exista ya (no pisa lo que hay). */
    fun <T> merge(current: List<T>, incoming: List<T>, id: (T) -> String): List<T> {
        val known = current.map(id).toHashSet()
        return current + incoming.filter { known.add(id(it)) }
    }
}

/** Guarda las vistas en SharedPreferences (texto JSON). También van dentro de la copia de seguridad. */
class SavedViewsStore(context: Context) {
    private val prefs = context.getSharedPreferences("saved_views", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(SavedView.serializer())

    private val _views = MutableStateFlow(load())
    val views: StateFlow<List<SavedView>> = _views

    private fun load(): List<SavedView> =
        runCatching { json.decodeFromString(serializer, prefs.getString(KEY, null) ?: "[]") }
            .getOrDefault(emptyList())

    private fun commit(list: List<SavedView>) {
        _views.value = list
        prefs.edit().putString(KEY, json.encodeToString(serializer, list)).apply()
    }

    fun add(view: SavedView) {
        val clean = view.name.trim()
        if (clean.isEmpty()) return
        commit(_views.value + view.copy(name = clean))
    }

    /** Atajo para las vistas de Notas. */
    fun add(name: String, filter: String, query: String) =
        add(SavedView(name = name, filter = filter, query = query.trim()))

    fun remove(id: String) = commit(_views.value.filter { it.id != id })

    /** Snapshot para la copia de seguridad. */
    fun snapshot(): List<SavedView> = _views.value

    /** Importar: [replace] sustituye todo; si no, añade las que no existan. */
    fun restore(incoming: List<SavedView>, replace: Boolean) =
        commit(if (replace) incoming else MergeLogic.merge(_views.value, incoming) { it.id })

    private companion object {
        const val KEY = "views"
    }
}
