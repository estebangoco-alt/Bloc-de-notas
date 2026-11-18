package io.github.estebangocoalt.blocdenotas.ui.tasks

import io.github.estebangocoalt.blocdenotas.ui.common.SettingsButton
import io.github.estebangocoalt.blocdenotas.ui.common.TaskFilterBar
import io.github.estebangocoalt.blocdenotas.ui.common.PropertyChips
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.estebangocoalt.blocdenotas.ui.common.formatMinute
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.data.RepeatRule
import io.github.estebangocoalt.blocdenotas.ui.editor.labelRes
import io.github.estebangocoalt.blocdenotas.data.TaskBucket
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import java.time.LocalDate

@StringRes
private fun TaskBucket.titleRes(): Int = when (this) {
    TaskBucket.OVERDUE -> R.string.tasks_overdue
    TaskBucket.TODAY -> R.string.tasks_today
    TaskBucket.UPCOMING -> R.string.tasks_upcoming
    TaskBucket.LATER -> R.string.tasks_later
    TaskBucket.NO_DATE -> R.string.tasks_no_date
    TaskBucket.DONE -> R.string.tasks_done
}

/** Lista de tareas agrupadas por fecha: vencidas, hoy, próximos días, más adelante, sin fecha y hechas. */
@Composable
fun TasksScreen(
    onOpenTask: (String) -> Unit,
    onNewTask: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TasksViewModel = viewModel(),
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now().toEpochDay() }
    val byId = remember(tasks) { tasks.associateBy { it.note.id } }
    val query by vm.query.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val allViews by vm.views.collectAsStateWithLifecycle()
    val views = remember(allViews) { vm.viewsOf(allViews) }
    val groups = remember(tasks, today, query) { TaskLogic.group(tasks.map { it.note }, today, query.taskSort) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nav_tasks),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SettingsButton(onOpenSettings)
            }
            TaskFilterBar(
                query = query,
                tags = tags,
                onChange = vm::setQuery,
                views = views,
                onApplyView = { vm.setQuery(it.tasks) },
                onSaveView = { name -> vm.saveView(name) },
                onDeleteView = vm::deleteView,
            )
            if (groups.isEmpty()) {
                Text(
                    text = stringResource(if (query.filters) R.string.tasks_no_match else R.string.tasks_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { (bucket, list) ->
                    item(key = "header-${bucket.name}") {
                        Text(
                            text = stringResource(bucket.titleRes(), list.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bucket == TaskBucket.OVERDUE) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp),
                        )
                    }
                    items(list.size, key = { index -> list[index].id }) { index ->
                        val task = byId[list[index].id] ?: return@items
                        TaskRow(
                            task = task,
                            overdue = bucket == TaskBucket.OVERDUE,
                            onToggle = { done -> vm.setCompleted(task.note.id, done) },
                            onOpen = { onOpenTask(task.note.id) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewTask,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new), fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
internal fun TaskRow(
    task: NoteWithItems,
    overdue: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val note = task.note
    val done = note.completedAt != null
    val swatch = NoteColors.at(note.colorIndex)
    val title = note.title.ifBlank {
        note.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
            .ifBlank { stringResource(R.string.board_untitled) }
    }
    val subtasksDone = task.items.count { it.checked }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(swatch.background.copy(alpha = if (done) 0.55f else 1f))
            .clickable(onClick = onOpen)
            .padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = done,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = swatch.accent,
                uncheckedColor = swatch.accent,
                checkmarkColor = Color.White,
            ),
        )
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = swatch.content.copy(alpha = if (done) 0.6f else 1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = buildList {
                note.dueAt?.let { add(formatDueDate(it)) }
                note.reminderMinute?.let { add(formatMinute(it)) }
                RepeatRule.fromName(note.repeatRule).takeIf { it != RepeatRule.NONE }
                    ?.let { add("↻ " + stringResource(it.labelRes())) }
                if (task.items.isNotEmpty()) add("$subtasksDone/${task.items.size}")
            }.joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal,
                    color = if (overdue) MaterialTheme.colorScheme.error else swatch.accent,
                )
            }
            PropertyChips(note.priority, note.tags, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
