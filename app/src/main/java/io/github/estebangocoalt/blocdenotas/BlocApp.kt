package io.github.estebangocoalt.blocdenotas

import android.app.Application
import android.content.Context
import io.github.estebangocoalt.blocdenotas.backup.BackupManager
import io.github.estebangocoalt.blocdenotas.data.AppDatabase
import io.github.estebangocoalt.blocdenotas.data.NoteRepository
import io.github.estebangocoalt.blocdenotas.reminders.AlarmReminderScheduler
import io.github.estebangocoalt.blocdenotas.reminders.ReminderNotifier

class BlocApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: NoteRepository by lazy {
        NoteRepository(database.noteDao(), AlarmReminderScheduler(this))
    }
    val backupManager: BackupManager by lazy { BackupManager(this, repository) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannel(this)
    }
}

/** Acceso corto a la aplicación desde cualquier [Context]. */
val Context.blocApp: BlocApp
    get() = applicationContext as BlocApp
