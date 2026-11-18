package io.github.estebangocoalt.blocdenotas.attachments

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Graba una nota de voz (AAC en un .m4a). Una sola grabación a la vez. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L
    private var target: File? = null

    val isRecording: Boolean get() = recorder != null

    /** Empieza a grabar en [file]. Devuelve false si el micrófono no está disponible. */
    fun start(file: File): Boolean {
        if (recorder != null) return false
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.path)
            r.prepare()
            r.start()
            recorder = r
            target = file
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            file.delete()
            false
        }
    }

    /** Para y devuelve la duración en milisegundos, o null si la grabación fue demasiado corta o falló. */
    fun stop(): Long? {
        val r = recorder ?: return null
        val file = target
        recorder = null
        target = null
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        if (!ok || elapsed < 500) {
            file?.delete()
            return null
        }
        return elapsed
    }

    /** Descarta la grabación en curso (al salir de la pantalla). */
    fun cancel() {
        val file = target
        stop()
        file?.delete()
    }
}

/** Reproduce un audio cada vez. [onDone] se llama al terminar o al parar. */
class VoicePlayer {
    private var player: MediaPlayer? = null

    fun play(file: File, onDone: () -> Unit): Boolean {
        stop()
        return try {
            val p = MediaPlayer()
            p.setDataSource(file.path)
            p.setOnCompletionListener {
                stop()
                onDone()
            }
            p.prepare()
            p.start()
            player = p
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
    }
}
