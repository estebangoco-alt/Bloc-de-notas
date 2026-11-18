package io.github.estebangocoalt.blocdenotas.data

import kotlinx.serialization.Serializable

/** Criterios de orden de las tareas. */
enum class TaskSort {
    /** Por fecha límite (las que no tienen, al final). */
    DATE,

    /** Por prioridad, de alta a baja. */
    PRIORITY,

    /** Por título, de la A a la Z. */
    TITLE,

    /** Las creadas más recientemente primero. */
    CREATED;

    companion object {
        fun fromName(name: String?): TaskSort = entries.firstOrNull { it.name == name } ?: DATE
    }
}

/**
 * Orden y filtros de las listas de tareas (Tareas, Tablero y Calendario).
 * [minPriority] 0 = todas; [tag] vacío = cualquier etiqueta.
 */
@Serializable
data class TaskQuery(
    val sort: String = TaskSort.DATE.name,
    val minPriority: Int = Priority.NONE,
    val tag: String = "",
    val hideDone: Boolean = false,
) {
    val taskSort: TaskSort get() = TaskSort.fromName(sort)

    /** True si no cambia nada respecto a la vista normal. */
    val isDefault: Boolean get() = this == TaskQuery()

    /** True si filtra tareas (el orden solo no cuenta). */
    val filters: Boolean get() = minPriority > Priority.NONE || tag.isNotBlank() || hideDone
}

/** Aplica un [TaskQuery]. Lógica pura (sin Android) para probarla con tests de JVM. */
object TaskQueryLogic {

    fun matches(note: NoteEntity, query: TaskQuery): Boolean {
        if (query.hideDone && note.completedAt != null) return false
        if (note.priority < query.minPriority) return false
        if (query.tag.isNotBlank() &&
            TagLogic.parse(note.tags).none { it.equals(query.tag, ignoreCase = true) }
        ) return false
        return true
    }

    fun filter(notes: List<NoteEntity>, query: TaskQuery): List<NoteEntity> =
        if (query.filters) notes.filter { matches(it, query) } else notes

    /** Comparador de [sort]; los empates se desempatan por fecha de creación. */
    fun comparator(sort: TaskSort): Comparator<NoteEntity> = when (sort) {
        TaskSort.DATE -> compareBy<NoteEntity> { it.dueAt ?: Long.MAX_VALUE }
        TaskSort.PRIORITY -> compareByDescending<NoteEntity> { it.priority }
        TaskSort.TITLE -> compareBy<NoteEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        TaskSort.CREATED -> compareByDescending<NoteEntity> { it.createdAt }
    }.thenBy { it.createdAt }

    fun sort(notes: List<NoteEntity>, sort: TaskSort): List<NoteEntity> =
        notes.sortedWith(comparator(sort))

    /** Filtra y ordena en un solo paso. */
    fun apply(notes: List<NoteEntity>, query: TaskQuery): List<NoteEntity> =
        sort(filter(notes, query), query.taskSort)
}
