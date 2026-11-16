package io.github.estebangocoalt.blocdenotas.ui.tasks

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
    modifier: Modifier = Modifier,
    vm: TasksViewModel = viewModel(),
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now().toEpochDay() }
    val byId = remember(tasks) { tasks.associateBy { it.note.id } }
    val groups = remember(tasks, today) { TaskLogic.group(tasks.map { it.note }, today) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                text = stringResource(R.string.nav_tasks),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
            )
            if (groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
            text = { Text(stringResource(R.string.fab_new)) },
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
    val title = note.title.ifBlank {
        note.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
            .ifBlank { stringResource(R.string.board_untitled) }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = done, onCheckedChange = onToggle)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            note.dueAt?.let { due ->
                Text(
                    text = formatDueDate(due),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
