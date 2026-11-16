package io.github.estebangocoalt.blocdenotas.backup

import android.content.Context
import android.net.Uri
import io.github.estebangocoalt.blocdenotas.data.ImportResult
import io.github.estebangocoalt.blocdenotas.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

/**
 * Exporta e importa la copia de seguridad usando el selector de archivos del sistema
 * (Storage Access Framework). Por eso la app no necesita ningún permiso de almacenamiento:
 * el usuario elige dónde guardar y de dónde leer.
 */
class BackupManager(
    private val context: Context,
    private val repository: NoteRepository,
) {

    /** Escribe la copia en [uri]. Devuelve cuántas notas se han exportado. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val file = repository.buildBackup(appVersion(), System.currentTimeMillis())
        val bytes = BackupCodec.encode(file)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("No se pudo abrir el archivo de destino")
        stream.use { it.write(bytes) }
        file.notes.size
    }

    /** Lee la copia de [uri] y la aplica según [mode]. */
    suspend fun import(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("No se pudo abrir el archivo de origen")
        val bytes = stream.use { BackupZip.readLimited(it) }
        val file = BackupCodec.decode(bytes)
        repository.importBackup(file, mode)
    }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    companion object {
        fun suggestedFileName(): String = "bloc-de-notas-${LocalDate.now()}.zip"
    }
}
