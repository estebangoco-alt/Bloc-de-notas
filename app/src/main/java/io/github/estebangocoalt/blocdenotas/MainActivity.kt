package io.github.estebangocoalt.blocdenotas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.estebangocoalt.blocdenotas.reminders.ReminderIntents
import io.github.estebangocoalt.blocdenotas.ui.BlocNavHost
import io.github.estebangocoalt.blocdenotas.ui.theme.BlocTheme

class MainActivity : ComponentActivity() {
    /** Tarea que hay que abrir porque el usuario tocó su aviso. */
    private val noteToOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        noteToOpen.value = intent?.getStringExtra(ReminderIntents.EXTRA_NOTE_ID)
        setContent {
            BlocTheme {
                BlocNavHost(
                    openNoteId = noteToOpen.value,
                    onOpenNoteHandled = {
                        noteToOpen.value = null
                        intent?.removeExtra(ReminderIntents.EXTRA_NOTE_ID)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        noteToOpen.value = intent.getStringExtra(ReminderIntents.EXTRA_NOTE_ID)
    }
}
