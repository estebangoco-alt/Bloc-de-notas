package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** Plantilla creada por el usuario a partir de una nota. Los textos van tal cual (sin traducir). */
@Serializable
data class CustomTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Nombre de [NoteKind]. */
    val kind: String = NoteKind.NOTE.name,
    val title: String = "",
    val body: String = "",
    val items: List<String> = emptyList(),
    val colorIndex: Int = 0,
) {
    val noteKind: NoteKind get() = NoteKind.entries.firstOrNull { it.name == kind } ?: NoteKind.NOTE
}

/** Identificador con el que viaja una plantilla propia en la ruta del editor. */
object CustomTemplateRef {
    private const val PREFIX = "custom:"
    fun of(id: String) = PREFIX + id
    fun idOf(ref: String?): String? = ref?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
}

/** Plantillas propias en SharedPreferences (JSON); también van dentro de la copia de seguridad. */
class CustomTemplatesStore(context: Context) {
    private val prefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CustomTemplate.serializer())

    private val _templates = MutableStateFlow(load())
    val templates: StateFlow<List<CustomTemplate>> = _templates

    private fun load(): List<CustomTemplate> =
        runCatching { json.decodeFromString(serializer, prefs.getString(KEY, null) ?: "[]") }
            .getOrDefault(emptyList())

    private fun commit(list: List<CustomTemplate>) {
        _templates.value = list
        prefs.edit().putString(KEY, json.encodeToString(serializer, list)).apply()
    }

    fun get(id: String): CustomTemplate? = _templates.value.firstOrNull { it.id == id }

    fun add(template: CustomTemplate) {
        val clean = template.name.trim()
        if (clean.isEmpty()) return
        commit(_templates.value + template.copy(name = clean))
    }

    fun remove(id: String) = commit(_templates.value.filter { it.id != id })

    fun snapshot(): List<CustomTemplate> = _templates.value

    fun restore(incoming: List<CustomTemplate>, replace: Boolean) =
        commit(if (replace) incoming else MergeLogic.merge(_templates.value, incoming) { it.id })

    private companion object {
        const val KEY = "templates"
    }
}
