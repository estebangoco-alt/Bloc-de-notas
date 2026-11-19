package io.github.estebangocoalt.blocdenotas.ui.settings

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.backup.BackupException
import io.github.estebangocoalt.blocdenotas.backup.ImportMode
import io.github.estebangocoalt.blocdenotas.blocApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Mensaje para mostrar al usuario: recurso de texto y sus argumentos. */
data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val backup = app.blocApp.backupManager

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun export(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                UiMessage(R.string.msg_export_ok, listOf(backup.export(uri)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage(e)
            }
            _busy.value = false
        }
    }

    fun exportMarkdown(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                UiMessage(R.string.msg_export_md_ok, listOf(backup.exportMarkdown(uri)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage(e)
            }
            _busy.value = false
        }
    }

    fun import(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                val result = backup.import(uri, mode)
                UiMessage(R.string.msg_import_ok, listOf(result.added, result.updated, result.skipped))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage(e)
            }
            _busy.value = false
        }
    }

    fun importKeep(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                val r = getApplication<Application>().blocApp.importManager.importKeep(uri)
                UiMessage(R.string.msg_import_ok, listOf(r.added, r.updated, r.skipped))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage(e)
            }
            _busy.value = false
        }
    }

    fun importTodoist(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = try {
                val (r, bad) = getApplication<Application>().blocApp.importManager.importTodoist(uri)
                if (bad > 0) UiMessage(R.string.msg_import_dates, listOf(r.added, r.updated, r.skipped, bad))
                else UiMessage(R.string.msg_import_ok, listOf(r.added, r.updated, r.skipped))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage(e)
            }
            _busy.value = false
        }
    }

    /** Activa o desactiva el bloqueo. Solo se puede activar si el móvil tiene huella o bloqueo de pantalla. */
    fun setLockEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        if (enabled && !io.github.estebangocoalt.blocdenotas.lock.AppLock.isAvailable(app)) {
            _message.value = UiMessage(R.string.msg_lock_unavailable)
            return
        }
        app.blocApp.appLock.setEnabled(enabled)
    }

    fun loadDemo() {
        viewModelScope.launch {
            val spanish = java.util.Locale.getDefault().language == "es"
            getApplication<Application>().blocApp.repository
                .loadDemo(spanish, java.time.LocalDate.now().toEpochDay())
            _message.value = UiMessage(R.string.msg_demo_loaded)
        }
    }

    fun removeDemo() {
        viewModelScope.launch {
            getApplication<Application>().blocApp.repository.removeDemo()
            _message.value = UiMessage(R.string.msg_demo_removed)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun errorMessage(e: Exception): UiMessage = when {
        e is BackupException -> when (e.reason) {
            BackupException.Reason.NOT_A_BACKUP,
            BackupException.Reason.CORRUPT,
            -> UiMessage(R.string.msg_error_invalid)
            BackupException.Reason.UNSUPPORTED_VERSION -> UiMessage(R.string.msg_error_version)
            BackupException.Reason.TOO_LARGE -> UiMessage(R.string.msg_error_too_large)
        }
        else -> UiMessage(R.string.msg_error_generic)
    }
}
