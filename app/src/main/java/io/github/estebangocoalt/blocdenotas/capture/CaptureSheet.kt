package io.github.estebangocoalt.blocdenotas.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.CaptureDestination
import io.github.estebangocoalt.blocdenotas.data.Priority
import io.github.estebangocoalt.blocdenotas.ui.common.priorityLabelRes

/**
 * Hoja de captura rápida: se abre desde el widget con el teclado ya activo.
 * Por defecto guarda en la bandeja; el destino se puede cambiar antes de guardar.
 */
@Composable
fun CaptureSheet(
    onDismiss: () -> Unit,
    onSave: (text: String, destination: CaptureDestination, priority: Int) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf(CaptureDestination.INBOX) }
    var priority by rememberSaveable { mutableStateOf(Priority.NONE) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            onClick = {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.capture_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.capture_hint)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .focusRequester(focusRequester),
                )
                Text(
                    text = stringResource(R.string.capture_save_to),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CaptureDestination.entries.forEach { option ->
                        FilterChip(
                            selected = destination == option,
                            onClick = { destination = option },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
                // La prioridad solo se elige cuando se guarda como tarea.
                if (destination == CaptureDestination.TASK) {
                    Text(
                        text = stringResource(R.string.capture_priority),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Priority.NONE, Priority.LOW, Priority.MEDIUM, Priority.HIGH).forEach { level ->
                            FilterChip(
                                selected = priority == level,
                                onClick = { priority = level },
                                label = { Text(stringResource(priorityLabelRes(level))) },
                            )
                        }
                    }
                }
                Button(
                    onClick = { onSave(text, destination, priority) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.capture_save))
                }
            }
        }
    }
}

private fun CaptureDestination.labelRes(): Int = when (this) {
    CaptureDestination.INBOX -> R.string.kind_inbox
    CaptureDestination.NOTE -> R.string.kind_note
    CaptureDestination.TASK -> R.string.kind_task
    CaptureDestination.IDEA -> R.string.kind_idea
    CaptureDestination.SHOPPING -> R.string.kind_shopping
}
