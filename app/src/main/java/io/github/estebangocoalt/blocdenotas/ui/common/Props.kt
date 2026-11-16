package io.github.estebangocoalt.blocdenotas.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.TagLogic
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

@StringRes
fun priorityLabelRes(priority: Int): Int = when (priority) {
    Priority.HIGH -> R.string.priority_high
    Priority.MEDIUM -> R.string.priority_medium
    Priority.LOW -> R.string.priority_low
    else -> R.string.priority_none
}

/** Alta = melocotón, media = amarillo, baja = menta (los colores de los mockups). */
private fun priorityColorIndex(priority: Int): Int = when (priority) {
    Priority.HIGH -> 2
    Priority.MEDIUM -> 0
    else -> 1
}

@Composable
fun PriorityChip(priority: Int, modifier: Modifier = Modifier) {
    if (priority == Priority.NONE) return
    val swatch = NoteColors.at(priorityColorIndex(priority))
    Text(
        text = stringResource(priorityLabelRes(priority)),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = swatch.content,
        modifier = modifier
            .clip(CircleShape)
            .background(swatch.background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun TagChip(tag: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val swatch = NoteColors.at(3)
    Text(
        text = "#$tag",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = swatch.content,
        modifier = modifier
            .clip(CircleShape)
            .background(swatch.background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Prioridad y etiquetas de una nota, en una fila que salta de línea si no caben. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyChips(
    priority: Int,
    tags: String,
    modifier: Modifier = Modifier,
    onTagClick: ((String) -> Unit)? = null,
) {
    val list = TagLogic.parse(tags)
    if (priority == Priority.NONE && list.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PriorityChip(priority)
        list.forEach { tag -> TagChip(tag, onClick = onTagClick?.let { click -> { click(tag) } }) }
    }
}
