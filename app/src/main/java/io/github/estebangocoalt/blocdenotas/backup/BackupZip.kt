package io.github.estebangocoalt.blocdenotas.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Empaquetado ZIP de la copia de seguridad. Lógica pura (sin Android).
 *
 * Nunca se extraen archivos al disco: solo se lee una entrada con nombre exacto en memoria
 * y con límite de tamaño, así que no hay riesgo de "zip slip" ni de "zip bomb".
 */
object BackupZip {
    const val MAX_BYTES: Int = 64 * 1024 * 1024

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && // 'P'
            bytes[1] == 0x4B.toByte() && // 'K'
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

    fun pack(entryName: String, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(data)
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    fun unpackEntry(zip: ByteArray, entryName: String): ByteArray {
        try {
            ZipInputStream(ByteArrayInputStream(zip)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (!entry.isDirectory && entry.name == entryName) {
                        return readLimited(input)
                    }
                }
            }
        } catch (e: ZipException) {
            throw BackupException(BackupException.Reason.CORRUPT, e)
        } catch (e: IOException) {
            throw BackupException(BackupException.Reason.CORRUPT, e)
        }
        throw BackupException(BackupException.Reason.NOT_A_BACKUP)
    }

    fun readLimited(input: InputStream, max: Int = MAX_BYTES): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > max) throw BackupException(BackupException.Reason.TOO_LARGE)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
