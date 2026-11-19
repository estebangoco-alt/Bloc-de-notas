package io.github.estebangocoalt.blocdenotas.ui.editor

import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.LinkLogic
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import io.github.estebangocoalt.blocdenotas.data.RepeatLogic
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import io.github.estebangocoalt.blocdenotas.data.TemplateText
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
import io.github.estebangocoalt.blocdenotas.data.AttachmentEntity
import io.github.estebangocoalt.blocdenotas.data.AttachmentKind
import io.github.estebangocoalt.blocdenotas.data.CustomTemplate
import io.github.estebangocoalt.blocdenotas.data.CustomTemplateRef
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import kotlinx.coroutines.launch
import java.util.UUID

/** Elemento de lista editable en pantalla. */
class EditItem(val id: String, text: String, checked: Boolean, aisle: String = "") {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(checked)
    /** Pasillo elegido a mano (id de Aisle). Vacío = se deduce del nombre. */
    var aisle by mutableStateOf(aisle)
}

/**
 * Estado del editor. Se guarda automáticamente al salir (sin botón "guardar"),
 * como en las apps de notas rápidas. Una nota vacía no se guarda.
 */
class EditorViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository
    private val existingId: String? = handle["noteId"]
    private val template: NoteTemplate? = NoteTemplate.fromName(handle["template"])
    private val customTemplate: CustomTemplate? =
        CustomTemplateRef.idOf(handle["template"])?.let { app.blocApp.customTemplates.get(it) }

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
    var repeatRule by mutableStateOf(RepeatRule.NONE)
    var priority by mutableStateOf(Priority.NONE)
    var tags by mutableStateOf("")
    /** Ids de las tareas de las que depende esta (ver DependencyLogic). */
    var dependsOn by mutableStateOf("")
    var parentId by mutableStateOf<String?>(null)

    /** Notas visibles (no borradas), para enlaces, referencias y subpáginas. */
    val allNotes: StateFlow<List<NoteEntity>> =
        repository.notes
            .map { list -> list.map { it.note } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Etiquetas que ya existen en otras notas, para sugerirlas al añadir. */
    val knownTags: StateFlow<List<String>> =
        repository.notes
            .map { list -> TagLogic.all(list.map { it.note.tags }) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items = mutableStateListOf<EditItem>()

    /** Imágenes y notas de voz de la nota (los archivos ya están en la app; la fila se guarda con la nota). */
    val attachments = mutableStateListOf<AttachmentEntity>()
    private val attachmentStore = app.blocApp.attachmentStore

    /** Copia la imagen elegida a la app y la adjunta. [onFail] se llama si no se pudo leer. */
    fun addImage(uri: android.net.Uri, onFail: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val name = attachmentStore.saveImage(uri)
            if (name == null) {
                launch(kotlinx.coroutines.Dispatchers.Main) { onFail() }
            } else {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    attachments += AttachmentEntity(
                        noteId = id, kind = AttachmentKind.IMAGE.name, fileName = name,
                        createdAt = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    fun addAudio(fileName: String, durationMs: Long) {
        attachments += AttachmentEntity(
            noteId = id, kind = AttachmentKind.AUDIO.name, fileName = fileName,
            durationMs = durationMs, createdAt = System.currentTimeMillis(),
        )
    }

    fun removeAttachment(attachment: AttachmentEntity) {
        attachments.remove(attachment)
        attachmentStore.delete(attachment.fileName)
    }

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
                repeatRule = RepeatRule.fromName(note.repeatRule)
                priority = note.priority
                tags = note.tags
                dependsOn = note.dependsOn
                parentId = note.parentId
                existing.items.forEach { items += EditItem(it.id, it.text, it.checked, it.aisle) }
                attachments += repository.attachmentsOf(id)
                lastSavedKey = stateKey()
            } else if (template != null) {
                parentId = handle["parent"]
                applyTemplate(template)
                lastSavedKey = stateKey() // una plantilla sin tocar no se guarda al salir
            } else if (customTemplate != null) {
                parentId = handle["parent"]
                applyCustomTemplate(customTemplate)
                lastSavedKey = stateKey()
            } else if (kind == NoteKind.TASK) {
                parentId = handle["parent"]
                // Nueva tarea creada desde el calendario: ya viene con su fecha.
                handle.get<String>("due")?.toLongOrNull()?.let { dueAt = TaskLogic.normalize(it) }
            } else {
                parentId = handle["parent"]
                if (kind == NoteKind.SHOPPING) items += EditItem(UUID.randomUUID().toString(), "", false)
            }
            loaded = true
        }
    }

    /** Abre la nota enlazada por [title]; si no existe, la crea. [onReady] recibe su id. */
    fun openLinked(title: String, onReady: (String) -> Unit) {
        val existing = LinkLogic.find(title, allNotes.value, except = id)
        saveOnExit()
        if (existing != null) {
            onReady(existing.id)
            return
        }
        val now = System.currentTimeMillis()
        val note = NoteEntity(title = title.trim(), createdAt = now, updatedAt = now)
        viewModelScope.launch {
            repository.save(note, emptyList())
            onReady(note.id)
        }
    }

    /** Antes de crear una subpágina, la nota madre tiene que existir (aunque esté en blanco). */
    fun prepareForSubpage() {
        if (title.isBlank() && body.isBlank() && items.none { it.text.isNotBlank() }) {
            title = getApplication<Application>().getString(R.string.board_untitled)
        }
        saveOnExit()
    }

    /** Id de la nota abierta (para abrir el modo compra). */
    val currentId: String get() = id

    private fun applyTemplate(t: NoteTemplate) {
        val ctx = getApplication<Application>()
        kind = t.kind
        colorIndex = t.kind.defaultColor
        title = t.title?.let { ctx.getString(it) }
            ?: LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        body = t.body?.let { ctx.getString(it) }.orEmpty()
        t.items?.let { res ->
            TemplateText.lines(ctx.getString(res)).forEach { items += EditItem(UUID.randomUUID().toString(), it, false) }
        }
    }

    private fun applyCustomTemplate(t: CustomTemplate) {
        kind = t.noteKind
        colorIndex = t.colorIndex.coerceIn(0, 5)
        title = t.title
        body = t.body
        t.items.forEach { items += EditItem(UUID.randomUUID().toString(), it, false) }
    }

    /** Guarda lo escrito como plantilla propia (sin marcas de hecho ni fechas). */
    fun saveAsTemplate(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        getApplication<Application>().blocApp.customTemplates.add(
            CustomTemplate(
                name = clean,
                kind = kind.name,
                title = title.trim(),
                body = body.trim(),
                items = items.map { it.text.trim() }.filter { it.isNotEmpty() },
                colorIndex = colorIndex,
            ),
        )
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
        // Marcar como hecha una tarea que se repite la reprograma en vez de cerrarla.
        if (kind == NoteKind.TASK && completed && completedAtStored == null && dueAt != null &&
            repeatRule != RepeatRule.NONE
        ) {
            val base = NoteEntity(
                kind = kind, dueAt = dueAt, repeatRule = repeatRule.name, createdAt = 0, updatedAt = 0,
            )
            RepeatLogic.advance(base, java.time.LocalDate.now().toEpochDay(), 0)?.let { next ->
                dueAt = next.dueAt
                completed = false
                items.forEach { it.checked = false }
            }
        }
        val key = stateKey()
        if (key == lastSavedKey) return
        lastSavedKey = key

        val cleanItems = items.filter { it.text.isNotBlank() }
        val blank = title.isBlank() && body.isBlank() && cleanItems.isEmpty() && attachments.isEmpty()
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
            repeatRule = if (dueAt != null && repeatRule != RepeatRule.NONE) repeatRule.name else null,
            priority = if (kind == NoteKind.TASK) priority else Priority.NONE,
            tags = tags,
            dependsOn = if (kind == NoteKind.TASK) dependsOn else "",
            parentId = parentId,
            completedAt = if (completed) (completedAtStored ?: System.currentTimeMillis()) else null,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        val entities = cleanItems.mapIndexed { index, item ->
            ChecklistItemEntity(
                id = item.id, noteId = id, text = item.text.trim(), checked = item.checked,
                position = index, aisle = if (kind == NoteKind.SHOPPING) item.aisle else "",
            )
        }
        val attachmentRows = attachments.mapIndexed { index, a -> a.copy(noteId = id, position = index) }
        repository.appScope.launch {
            if (blank) {
                repository.deletePermanently(id)
            } else {
                repository.save(note, entities, attachmentRows)
            }
        }
    }

    private fun stateKey(): String =
        listOf(title, body, kind, colorIndex, pinned, dueAt, completed, reminderMinute, repeatRule, priority, tags, dependsOn, parentId, items.map { listOf(it.id, it.text, it.checked, it.aisle) }, attachments.map { it.id }).toString()
}
