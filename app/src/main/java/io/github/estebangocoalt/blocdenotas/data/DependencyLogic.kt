package io.github.estebangocoalt.blocdenotas.data

/**
 * Tareas que dependen de otras («esta tarea espera a aquella»). Se guardan como una lista de ids
 * separados por comas en [NoteEntity.dependsOn]. Lógica pura (sin Android) para probarla con tests de JVM.
 */
object DependencyLogic {
    fun parse(stored: String): List<String> =
        stored.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun join(ids: List<String>): String = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

    fun add(stored: String, id: String): String = join(parse(stored) + id)

    fun remove(stored: String, id: String): String = join(parse(stored).filter { it != id })

    /** Tareas de las que depende [task] (las que existen en [byId]), hechas o no. */
    fun dependencies(task: NoteEntity, byId: Map<String, NoteEntity>): List<NoteEntity> =
        parse(task.dependsOn).mapNotNull { byId[it] }

    /** Las que todavía bloquean a [task]: dependencias pendientes. Una tarea sin pendientes está libre. */
    fun blockers(task: NoteEntity, byId: Map<String, NoteEntity>): List<NoteEntity> =
        dependencies(task, byId).filter { it.completedAt == null }

    /** Tareas que esperan a [taskId]. */
    fun dependents(taskId: String, all: List<NoteEntity>): List<NoteEntity> =
        all.filter { taskId in parse(it.dependsOn) }

    /** True si hacer que [taskId] dependa de [candidateId] cerraría un círculo (o es ella misma). */
    fun wouldCycle(taskId: String, candidateId: String, byId: Map<String, NoteEntity>): Boolean {
        if (taskId == candidateId) return true
        val seen = HashSet<String>()
        val stack = ArrayDeque<String>().apply { add(candidateId) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (!seen.add(current)) continue
            if (current == taskId) return true
            byId[current]?.let { stack.addAll(parse(it.dependsOn)) }
        }
        return false
    }

    /** Tareas que se pueden añadir como dependencia de [task]: sin repetir y sin formar círculos. */
    fun candidates(task: NoteEntity, all: List<NoteEntity>): List<NoteEntity> {
        val byId = all.associateBy { it.id }
        val already = parse(task.dependsOn).toSet()
        return all
            .filter { it.kind == NoteKind.TASK && it.id !in already && !wouldCycle(task.id, it.id, byId) }
            .sortedBy { it.title.lowercase() }
    }
}
