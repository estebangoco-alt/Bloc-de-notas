package io.github.estebangocoalt.blocdenotas.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.CalendarLogic
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import io.github.estebangocoalt.blocdenotas.ui.tasks.TaskRow
import io.github.estebangocoalt.blocdenotas.ui.tasks.TasksViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Calendario mensual: los días con tareas llevan un punto; al elegir un día se ven sus tareas. */
@Composable
fun CalendarScreen(
    onOpenTask: (String) -> Unit,
    onNewTask: (dueAt: Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: TasksViewModel = viewModel(),
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    // Se guardan como días desde 1970 para que sobrevivan a rotar la pantalla.
    var monthStart by rememberSaveable { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var selected by rememberSaveable { mutableStateOf(today.toEpochDay()) }

    val locale = Locale.getDefault()
    val month = YearMonth.from(LocalDate.ofEpochDay(monthStart))
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val grid = remember(month, firstDayOfWeek) { CalendarLogic.monthGrid(month, firstDayOfWeek) }
    val byDay = remember(tasks) { CalendarLogic.tasksByDay(tasks.map { it.note }) }
    val byId = remember(tasks) { tasks.associateBy { it.note.id } }
    val todayDay = today.toEpochDay()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthStart = month.minusMonths(1).atDay(1).toEpochDay() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_prev_month))
                }
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                        .replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { monthStart = month.plusMonths(1).atDay(1).toEpochDay() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_next_month))
                }
                TextButton(onClick = {
                    monthStart = today.withDayOfMonth(1).toEpochDay()
                    selected = todayDay
                }) { Text(stringResource(R.string.action_today)) }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                CalendarLogic.weekDays(firstDayOfWeek).forEach { day ->
                    Text(
                        text = day.getDisplayName(DateTextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 8.dp)) {
                grid.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val day = date.toEpochDay()
                            val dayTasks = byDay[day].orEmpty()
                            DayCell(
                                date = date,
                                inMonth = YearMonth.from(date) == month,
                                isToday = day == todayDay,
                                isSelected = day == selected,
                                marker = when {
                                    dayTasks.isEmpty() -> Marker.NONE
                                    dayTasks.all { it.completedAt != null } -> Marker.DONE
                                    day < todayDay -> Marker.OVERDUE
                                    else -> Marker.PENDING
                                },
                                onClick = { selected = day },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Text(
                text = LocalDate.ofEpochDay(selected).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                    .replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp),
            )

            val dayTasks: List<NoteWithItems> = byDay[selected].orEmpty().mapNotNull { byId[it.id] }
            if (dayTasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(dayTasks, key = { it.note.id }) { task ->
                    TaskRow(
                        task = task,
                        overdue = selected < todayDay && task.note.completedAt == null,
                        onToggle = { done -> vm.setCompleted(task.note.id, done) },
                        onOpen = { onOpenTask(task.note.id) },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onNewTask(TaskLogic.dueAtOfDay(selected)) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

private enum class Marker { NONE, PENDING, OVERDUE, DONE }

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    marker: Marker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> scheme.onPrimary
        inMonth -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.35f)
    }
    val dotColor = when {
        isSelected -> scheme.onPrimary
        marker == Marker.OVERDUE -> scheme.error
        marker == Marker.DONE -> scheme.outline
        else -> scheme.primary
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) scheme.primary else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (marker == Marker.NONE) {
                Spacer(Modifier.size(6.dp))
            } else {
                Box(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(dotColor))
            }
        }
    }
}
