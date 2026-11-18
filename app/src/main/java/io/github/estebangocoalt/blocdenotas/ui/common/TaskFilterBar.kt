package io.github.estebangocoalt.blocdenotas.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.data.SavedView
import io.github.estebangocoalt.blocdenotas.data.TaskQuery
import io.github.estebangocoalt.blocdenotas.data.TaskSort

@StringRes
private fun TaskSort.labelRes(): Int = when (this) {
    TaskSort.DATE -> R.string.sort_date
    TaskSort.PRIORITY -> R.string.sort_priority
    TaskSort.TITLE -> R.string.sort_title
    TaskSort.CREATED -> R.string.sort_created
}

/**
 * Barra de orden y filtros de las tareas (Tareas, Tablero y Calendario) con sus vistas guardadas.
 * [onSaveView] recibe el nombre; cada pantalla añade lo suyo (tablero, agenda…) al crear la [SavedView].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskFilterBar(
    query: TaskQuery,
    tags: List<String>,
    onChange: (TaskQuery) -> Unit,
    views: List<SavedView>,
    onApplyView: (SavedView) -> Unit,
    onSaveView: (String) -> Unit,
    onDeleteView: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSort: Boolean = true,
) {
    var saveDialog by remember { mutableStateOf(false) }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    Column(modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSort) item(key = "sort") {
                var open by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = query.taskSort != TaskSort.DATE,
                        onClick = { open = true },
                        label = { Text(stringResource(R.string.sort_label, stringResource(query.taskSort.labelRes()))) },
                        shape = CircleShape, border = null, colors = chipColors,
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        TaskSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(stringResource(sort.labelRes())) },
                                onClick = {
                                    open = false
                                    onChange(query.copy(sort = sort.name))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "priority") {
                var open by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = query.minPriority > Priority.NONE,
                        onClick = { open = true },
                        label = {
                            Text(
                                if (query.minPriority > Priority.NONE) {
                                    stringResource(R.string.filter_priority_min, stringResource(priorityLabelRes(query.minPriority)))
                                } else {
                                    stringResource(R.string.filter_priority)
                                },
                            )
                        },
                        shape = CircleShape, border = null, colors = chipColors,
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_any)) },
                            onClick = {
                                open = false
                                onChange(query.copy(minPriority = Priority.NONE))
                            },
                        )
                        listOf(Priority.LOW, Priority.MEDIUM, Priority.HIGH).forEach { p ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_priority_min, stringResource(priorityLabelRes(p)))) },
                                onClick = {
                                    open = false
                                    onChange(query.copy(minPriority = p))
                                },
                            )
                        }
                    }
                }
            }
            if (tags.isNotEmpty() || query.tag.isNotBlank()) item(key = "tag") {
                var open by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = query.tag.isNotBlank(),
                        onClick = { open = true },
                        label = { Text(if (query.tag.isNotBlank()) "#${query.tag}" else stringResource(R.string.filter_tag)) },
                        shape = CircleShape, border = null, colors = chipColors,
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_any)) },
                            onClick = {
                                open = false
                                onChange(query.copy(tag = ""))
                            },
                        )
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text("#$tag") },
                                onClick = {
                                    open = false
                                    onChange(query.copy(tag = tag))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "hideDone") {
                FilterChip(
                    selected = query.hideDone,
                    onClick = { onChange(query.copy(hideDone = !query.hideDone)) },
                    label = { Text(stringResource(R.string.filter_hide_done)) },
                    shape = CircleShape, border = null, colors = chipColors,
                )
            }
        }

        if (views.isNotEmpty() || !query.isDefault) {
            LazyRow(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(views, key = { it.id }) { view ->
                    val selected = view.tasks == query
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            text = view.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(onClick = { onApplyView(view) }, onLongClick = { menu = true })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.view_delete)) },
                                onClick = {
                                    menu = false
                                    onDeleteView(view.id)
                                },
                            )
                        }
                    }
                }
                if (!query.isDefault) item(key = "save") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { saveDialog = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.view_save),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (saveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(stringResource(R.string.view_save_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.view_name_hint)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSaveView(name)
                        saveDialog = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { saveDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
