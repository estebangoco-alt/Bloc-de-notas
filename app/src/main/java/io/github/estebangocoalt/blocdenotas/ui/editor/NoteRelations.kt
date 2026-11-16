package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.LinkLogic
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch

private fun NoteEntity.shownTitle(fallback: String) = title.ifBlank { fallback }

/** «Dentro de: Página madre», para volver a la nota que contiene esta subpágina. */
@Composable
fun ParentChip(parent: NoteEntity, swatch: NoteSwatch, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp)) {
        AssistChip(
            onClick = { onOpen(parent.id) },
            label = { Text(stringResource(R.string.parent_up, parent.shownTitle(stringResource(R.string.board_untitled)))) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
        )
    }
}

/**
 * Enlaces `[[Título]]` de esta nota, notas que la mencionan y subpáginas.
 * Las que aún no existen se muestran con «+» y se crean al tocarlas.
 */
@Composable
fun NoteRelations(
    body: String,
    currentId: String,
    currentTitle: String,
    all: List<NoteEntity>,
    swatch: NoteSwatch,
    onOpen: (String) -> Unit,
    onCreateLinked: (String) -> Unit,
    onNewSubpage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val untitled = stringResource(R.string.board_untitled)
    val links = LinkLogic.titles(body)
    val backlinks = LinkLogic.backlinks(
        NoteEntity(id = currentId, title = currentTitle, createdAt = 0, updatedAt = 0),
        all,
    )
    val children = all.filter { it.parentId == currentId }

    Column(modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (links.isNotEmpty()) {
            RelationLabel(R.string.links_title, swatch)
            ChipRow {
                links.forEach { title ->
                    val target = LinkLogic.find(title, all, except = currentId)
                    AssistChip(
                        onClick = { if (target != null) onOpen(target.id) else onCreateLinked(title) },
                        label = { Text(title) },
                        leadingIcon = if (target == null) {
                            { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.links_create)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (backlinks.isNotEmpty()) {
            RelationLabel(R.string.backlinks_title, swatch)
            ChipRow {
                backlinks.forEach { note ->
                    AssistChip(onClick = { onOpen(note.id) }, label = { Text(note.shownTitle(untitled)) })
                }
            }
        }
        RelationLabel(R.string.subpages_title, swatch)
        ChipRow {
            children.forEach { note ->
                AssistChip(onClick = { onOpen(note.id) }, label = { Text(note.shownTitle(untitled)) })
            }
            AssistChip(
                onClick = onNewSubpage,
                label = { Text(stringResource(R.string.subpage_add)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun RelationLabel(res: Int, swatch: NoteSwatch) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = swatch.accent,
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
