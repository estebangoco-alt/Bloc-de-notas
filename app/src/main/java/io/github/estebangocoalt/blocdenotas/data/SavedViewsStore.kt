package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** Vista guardada: un filtro (todas, notas, ideas, compra, tareas, fijadas) y un texto de búsqueda con nombre. */
@Serializable
data class SavedView(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filter: String,
    val query: String = "",
)

/** Guarda las vistas en SharedPreferences (texto JSON). Son preferencias de la app, no datos del usuario. */
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

    fun add(name: String, filter: String, query: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        commit(_views.value + SavedView(name = clean, filter = filter, query = query.trim()))
    }

    fun remove(id: String) = commit(_views.value.filter { it.id != id })

    private companion object {
        const val KEY = "views"
    }
}
