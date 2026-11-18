package io.github.estebangocoalt.blocdenotas.capture

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.ui.theme.BlocTheme
import kotlinx.coroutines.launch

/** Se abre desde el widget de captura rápida. Guarda y se cierra enseguida. */
class QuickCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = blocApp.repository
        val autoRecord = intent.getBooleanExtra(EXTRA_RECORD, false)
        setContent {
            BlocTheme {
                CaptureSheet(
                    autoRecord = autoRecord,
                    onDismiss = { finish() },
                    onSave = { text, destination, priority, audio ->
                        // El ámbito de la app sobrevive al cierre de esta actividad.
                        val audioTitle = getString(R.string.attach_voice)
                        repository.appScope.launch { repository.capture(text, destination, priority, audio, audioTitle) }
                        Toast.makeText(this, R.string.capture_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        /** Acción distinta para que el widget tenga un PendingIntent propio para el micrófono. */
        const val ACTION_RECORD = "io.github.estebangocoalt.blocdenotas.action.RECORD"
        const val EXTRA_RECORD = "record"
    }
}
