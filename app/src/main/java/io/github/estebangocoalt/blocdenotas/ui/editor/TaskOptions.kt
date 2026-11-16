package io.github.estebangocoalt.blocdenotas.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.ReminderLogic
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import io.github.estebangocoalt.blocdenotas.ui.common.formatMinute
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch
import java.time.LocalDate

/** Opciones propias de las tareas en el editor: «hecha» y fecha límite. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptions(
    dueAt: Long?,
    completed: Boolean,
    swatch: NoteSwatch,
    onCompletedChange: (Boolean) -> Unit,
    onDueChange: (Long?) -> Unit,
    reminderMinute: Int?,
    onReminderChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Si lo deniega, el aviso se guarda pero Android no lo mostrará. Se puede cambiar en Ajustes. */ }

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = onCompletedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = swatch.accent,
                uncheckedColor = swatch.accent,
                checkmarkColor = swatch.background,
            ),
        )
        Text(stringResource(R.string.task_mark_done))
    }
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(
            onClick = { showPicker = true },
            label = {
                Text(if (dueAt == null) stringResource(R.string.task_add_due) else formatDueDate(dueAt))
            },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        )
        if (dueAt != null) {
            IconButton(onClick = { onDueChange(null) }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_remove_due))
            }
        }
    }

    if (dueAt != null) {
        Row(
            modifier = modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = { showTimePicker = true },
                label = {
                    Text(
                        if (reminderMinute == null) {
                            stringResource(R.string.task_add_reminder)
                        } else {
                            formatMinute(reminderMinute)
                        },
                    )
                },
                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            )
            if (reminderMinute != null) {
                IconButton(onClick = { onReminderChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_remove_reminder))
                }
            }
        }
    }

    if (showTimePicker) {
        val start = reminderMinute ?: ReminderLogic.DEFAULT_MINUTE
        val timeState = rememberTimePickerState(
            initialHour = start / 60,
            initialMinute = start % 60,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time_title)) },
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onReminderChange(timeState.hour * 60 + timeState.minute)
                    showTimePicker = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueAt ?: TaskLogic.dueAtOfDay(LocalDate.now().toEpochDay()),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDueChange(TaskLogic.normalize(it)) }
                    showPicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
