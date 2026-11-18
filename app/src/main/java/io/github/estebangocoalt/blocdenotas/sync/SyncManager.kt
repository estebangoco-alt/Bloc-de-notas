package io.github.estebangocoalt.blocdenotas.sync

import android.content.Context
import io.github.estebangocoalt.blocdenotas.backup.BackupException
import io.github.estebangocoalt.blocdenotas.backup.BackupManager
import io.github.estebangocoalt.blocdenotas.backup.ImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Ajustes de la sincronización (en preferencias privadas de la app; la sincronización está apagada por defecto). */
class SyncPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)

    var url: String
        get() = prefs.getString("url", "").orEmpty()
        set(value) = prefs.edit().putString("url", value).apply()
    var user: String
        get() = prefs.getString("user", "").orEmpty()
        set(value) = prefs.edit().putString("user", value).apply()
    var password: String
        get() = prefs.getString("password", "").orEmpty()
        set(value) = prefs.edit().putString("password", value).apply()
    var autoOnOpen: Boolean
        get() = prefs.getBoolean("auto", false)
        set(value) = prefs.edit().putBoolean("auto", value).apply()
    var lastSyncAt: Long
        get() = prefs.getLong("last", 0L)
        set(value) = prefs.edit().putLong("last", value).apply()

    val config: SyncConfig get() = SyncConfig(url, user, password)
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Done(val added: Int, val updated: Int) : SyncState
    data class Failed(val reason: SyncException.Reason) : SyncState
}

/**
 * Sincroniza con un archivo ZIP en tu servidor WebDAV: descarga el remoto, lo fusiona con lo local
 * (gana la nota más reciente) y sube el resultado. Si otro dispositivo sube a la vez, repite.
 * Las notas borradas del todo (vaciar papelera) no se propagan: pasan por la papelera, que sí sincroniza.
 */
class SyncManager(
    private val prefs: SyncPrefs,
    private val backup: BackupManager,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state

    suspend fun syncNow(allowInsecure: Boolean = false): SyncState {
        if (!mutex.tryLock()) return _state.value
        _state.value = SyncState.Running
        val result = try {
            withContext(Dispatchers.IO) { run(allowInsecure) }
        } catch (e: SyncException) {
            SyncState.Failed(e.reason)
        } catch (e: BackupException) {
            SyncState.Failed(SyncException.Reason.INVALID_REMOTE)
        } catch (e: Exception) {
            SyncState.Failed(SyncException.Reason.SERVER)
        } finally {
            mutex.unlock()
        }
        _state.value = result
        return result
    }

    private suspend fun run(allowInsecure: Boolean): SyncState {
        val config = prefs.config
        if (!config.isComplete) throw SyncException(SyncException.Reason.NOT_CONFIGURED)
        val client = WebDavClient(config, allowInsecure)
        var added = 0
        var updated = 0
        repeat(MAX_ATTEMPTS) {
            val remote = client.get()
            if (remote != null) {
                val result = backup.applyBytes(remote.bytes, ImportMode.MERGE)
                added += result.added
                updated += result.updated
            }
            val (bytes, _) = backup.buildBytes()
            try {
                client.put(bytes, remote?.etag)
                prefs.lastSyncAt = System.currentTimeMillis()
                return SyncState.Done(added, updated)
            } catch (e: SyncException) {
                if (e.reason != SyncException.Reason.CONFLICT) throw e
                // Otro dispositivo subió mientras tanto: se vuelve a descargar y fusionar.
            }
        }
        throw SyncException(SyncException.Reason.CONFLICT)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
