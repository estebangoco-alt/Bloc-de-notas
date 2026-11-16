package io.github.estebangocoalt.blocdenotas.backup

import kotlinx.serialization.json.Json

/**
 * Convierte una copia de seguridad a bytes (ZIP con `backup.json`) y viceversa.
 * Al leer acepta tanto el ZIP como un JSON suelto.
 */
object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // tolera campos nuevos de versiones posteriores del mismo formato
        encodeDefaults = true
    }

    fun encode(file: BackupFile): ByteArray {
        val text = json.encodeToString(BackupFile.serializer(), file)
        return BackupZip.pack(BACKUP_ENTRY, text.toByteArray(Charsets.UTF_8))
    }

    fun decode(bytes: ByteArray): BackupFile {
        val jsonBytes = if (BackupZip.isZip(bytes)) BackupZip.unpackEntry(bytes, BACKUP_ENTRY) else bytes
        val file = try {
            json.decodeFromString(BackupFile.serializer(), String(jsonBytes, Charsets.UTF_8))
        } catch (e: IllegalArgumentException) { // incluye SerializationException
            throw BackupException(BackupException.Reason.NOT_A_BACKUP, e)
        }
        if (file.format != BACKUP_FORMAT) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        }
        if (file.version < 1 || file.version > BACKUP_VERSION) {
            throw BackupException(BackupException.Reason.UNSUPPORTED_VERSION)
        }
        return file
    }
}
