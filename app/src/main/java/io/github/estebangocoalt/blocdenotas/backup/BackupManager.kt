package io.github.estebangocoalt.blocdenotas.backup

import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
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

    /** Construye la copia completa (notas, vistas, plantillas y archivos adjuntos) en un ZIP. */
    suspend fun buildBytes(): Pair<ByteArray, Int> = withContext(Dispatchers.IO) {
        val app = context.blocApp
        val file = repository.buildBackup(appVersion(), System.currentTimeMillis()).copy(
            views = app.savedViews.snapshot(),
            templates = app.customTemplates.snapshot(),
        )
        // Solo van los adjuntos cuyo archivo existe.
        val files = LinkedHashMap<String, ByteArray>()
        for (a in file.attachments) {
            if (a.fileName !in files) app.attachmentStore.read(a.fileName)?.let { files[a.fileName] = it }
        }
        BackupCodec.encode(file.copy(attachments = file.attachments.filter { it.fileName in files }), files) to file.notes.size
    }

    /** Escribe la copia en [uri]. Devuelve cuántas notas se han exportado. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val (bytes, count) = buildBytes()
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("No se pudo abrir el archivo de destino")
        stream.use { it.write(bytes) }
        count
    }

    /** Escribe un ZIP con un archivo .md por nota en [uri]. Devuelve cuántas notas se han exportado. */
    suspend fun exportMarkdown(uri: Uri): Int = withContext(Dispatchers.IO) {
        val (notes, items) = repository.allForExport()
        val files = MarkdownExport.files(notes, items, context.getString(R.string.board_untitled))
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("No se pudo abrir el archivo de destino")
        stream.use { it.write(MarkdownExport.pack(files)) }
        files.size
    }

    /** Lee la copia de [uri] y la aplica según [mode]. */
    suspend fun import(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("No se pudo abrir el archivo de origen")
        applyBytes(stream.use { BackupZip.readLimited(it) }, mode)
    }

    /** Aplica una copia ya leída (también la usa la sincronización). */
    suspend fun applyBytes(bytes: ByteArray, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val file = BackupCodec.decode(bytes)
        val app = context.blocApp
        // Primero se restauran los archivos; solo se importan los adjuntos cuyo archivo está.
        val restored = HashSet<String>()
        for ((name, data) in BackupCodec.decodeAttachments(bytes)) {
            if (app.attachmentStore.write(name, data)) restored += name
        }
        val result = repository.importBackup(file, mode, restored)
        // Vistas y plantillas viven en las preferencias de la app. Una copia antigua (sin ellas)
        // no debe borrar las que ya hay, ni siquiera al reemplazar.
        val replace = mode == ImportMode.REPLACE
        if (file.views.isNotEmpty()) app.savedViews.restore(file.views, replace)
        if (file.templates.isNotEmpty()) app.customTemplates.restore(file.templates, replace)
        app.attachmentStore.sweep(repository.allAttachmentFileNames())
        result
    }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    companion object {
        fun suggestedFileName(): String = "bloc-de-notas-${LocalDate.now()}.zip"

        fun suggestedMarkdownFileName(): String = "bloc-de-notas-markdown-${LocalDate.now()}.zip"
    }
}
