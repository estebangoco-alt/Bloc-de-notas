package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.DependencyLogic
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch

/** «Depende de»: tareas que hay que terminar antes que esta, y las tareas que esperan a esta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DependencyEditor(
    currentId: String,
    dependsOn: String,
    all: List<NoteEntity>,
    swatch: NoteSwatch,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    val byId = remember(all) { all.associateBy { it.id } }
    val ids = DependencyLogic.parse(dependsOn).filter { it in byId }
    val waiting = remember(all, currentId) { DependencyLogic.dependents(currentId, all.filter { it.kind == NoteKind.TASK }) }
    // Para calcular candidatas se necesita la tarea actual con sus dependencias de ahora.
    val current = NoteEntity(id = currentId, kind = NoteKind.TASK, dependsOn = dependsOn, createdAt = 0, updatedAt = 0)
    val candidates = remember(all, dependsOn, currentId) {
        DependencyLogic.candidates(current, all.filter { it.id != currentId })
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.dep_title),
            style = MaterialTheme.typography.labelLarge,
            color = swatch.content.copy(alpha = 0.75f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ids.forEach { id ->
                val dep = byId.getValue(id)
                InputChip(
                    selected = false,
                    onClick = { onChange(DependencyLogic.remove(dependsOn, id)) },
                    label = {
                        Text(
                            text = (if (dep.completedAt != null) "✓ " else "") + dep.title.ifBlank { stringResource(R.string.board_untitled) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = { Text("✕") },
                )
            }
            if (candidates.isNotEmpty()) {
                AssistChip(
                    onClick = { picking = true },
                    label = { Text(stringResource(R.string.dep_add)) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                )
            }
        }
        if (waiting.isNotEmpty()) {
            Text(
                text = stringResource(R.string.dep_blocks, waiting.joinToString(", ") { it.title.ifBlank { "…" } }),
                style = MaterialTheme.typography.bodySmall,
                color = swatch.content.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.dep_pick_title)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(candidates, key = { it.id }) { task ->
                        Text(
                            text = (if (task.completedAt != null) "✓ " else "") + task.title.ifBlank { stringResource(R.string.board_untitled) },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChange(DependencyLogic.add(dependsOn, task.id))
                                    picking = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
