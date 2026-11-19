package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyLogicTest {
    private fun task(id: String, deps: String = "", done: Boolean = false, title: String = id) = NoteEntity(
        id = id, title = title, kind = NoteKind.TASK, dependsOn = deps,
        completedAt = if (done) 1L else null, createdAt = 0, updatedAt = 0,
    )

    @Test fun parseAndJoinCleanUp() {
        assertEquals(listOf("a", "b"), DependencyLogic.parse(" a, b ,,a"))
        assertEquals("a,b", DependencyLogic.join(listOf("a", " b", "a", "")))
        assertEquals("a,b", DependencyLogic.add("a", "b"))
        assertEquals("a", DependencyLogic.remove("a,b", "b"))
    }

    @Test fun blockersAreThePendingDependencies() {
        val a = task("a", done = true)
        val b = task("b")
        val c = task("c", deps = "a,b,zz")
        val byId = listOf(a, b, c).associateBy { it.id }
        assertEquals(listOf("b"), DependencyLogic.blockers(c, byId).map { it.id })
        assertEquals(listOf("a", "b"), DependencyLogic.dependencies(c, byId).map { it.id })
    }

    @Test fun deletedDependencyNoLongerBlocks() {
        val c = task("c", deps = "gone")
        assertTrue(DependencyLogic.blockers(c, mapOf("c" to c)).isEmpty())
    }

    @Test fun detectsDirectAndIndirectCycles() {
        val a = task("a", deps = "b")
        val b = task("b", deps = "c")
        val c = task("c")
        val byId = listOf(a, b, c).associateBy { it.id }
        assertTrue(DependencyLogic.wouldCycle("a", "a", byId))
        assertTrue(DependencyLogic.wouldCycle("c", "a", byId)) // c -> a -> b -> c
        assertFalse(DependencyLogic.wouldCycle("a", "c", byId)) // a ya depende de c por b: no es un círculo
    }

    @Test fun cycleCheckSurvivesExistingLoops() {
        val a = task("a", deps = "b")
        val b = task("b", deps = "a")
        val byId = listOf(a, b).associateBy { it.id }
        assertFalse(DependencyLogic.wouldCycle("x", "a", byId))
    }

    @Test fun candidatesExcludeSelfExistingAndCycles() {
        val a = task("a", deps = "b", title = "A")
        val b = task("b", title = "B")
        val c = task("c", deps = "a", title = "C")
        val note = NoteEntity(id = "n", title = "nota", createdAt = 0, updatedAt = 0)
        val all = listOf(a, b, c, note)
        // Una tarea libre puede depender de cualquier tarea (la nota no cuenta).
        assertEquals(listOf("a", "b", "c"), DependencyLogic.candidates(task("x"), all).map { it.id })
        // Para «a» (que ya depende de b): no se ofrece b (ya está), ni a, ni c (c depende de a → círculo), ni la nota.
        assertEquals(emptyList<String>(), DependencyLogic.candidates(a, all).map { it.id })
        // Para «b»: se ofrece a? a depende de b → círculo; c → depende de a que depende de b → círculo.
        assertEquals(emptyList<String>(), DependencyLogic.candidates(b, all).map { it.id })
    }

    @Test fun dependentsAreTheTasksWaiting() {
        val a = task("a")
        val b = task("b", deps = "a")
        val c = task("c", deps = "a,b")
        assertEquals(listOf("b", "c"), DependencyLogic.dependents("a", listOf(a, b, c)).map { it.id })
    }
}
