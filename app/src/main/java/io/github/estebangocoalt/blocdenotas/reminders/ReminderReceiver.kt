package io.github.estebangocoalt.blocdenotas.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.estebangocoalt.blocdenotas.blocApp
import kotlinx.coroutines.launch

/** Recibe la alarma de una tarea (muestra el aviso) y el botón «Marcar como hecha» del aviso. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(ReminderIntents.EXTRA_NOTE_ID) ?: return
        val repository = context.blocApp.repository
        val pending = goAsync()
        repository.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_REMIND -> {
                        val note = repository.get(noteId)?.note
                        if (note != null && note.completedAt == null) ReminderNotifier.show(context, note)
                    }
                    ACTION_DONE -> {
                        repository.setCompleted(noteId, true)
                        NotificationManagerCompat.from(context).cancel(ReminderNotifier.notificationId(noteId))
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "io.github.estebangocoalt.blocdenotas.action.REMIND"
        const val ACTION_DONE = "io.github.estebangocoalt.blocdenotas.action.DONE"
    }
}
