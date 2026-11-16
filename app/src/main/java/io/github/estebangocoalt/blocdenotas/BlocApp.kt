package io.github.estebangocoalt.blocdenotas

import androidx.glance.appwidget.updateAll
import io.github.estebangocoalt.blocdenotas.widget.AgendaWidget
import io.github.estebangocoalt.blocdenotas.widget.PinnedWidget
import io.github.estebangocoalt.blocdenotas.widget.TaskListWidget
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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
    val themePrefs: io.github.estebangocoalt.blocdenotas.ui.theme.ThemePrefs by lazy {
        io.github.estebangocoalt.blocdenotas.ui.theme.ThemePrefs(this)
    }
    val savedViews: io.github.estebangocoalt.blocdenotas.data.SavedViewsStore by lazy {
        io.github.estebangocoalt.blocdenotas.data.SavedViewsStore(this)
    }
    val appLock: io.github.estebangocoalt.blocdenotas.lock.AppLock by lazy {
        io.github.estebangocoalt.blocdenotas.lock.AppLock(this)
    }
    val calendarPrefs: io.github.estebangocoalt.blocdenotas.data.CalendarPrefs by lazy {
        io.github.estebangocoalt.blocdenotas.data.CalendarPrefs(this)
    }
    val backupManager: BackupManager by lazy { BackupManager(this, repository) }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannel(this)
        repository.appScope.launch { repository.purgeOldTrash() }
        // Los widgets de lista, agenda y fijadas se redibujan cuando cambian las notas.
        repository.appScope.launch {
            repository.notes.debounce(600).collect {
                runCatching {
                    TaskListWidget().updateAll(this@BlocApp)
                    AgendaWidget().updateAll(this@BlocApp)
                    PinnedWidget().updateAll(this@BlocApp)
                }
            }
        }
    }
}

/** Acceso corto a la aplicación desde cualquier [Context]. */
val Context.blocApp: BlocApp
    get() = applicationContext as BlocApp
