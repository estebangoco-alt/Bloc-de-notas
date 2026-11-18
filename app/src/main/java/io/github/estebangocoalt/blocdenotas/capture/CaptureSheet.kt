package io.github.estebangocoalt.blocdenotas.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.estebangocoalt.blocdenotas.attachments.VoicePlayer
import io.github.estebangocoalt.blocdenotas.attachments.VoiceRecorder
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.AttachmentLogic
import io.github.estebangocoalt.blocdenotas.data.CaptureAudio
import kotlinx.coroutines.delay
import java.io.File
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
    autoRecord: Boolean = false,
    onSave: (text: String, destination: CaptureDestination, priority: Int, audio: CaptureAudio?) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { context.blocApp.attachmentStore }
    val recorder = remember { VoiceRecorder(context) }
    val player = remember { VoicePlayer() }
    var audioName by rememberSaveable { mutableStateOf("") }
    var audioMs by rememberSaveable { mutableLongStateOf(0L) }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var target by remember { mutableStateOf<File?>(null) }
    val micFailed = stringResource(R.string.attach_mic_failed)
    val micDenied = stringResource(R.string.attach_mic_denied)

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            player.stop()
        }
    }
    LaunchedEffect(recording) {
        while (recording) {
            elapsed = SystemClock.elapsedRealtime() - startedAt
            delay(250)
        }
    }

    // Quita la grabación pendiente (y su archivo) si no se va a guardar.
    fun discardAudio() {
        player.stop()
        playing = false
        if (audioName.isNotEmpty()) store.delete(audioName)
        audioName = ""
        audioMs = 0
    }

    fun startRecording() {
        discardAudio()
        val file = store.newAudioFile()
        if (recorder.start(file)) {
            target = file
            startedAt = SystemClock.elapsedRealtime()
            elapsed = 0
            recording = true
        } else {
            Toast.makeText(context, micFailed, Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        val file = target
        val duration = recorder.stop()
        recording = false
        target = null
        if (file != null && duration != null) {
            audioName = file.name
            audioMs = duration
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, micDenied, Toast.LENGTH_LONG).show()
    }

    var text by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf(CaptureDestination.INBOX) }
    var priority by rememberSaveable { mutableStateOf(Priority.NONE) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoRecord && audioName.isEmpty()) {
            // Desde el micrófono del widget: se empieza a grabar sin esperar (pide permiso si falta).
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    discardAudio()
                    onDismiss()
                },
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
                // Nota de voz: se graba aquí y se guarda junto al texto (o sola).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recording) {
                        OutlinedButton(onClick = ::stopRecording) {
                            Text(
                                "⏹  " + stringResource(R.string.attach_stop, AttachmentLogic.formatDuration(elapsed)),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        OutlinedButton(onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }) {
                            Text("🎙  " + stringResource(if (audioName.isEmpty()) R.string.attach_record else R.string.capture_record_again))
                        }
                    }
                    if (audioName.isNotEmpty() && !recording) {
                        Text(
                            text = (if (playing) "⏹ " else "▶ ") + AttachmentLogic.formatDuration(audioMs),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    if (playing) {
                                        player.stop()
                                        playing = false
                                    } else {
                                        val file = store.fileOf(audioName)
                                        if (file != null && player.play(file) { playing = false }) playing = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Text(
                            text = "✕",
                            modifier = Modifier.clip(CircleShape).clickable(onClick = ::discardAudio).padding(10.dp),
                        )
                    }
                }
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
                    onClick = {
                        player.stop()
                        onSave(text, destination, priority, if (audioName.isEmpty()) null else CaptureAudio(audioName, audioMs))
                    },
                    enabled = !recording && (text.isNotBlank() || audioName.isNotEmpty()),
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
