package io.github.estebangocoalt.blocdenotas.ui.calendar

import io.github.estebangocoalt.blocdenotas.ui.common.SettingsButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.background
import io.github.estebangocoalt.blocdenotas.data.Santoral
import io.github.estebangocoalt.blocdenotas.data.HolidayLogic
import io.github.estebangocoalt.blocdenotas.blocApp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import io.github.estebangocoalt.blocdenotas.data.ViewScope
import io.github.estebangocoalt.blocdenotas.ui.common.TaskFilterBar
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TasksViewModel = viewModel(),
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    remember(vm) { vm.scope = ViewScope.CALENDAR }
    val query by vm.query.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val allViews by vm.views.collectAsStateWithLifecycle()
    val views = remember(allViews) { vm.viewsOf(allViews) }
    val today = remember { LocalDate.now() }
    // Se guardan como días desde 1970 para que sobrevivan a rotar la pantalla.
    var monthStart by rememberSaveable { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var selected by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var agenda by rememberSaveable { mutableStateOf(false) }

    val locale = Locale.getDefault()
    val month = YearMonth.from(LocalDate.ofEpochDay(monthStart))
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val grid = remember(month, firstDayOfWeek) { CalendarLogic.monthGrid(month, firstDayOfWeek) }
    val byDay = remember(tasks) { CalendarLogic.tasksByDay(tasks.map { it.note }) }
    val byId = remember(tasks) { tasks.associateBy { it.note.id } }
    val todayDay = today.toEpochDay()

    // Festivos y santoral (sin internet): según la zona y el interruptor de Ajustes.
    val calendarPrefs = LocalContext.current.blocApp.calendarPrefs
    val region by calendarPrefs.region.collectAsStateWithLifecycle()
    val showSaints by calendarPrefs.showSaints.collectAsStateWithLifecycle()
    val holidays = remember(region, month) {
        HolidayLogic.holidays(region, listOf(month.year - 1, month.year, month.year + 1))
    }
    val english = locale.language == "en"

    // Deslizar a izquierda o derecha en la vista de mes cambia de mes.
    val swipeToChangeMonth = Modifier.pointerInput(agenda) {
        if (agenda) return@pointerInput
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val shown = YearMonth.from(LocalDate.ofEpochDay(monthStart))
                when {
                    total <= -SWIPE_THRESHOLD -> monthStart = shown.plusMonths(1).atDay(1).toEpochDay()
                    total >= SWIPE_THRESHOLD -> monthStart = shown.minusMonths(1).atDay(1).toEpochDay()
                }
            },
            onDragCancel = { total = 0f },
            onHorizontalDrag = { _, amount -> total += amount },
        )
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().then(swipeToChangeMonth)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(false to R.string.view_month, true to R.string.view_agenda).forEach { (isAgenda, label) ->
                    FilterChip(
                        selected = agenda == isAgenda,
                        onClick = { agenda = isAgenda },
                        label = { Text(stringResource(label)) },
                        shape = CircleShape,
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
                Spacer(Modifier.weight(1f))
                SettingsButton(onOpenSettings)
            }
            TaskFilterBar(
                query = query,
                tags = tags,
                onChange = vm::setQuery,
                views = views,
                onApplyView = { view ->
                    vm.setQuery(view.tasks)
                    agenda = view.agenda
                },
                onSaveView = { name -> vm.saveView(name, agenda = agenda) },
                onDeleteView = vm::deleteView,
                modifier = Modifier.padding(top = 8.dp),
                showSort = false,
            )
            if (agenda) {
                AgendaList(
                    tasks = tasks,
                    todayDay = todayDay,
                    locale = locale,
                    onOpenTask = onOpenTask,
                    onToggle = { id, done -> vm.setCompleted(id, done) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { monthStart = month.minusMonths(1).atDay(1).toEpochDay() },
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
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
                IconButton(
                    onClick = { monthStart = month.plusMonths(1).atDay(1).toEpochDay() },
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
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
                                isHoliday = holidays.containsKey(date),
                                marker = when {
                                    dayTasks.isEmpty() -> Marker.NONE
                                    dayTasks.all { it.completedAt != null } -> Marker.DONE
                                    day < todayDay -> Marker.OVERDUE
                                    else -> Marker.PENDING
                                },
                                onClick = {
                                    selected = day
                                    monthStart = date.withDayOfMonth(1).toEpochDay()
                                },
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

            val selectedDate = LocalDate.ofEpochDay(selected)
            val holiday = holidays[selectedDate]
            val saint = if (showSaints) Santoral.of(selectedDate) else null
            if (holiday != null) {
                Text(
                    text = stringResource(R.string.calendar_holiday, if (english) holiday.en else holiday.es),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            if (saint != null) {
                Text(
                    text = stringResource(R.string.calendar_saint, saint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

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
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
        }

        ExtendedFloatingActionButton(
            onClick = { onNewTask(TaskLogic.dueAtOfDay(selected)) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new), fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

/** Cuánto hay que deslizar (en píxeles) para cambiar de mes. */
private const val SWIPE_THRESHOLD = 120f

private enum class Marker { NONE, PENDING, OVERDUE, DONE }

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isHoliday: Boolean,
    marker: Marker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> scheme.onPrimary
        isHoliday && inMonth -> scheme.error
        isHoliday -> scheme.error.copy(alpha = 0.45f)
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
                fontWeight = if (isToday || isHoliday) FontWeight.Bold else FontWeight.Normal,
            )
            if (marker == Marker.NONE) {
                Spacer(Modifier.size(6.dp))
            } else {
                Box(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(dotColor))
            }
        }
    }
}

/** Vista de agenda: tareas vencidas y próximas, agrupadas por día. */
@Composable
private fun AgendaList(
    tasks: List<NoteWithItems>,
    todayDay: Long,
    locale: Locale,
    onOpenTask: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dated = remember(tasks) { tasks.filter { it.note.dueAt != null } }
    val order = compareBy<NoteWithItems>({ it.note.reminderMinute ?: -1 }, { it.note.title })
    val overdue = remember(dated, todayDay) {
        dated.filter { TaskLogic.dayOf(it.note.dueAt!!) < todayDay && it.note.completedAt == null }
            .sortedWith(compareBy<NoteWithItems> { it.note.dueAt }.then(order))
    }
    val days = remember(dated, todayDay) {
        dated.filter { TaskLogic.dayOf(it.note.dueAt!!) >= todayDay }
            .groupBy { TaskLogic.dayOf(it.note.dueAt!!) }
            .toSortedMap()
            .mapValues { (_, list) -> list.sortedWith(order) }
    }
    val dayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }

    if (overdue.isEmpty() && days.isEmpty()) {
        Text(
            text = stringResource(R.string.agenda_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(20.dp),
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (overdue.isNotEmpty()) {
            item(key = "h-overdue") {
                AgendaHeader(stringResource(R.string.agenda_overdue), MaterialTheme.colorScheme.error)
            }
            items(overdue, key = { "o-" + it.note.id }) { task ->
                TaskRow(
                    task = task,
                    overdue = true,
                    onToggle = { done -> onToggle(task.note.id, done) },
                    onOpen = { onOpenTask(task.note.id) },
                )
            }
        }
        days.forEach { (day, list) ->
            item(key = "h-$day") {
                val date = LocalDate.ofEpochDay(day)
                val text = date.format(dayFormat).replaceFirstChar { it.titlecase(locale) }
                val label = when (day) {
                    todayDay -> stringResource(R.string.agenda_today, text)
                    todayDay + 1 -> stringResource(R.string.agenda_tomorrow, text)
                    else -> text
                }
                AgendaHeader(label, MaterialTheme.colorScheme.primary)
            }
            items(list, key = { "d-$day-" + it.note.id }) { task ->
                TaskRow(
                    task = task,
                    overdue = false,
                    onToggle = { done -> onToggle(task.note.id, done) },
                    onOpen = { onOpenTask(task.note.id) },
                )
            }
        }
    }
}

@Composable
private fun AgendaHeader(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
    )
}
