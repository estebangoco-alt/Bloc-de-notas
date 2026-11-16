package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.annotation.StringRes
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteKind

/**
 * Plantillas incluidas en la app. Los textos están en los recursos (castellano e inglés).
 * [title] = null significa «usa la fecha de hoy» (el diario).
 */
enum class NoteTemplate(
    val kind: NoteKind,
    @StringRes val label: Int,
    @StringRes val title: Int?,
    @StringRes val body: Int?,
    @StringRes val items: Int?,
) {
    WEEKLY_SHOPPING(
        NoteKind.SHOPPING, R.string.template_weekly_shopping,
        R.string.template_weekly_shopping, null, R.string.template_weekly_shopping_items,
    ),
    MEETING(
        NoteKind.NOTE, R.string.template_meeting,
        R.string.template_meeting, R.string.template_meeting_body, null,
    ),
    PROJECT(
        NoteKind.NOTE, R.string.template_project,
        R.string.template_project_title, R.string.template_project_body, R.string.template_project_items,
    ),
    DIARY(
        NoteKind.NOTE, R.string.template_diary,
        null, R.string.template_diary_body, null,
    ),
    ;

    companion object {
        fun fromName(name: String?): NoteTemplate? = entries.firstOrNull { it.name == name }
    }
}
