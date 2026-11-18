package io.github.estebangocoalt.blocdenotas.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.CustomTemplate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.common.labelRes
import io.github.estebangocoalt.blocdenotas.ui.editor.NoteTemplate
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

private fun NoteTemplate.colorIndex(): Int = when (this) {
    NoteTemplate.WEEKLY_SHOPPING -> 1
    NoteTemplate.MEETING -> 0
    NoteTemplate.PROJECT -> 4
    NoteTemplate.DIARY -> 3
}

/** Galería «Nueva desde plantilla»: cuadrícula de 2 columnas con colores pastel. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateSheet(
    onPick: (NoteTemplate) -> Unit,
    onPickCustom: (CustomTemplate) -> Unit,
    onBlank: () -> Unit,
    onDismiss: () -> Unit,
) {
    val store = LocalContext.current.blocApp.customTemplates
    val custom by store.templates.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<CustomTemplate?>(null) }
    toDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.template_delete_title, template.name)) },
            confirmButton = {
                TextButton(onClick = {
                    store.remove(template.id)
                    toDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.template_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            // Cada tarjeta: color, texto (propio o de recursos), qué hacer al pulsar y qué al mantener pulsada.
            class Tile(val color: Int, val label: String, val onClick: () -> Unit, val onLongClick: (() -> Unit)? = null)
            val tiles: List<Tile> =
                NoteTemplate.entries.map { t ->
                    Tile(t.colorIndex(), stringResource(t.label), { onPick(t) })
                } + custom.map { c ->
                    Tile(c.colorIndex.coerceIn(0, 5), c.name, { onPickCustom(c) }, { toDelete = c })
                } + Tile(5, stringResource(R.string.template_blank), onBlank)
            tiles.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { tile ->
                        val swatch = NoteColors.at(tile.color)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(swatch.background)
                                .combinedClickable(onClick = tile.onClick, onLongClick = tile.onLongClick)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = tile.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = swatch.content,
                            )
                        }
                    }
                    if (row.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
            Text(
                text = stringResource(R.string.template_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
