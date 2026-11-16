package io.github.estebangocoalt.blocdenotas.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.ReminderLogic
import io.github.estebangocoalt.blocdenotas.data.ReminderScheduler
import java.time.ZoneId

/**
 * Programa los avisos con AlarmManager. Si el usuario no ha concedido las «alarmas exactas»,
 * el aviso se programa igualmente pero puede retrasarse unos minutos (Android lo agrupa).
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun sync(note: NoteEntity) {
        val trigger = ReminderLogic.triggerAt(note, System.currentTimeMillis(), ZoneId.systemDefault())
        if (trigger == null) {
            cancel(note.id)
            return
        }
        val intent = pendingIntent(note.id)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canBeExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
            }
        } catch (e: SecurityException) {
            // El permiso se retiró justo ahora: usamos la alarma aproximada.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
    }

    override fun cancel(noteId: String) {
        val intent = pendingIntent(noteId)
        alarmManager.cancel(intent)
        intent.cancel()
    }

    private fun pendingIntent(noteId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            // El «data» distingue las alarmas de cada tarea entre sí.
            .setData(Uri.parse("bloc-de-notas://reminder/$noteId"))
            .putExtra(ReminderIntents.EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
