package io.github.estebangocoalt.blocdenotas.ui.dashboard

import io.github.estebangocoalt.blocdenotas.ui.common.SettingsButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.DashboardData
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Resumen con los datos de las cuatro secciones: notas, tareas, tablero y calendario. */
@Composable
fun DashboardScreen(
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenBoard: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DashboardViewModel = viewModel(),
) {
    val data by vm.data.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dash_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              SettingsButton(onOpenSettings)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        colorIndex = 0,
                        icon = Icons.Default.Create,
                        title = R.string.dash_notes,
                        value = data.notes,
                        subtitle = stringResource(R.string.dash_notes_sub, data.pinned, data.inbox),
                        onClick = onOpenNotes,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        colorIndex = 4,
                        icon = Icons.Default.CheckCircle,
                        title = R.string.dash_tasks,
                        value = data.pendingTasks,
                        subtitle = stringResource(R.string.dash_tasks_sub, data.overdue, data.dueToday),
                        onClick = onOpenTasks,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        colorIndex = 1,
                        icon = Icons.Default.List,
                        title = R.string.dash_board,
                        value = data.boardCards,
                        subtitle = stringResource(R.string.dash_board_sub, data.boards.size, data.boardPercent),
                        onClick = onOpenBoard,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        colorIndex = 3,
                        icon = Icons.Default.DateRange,
                        title = R.string.dash_calendar,
                        value = data.dueToday,
                        subtitle = stringResource(R.string.dash_calendar_sub, data.weekTotal),
                        onClick = onOpenCalendar,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item { SectionTitle(R.string.dash_week) }
        item { WeekChart(data, today, onOpenCalendar) }
        item { SectionTitle(R.string.dash_today) }
        if (data.todayList.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.dash_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(data.todayList, key = { "t" + it.id }) { task ->
                NoteLine(task, showDate = true, overdue = task.dueAt != null && task.dueAt < todayStartUtc(today), onOpenNote)
            }
        }
        if (data.boards.isNotEmpty()) {
            item { SectionTitle(R.string.dash_boards) }
            items(data.boards, key = { "b" + it.id }) { board ->
                Surface(
                    onClick = onOpenBoard,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = board.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.board_checklist_progress, board.done, board.total),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { board.fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        )
                    }
                }
            }
        }
        if (data.recent.isNotEmpty()) {
            item { SectionTitle(R.string.dash_recent) }
            items(data.recent, key = { "n" + it.id }) { note ->
                NoteLine(note, showDate = false, overdue = false, onOpenNote)
            }
        }
    }
}

/** Medianoche UTC del día [day]: así se guardan las fechas límite (ver TaskLogic). */
private fun todayStartUtc(day: LocalDate): Long = day.toEpochDay() * 86_400_000L

@Composable
private fun SectionTitle(res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatCard(
    colorIndex: Int,
    icon: ImageVector,
    title: Int,
    value: Int,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = NoteColors.at(colorIndex)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = swatch.background,
        contentColor = swatch.content,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(swatch.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = swatch.background, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

/** Barras con las tareas pendientes de hoy y los 6 días siguientes. */
@Composable
private fun WeekChart(data: DashboardData, today: LocalDate, onClick: () -> Unit) {
    val max = (data.week.maxOrNull() ?: 0).coerceAtLeast(1)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            data.week.forEachIndexed { offset, count ->
                val day = today.plusDays(offset.toLong())
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = count.toString(), style = MaterialTheme.typography.labelMedium)
                    Box(
                        Modifier
                            .padding(vertical = 4.dp)
                            .height((6 + 58 * count / max).dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (offset == 0) 1f else 0.35f),
                            ),
                    )
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteLine(note: NoteEntity, showDate: Boolean, overdue: Boolean, onOpen: (String) -> Unit) {
    val swatch = NoteColors.at(note.colorIndex)
    Surface(
        onClick = { onOpen(note.id) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(swatch.accent))
            Text(
                text = note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() } ?: stringResource(R.string.board_untitled) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showDate && note.dueAt != null) {
                Text(
                    text = if (overdue) stringResource(R.string.widget_overdue) else formatDueDate(note.dueAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
