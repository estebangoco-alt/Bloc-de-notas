package io.github.estebangocoalt.blocdenotas.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewsAndTemplatesTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test fun oldSavedViewWithoutScopeIsANotesView() {
        val v = json.decodeFromString(SavedView.serializer(), """{"id":"1","name":"Compra","filter":"SHOPPING","query":"leche"}""")
        assertEquals(ViewScope.NOTES, v.scope)
        assertEquals(TaskQuery(), v.tasks)
    }

    @Test fun savedViewRoundTripsTaskQuery() {
        val v = SavedView(id = "2", name = "Urgentes", scope = ViewScope.TASKS, tasks = TaskQuery(sort = "PRIORITY", minPriority = 3, tag = "casa", hideDone = true))
        val back = json.decodeFromString(SavedView.serializer(), json.encodeToString(SavedView.serializer(), v))
        assertEquals(v, back)
    }

    @Test fun mergeKeepsExistingAndAddsNew() {
        val a = listOf(SavedView(id = "1", name = "A"), SavedView(id = "2", name = "B"))
        val b = listOf(SavedView(id = "2", name = "OTRA"), SavedView(id = "3", name = "C"))
        val merged = MergeLogic.merge(a, b) { it.id }
        assertEquals(listOf("A", "B", "C"), merged.map { it.name })
    }

    @Test fun mergeDropsDuplicatesInsideIncoming() {
        val merged = MergeLogic.merge(emptyList(), listOf("x", "x", "y")) { it }
        assertEquals(listOf("x", "y"), merged)
    }

    @Test fun customTemplateRef() {
        assertEquals("abc", CustomTemplateRef.idOf(CustomTemplateRef.of("abc")))
        assertNull(CustomTemplateRef.idOf("WEEKLY_SHOPPING"))
        assertNull(CustomTemplateRef.idOf(null))
    }

    @Test fun unknownKindFallsBackToNote() {
        assertEquals(NoteKind.NOTE, CustomTemplate(name = "x", kind = "???").noteKind)
        assertEquals(NoteKind.SHOPPING, CustomTemplate(name = "x", kind = "SHOPPING").noteKind)
    }
}
