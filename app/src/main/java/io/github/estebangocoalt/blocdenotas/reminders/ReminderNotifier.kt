package io.github.estebangocoalt.blocdenotas.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.estebangocoalt.blocdenotas.MainActivity
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate

object ReminderNotifier {
    private const val CHANNEL_ID = "task_reminders"

    fun createChannel(context: Context) {
        val channel = androidx.core.app.NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName(context.getString(R.string.reminder_channel_name))
            .setDescription(context.getString(R.string.reminder_channel_desc))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun notificationId(noteId: String): Int = noteId.hashCode()

    @SuppressLint("MissingPermission") // se comprueba con areNotificationsEnabled()
    fun show(context: Context, note: NoteEntity) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val id = notificationId(note.id)
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(ReminderIntents.EXTRA_NOTE_ID, note.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val done = PendingIntent.getBroadcast(
            context, id,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_DONE)
                .putExtra(ReminderIntents.EXTRA_NOTE_ID, note.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = note.title.ifBlank {
            note.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
                .ifBlank { context.getString(R.string.board_untitled) }
        }
        val text = note.dueAt?.let { context.getString(R.string.reminder_text, formatDueDate(it)) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.task_mark_done), done)
            .build()
        manager.notify(id, notification)
    }
}
