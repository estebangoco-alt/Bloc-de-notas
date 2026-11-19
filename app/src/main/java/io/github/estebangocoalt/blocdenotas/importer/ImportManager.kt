package io.github.estebangocoalt.blocdenotas.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.backup.BackupException
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.AttachmentEntity
import io.github.estebangocoalt.blocdenotas.data.AttachmentKind
import io.github.estebangocoalt.blocdenotas.data.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipInputStream

/** Importa notas de Google Keep (Takeout, ZIP o JSON) y tareas de Todoist (CSV). */
class ImportManager(private val context: Context) {

    private val maxEntry = 25 * 1024 * 1024
    private val maxTotal = 128 * 1024 * 1024

    suspend fun importKeep(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val jsons = ArrayList<String>()
        val files = HashMap<String, ByteArray>()
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("No se pudo abrir el archivo")
        val head = stream.buffered().use { input ->
            input.mark(4)
            val magic = ByteArray(2)
            val n = input.read(magic)
            input.reset()
            if (n == 2 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()) {
                readZip(input, jsons, files)
                null
            } else {
                String(readLimited(input, maxEntry), Charsets.UTF_8)
            }
        }
        if (head != null) jsons += head

        val app = context.blocApp
        val now = System.currentTimeMillis()
        val notes = ArrayList<io.github.estebangocoalt.blocdenotas.data.NoteEntity>()
        val items = ArrayList<io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity>()
        val attachments = ArrayList<AttachmentEntity>()
        for (text in jsons) {
            val keep = KeepImport.parse(text, now) ?: continue
            notes += keep.note
            items += keep.items
            keep.attachments.forEachIndexed { index, a ->
                val bytes = files[a.name.substringAfterLast('/')] ?: return@forEachIndexed
                val kind = if (a.isAudio) AttachmentKind.AUDIO else AttachmentKind.IMAGE
                val ext = a.name.substringAfterLast('.', kind.extension).lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,5}")) } ?: kind.extension
                val fileName = UUID.nameUUIDFromBytes("${keep.note.id}:att:$index".toByteArray()).toString() + "." + ext
                if (!app.attachmentStore.exists(fileName) && !app.attachmentStore.write(fileName, bytes)) return@forEachIndexed
                attachments += AttachmentEntity(
                    id = UUID.nameUUIDFromBytes("${keep.note.id}:attrow:$index".toByteArray()).toString(),
                    noteId = keep.note.id, kind = kind.name, fileName = fileName,
                    position = attachments.count { it.noteId == keep.note.id },
                    createdAt = keep.note.createdAt,
                )
            }
        }
        if (notes.isEmpty()) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        app.repository.importExternal(notes, items, attachments = attachments)
    }

    /** Devuelve el resultado y cuántas fechas no se entendieron. */
    suspend fun importTodoist(uri: Uri): Pair<ImportResult, Int> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { readLimited(it, maxEntry) }
            ?: throw IOException("No se pudo abrir el archivo")
        val name = displayName(uri).substringBeforeLast('.').ifBlank { "Todoist" }
        val repo = context.blocApp.repository
        val result = TodoistImport.parse(
            csv = String(bytes, Charsets.UTF_8), project = name,
            defaultColumn = context.getString(R.string.import_default_column),
            boardPosition = repo.boardCount(), nowMs = System.currentTimeMillis(), today = LocalDate.now(),
        ) ?: throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        repo.importExternal(
            result.notes, result.items,
            columns = result.columns, boards = listOf(result.board),
        ) to result.unparsedDates
    }

    private fun readZip(input: InputStream, jsons: MutableList<String>, files: MutableMap<String, ByteArray>) {
        var total = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name.substringAfterLast('/')
                val isJson = name.endsWith(".json", ignoreCase = true)
                val bytes = readLimited(zip, maxEntry, skipIfTooBig = true) ?: continue
                total += bytes.size
                if (total > maxTotal) throw BackupException(BackupException.Reason.TOO_LARGE)
                if (isJson) jsons += String(bytes, Charsets.UTF_8) else files[name] = bytes
            }
        }
    }

    private fun readLimited(input: InputStream, max: Int): ByteArray = readLimited(input, max, false)!!

    private fun readLimited(input: InputStream, max: Int, skipIfTooBig: Boolean): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        var tooBig = false
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > max) {
                if (!skipIfTooBig) throw BackupException(BackupException.Reason.TOO_LARGE)
                tooBig = true
            }
            if (!tooBig) out.write(buffer, 0, n)
        }
        return if (tooBig) null else out.toByteArray()
    }

    private fun displayName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
}
