package io.github.estebangocoalt.blocdenotas.ui.editor

import android.Manifest
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.attachments.VoicePlayer
import io.github.estebangocoalt.blocdenotas.attachments.VoiceRecorder
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.AttachmentEntity
import io.github.estebangocoalt.blocdenotas.data.AttachmentKind
import io.github.estebangocoalt.blocdenotas.data.AttachmentLogic
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Imágenes y notas de voz de una nota: botones para añadir, miniaturas y reproductor. */
@Composable
internal fun AttachmentsSection(vm: EditorViewModel, swatch: NoteSwatch, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { context.blocApp.attachmentStore }
    val recorder = remember { VoiceRecorder(context) }
    val player = remember { VoicePlayer() }
    var recording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var target by remember { mutableStateOf<File?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<AttachmentEntity?>(null) }

    val imageFailed = stringResource(R.string.attach_image_failed)
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

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addImage(uri) { Toast.makeText(context, imageFailed, Toast.LENGTH_SHORT).show() }
    }

    fun startRecording() {
        player.stop()
        playingId = null
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
        if (file != null && duration != null) vm.addAudio(file.name, duration)
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, micDenied, Toast.LENGTH_LONG).show()
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("🖼  " + stringResource(R.string.attach_image), color = swatch.accent, fontWeight = FontWeight.SemiBold)
            }
            if (recording) {
                TextButton(onClick = ::stopRecording) {
                    Text(
                        "⏹  " + stringResource(R.string.attach_stop, AttachmentLogic.formatDuration(elapsed)),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                TextButton(onClick = {
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("🎙  " + stringResource(R.string.attach_record), color = swatch.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        val images = vm.attachments.filter { it.attachmentKind == AttachmentKind.IMAGE }
        if (images.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                images.forEach { attachment ->
                    androidx.compose.runtime.key(attachment.id) {
                        val thumb by produceState<ImageBitmap?>(null, attachment.fileName) {
                            value = withContext(Dispatchers.IO) { store.loadBitmap(attachment.fileName, 360)?.asImageBitmap() }
                        }
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.4f))) {
                            thumb?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = stringResource(R.string.attach_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp).clickable { viewing = attachment },
                                )
                            }
                            RemoveBadge(Modifier.align(Alignment.TopEnd).padding(4.dp)) { vm.removeAttachment(attachment) }
                        }
                    }
                }
            }
        }

        vm.attachments.filter { it.attachmentKind == AttachmentKind.AUDIO }.forEach { attachment ->
            androidx.compose.runtime.key(attachment.id) {
                val playing = playingId == attachment.id
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable {
                            if (playing) {
                                player.stop()
                                playingId = null
                            } else {
                                val file = store.fileOf(attachment.fileName)
                                if (file != null && file.exists() && player.play(file) { playingId = null }) {
                                    playingId = attachment.id
                                } else {
                                    Toast.makeText(context, micFailed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (playing) "⏹" else "▶", color = swatch.accent)
                    Text(
                        text = stringResource(R.string.attach_voice) + " · " + AttachmentLogic.formatDuration(attachment.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = swatch.content,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                    RemoveBadge {
                        if (playing) {
                            player.stop()
                            playingId = null
                        }
                        vm.removeAttachment(attachment)
                    }
                }
            }
        }
    }

    viewing?.let { attachment ->
        Dialog(onDismissRequest = { viewing = null }) {
            val big by produceState<ImageBitmap?>(null, attachment.fileName) {
                value = withContext(Dispatchers.IO) { store.loadBitmap(attachment.fileName, AttachmentLogic.MAX_IMAGE_SIDE)?.asImageBitmap() }
            }
            Box(Modifier.clip(RoundedCornerShape(16.dp)).clickable { viewing = null }) {
                big?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.attach_image),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
