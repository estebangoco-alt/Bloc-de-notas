package io.github.estebangocoalt.blocdenotas.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.CaptureDestination
import kotlinx.coroutines.launch

/**
 * Recibe texto compartido desde otras apps ("Compartir > Guardar en Bloc de notas")
 * y lo guarda en la bandeja. No tiene interfaz: guarda y se cierra.
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            val shared = listOfNotNull(subject, text)
                .filter { it.isNotBlank() }
                .joinToString("\n")
                // Cualquier app puede compartir texto aquí: se limita el tamaño para que nadie llene la base de datos.
                .take(MAX_SHARED_CHARS)

            if (shared.isNotBlank()) {
                val repository = blocApp.repository
                repository.appScope.launch { repository.capture(shared, CaptureDestination.INBOX) }
                Toast.makeText(this, R.string.share_saved, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private companion object {
        const val MAX_SHARED_CHARS = 100_000
    }
}
