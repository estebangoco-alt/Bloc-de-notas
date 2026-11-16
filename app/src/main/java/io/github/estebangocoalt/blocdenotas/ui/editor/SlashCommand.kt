package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.annotation.StringRes
import io.github.estebangocoalt.blocdenotas.R

/** Comandos del editor que se abren al escribir «/» al final del texto. */
enum class SlashCommand(@StringRes val label: Int) {
    TODO(R.string.slash_todo),
    HEADING(R.string.slash_heading),
    BULLETS(R.string.slash_bullets),
    QUOTE(R.string.slash_quote),
    DIVIDER(R.string.slash_divider),
    DATE(R.string.slash_date),
    TIME(R.string.slash_time),
    LINK(R.string.slash_link),
    MENTION(R.string.slash_mention),
    SUBPAGE(R.string.slash_subpage),
}
