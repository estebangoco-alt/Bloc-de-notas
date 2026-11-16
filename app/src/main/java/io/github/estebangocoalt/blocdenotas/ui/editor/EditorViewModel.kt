package io.github.estebangocoalt.blocdenotas.ui.editor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import kotlinx.coroutines.launch
import java.util.UUID

/** Elemento de lista editable en pantalla. */
class EditItem(val id: String, text: String, checked: Boolean) {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(checked)
}

/**
 * Estado del editor. Se guarda automáticamente al salir (sin botón "guardar"),
 * como en las apps de notas rápidas. Una nota vacía no se guarda.
 */
class EditorViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository
    private val existingId: String? = handle["noteId"]

    private val id: String = existingId ?: UUID.randomUUID().toString()
    private var createdAt: Long = System.currentTimeMillis()
    private var inboxFlag = false
    private var completedAtStored: Long? = null
    private var loaded = false
    private var discarded = false
    private var lastSavedKey: String? = null

    var title by mutableStateOf("")
    var body by mutableStateOf("")
    var kind by mutableStateOf(NoteKind.fromName(handle["kind"]))
        private set
    var colorIndex by mutableStateOf(kind.defaultColor)
    var pinned by mutableStateOf(false)
    var dueAt by mutableStateOf<Long?>(null)
    var completed by mutableStateOf(false)
    var reminderMinute by mutableStateOf<Int?>(null)
    val items = mutableStateListOf<EditItem>()

    init {
        viewModelScope.launch {
            val existing = existingId?.let { repository.get(it) }
            if (existing != null) {
                val note = existing.note
                title = note.title
                body = note.body
                kind = note.kind
                colorIndex = note.colorIndex
                pinned = note.pinned
                createdAt = note.createdAt
                inboxFlag = note.inbox
                dueAt = note.dueAt
                completedAtStored = note.completedAt
                completed = note.completedAt != null
                reminderMinute = note.reminderMinute
                existing.items.forEach { items += EditItem(it.id, it.text, it.checked) }
                lastSavedKey = stateKey()
            } else if (kind == NoteKind.TASK) {
                // Nueva tarea creada desde el calendario: ya viene con su fecha.
                handle.get<String>("due")?.toLongOrNull()?.let { dueAt = TaskLogic.normalize(it) }
            } else if (kind == NoteKind.SHOPPING) {
                items += EditItem(UUID.randomUUID().toString(), "", false)
            }
            loaded = true
        }
    }

    fun changeKind(newKind: NoteKind) {
        if (colorIndex == kind.defaultColor) colorIndex = newKind.defaultColor
        kind = newKind
    }

    fun addItem() {
        items += EditItem(UUID.randomUUID().toString(), "", false)
    }

    fun removeItem(item: EditItem) {
        items.remove(item)
    }

    /** Elimina la nota y evita que el guardado automático la recree al salir. */
    fun discard() {
        discarded = true
        repository.appScope.launch { repository.delete(id) }
    }

    /** Se llama al salir de la pantalla. Guarda solo si algo cambió. */
    fun saveOnExit() {
        if (!loaded || discarded) return
        val key = stateKey()
        if (key == lastSavedKey) return
        lastSavedKey = key

        val cleanItems = items.filter { it.text.isNotBlank() }
        val blank = title.isBlank() && body.isBlank() && cleanItems.isEmpty()
        val note = NoteEntity(
            id = id,
            title = title.trim(),
            body = body.trim(),
            kind = kind,
            colorIndex = colorIndex,
            pinned = pinned,
            inbox = inboxFlag,
            dueAt = dueAt,
            reminderMinute = if (dueAt != null) reminderMinute else null,
            completedAt = if (completed) (completedAtStored ?: System.currentTimeMillis()) else null,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        val entities = cleanItems.mapIndexed { index, item ->
            ChecklistItemEntity(id = item.id, noteId = id, text = item.text.trim(), checked = item.checked, position = index)
        }
        repository.appScope.launch {
            if (blank) repository.delete(id) else repository.save(note, entities)
        }
    }

    private fun stateKey(): String =
        listOf(title, body, kind, colorIndex, pinned, dueAt, completed, reminderMinute, items.map { Triple(it.id, it.text, it.checked) }).toString()
}
