package io.github.estebangocoalt.blocdenotas.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import io.github.estebangocoalt.blocdenotas.data.AttachmentKind
import io.github.estebangocoalt.blocdenotas.data.AttachmentLogic
import java.io.File
import java.io.FileOutputStream

/**
 * Archivos de las imágenes y notas de voz, en el almacenamiento privado de la app
 * (`files/attachments`). Ninguna otra app puede leerlos; salen de aquí solo en la copia de seguridad.
 */
class AttachmentStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val dir = File(context.applicationContext.filesDir, AttachmentLogic.FOLDER).apply { mkdirs() }

    /** Archivo de un adjunto, o null si el nombre no es seguro. */
    fun fileOf(name: String): File? =
        if (AttachmentLogic.isSafeName(name)) File(dir, name) else null

    /** Archivo nuevo para una grabación de voz. */
    fun newAudioFile(): File = File(dir, AttachmentLogic.newFileName(AttachmentKind.AUDIO))

    /** Copia la imagen elegida a la app, reducida y en JPEG. Devuelve el nombre del archivo o null si falla. */
    fun saveImage(uri: Uri): String? = runCatching {
        val bitmap = decode(uri) ?: return null
        val name = AttachmentLogic.newFileName(AttachmentKind.IMAGE)
        FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        name
    }.getOrNull()

    private fun decode(uri: Uri): Bitmap? {
        val max = AttachmentLogic.MAX_IMAGE_SIDE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder respeta la orientación EXIF de las fotos.
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val (w, h) = AttachmentLogic.scaledSize(info.size.width, info.size.height, max)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= max) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    /** Miniatura o vista grande de una imagen guardada, con el lado mayor aproximado a [maxSide]. */
    fun loadBitmap(name: String, maxSide: Int): Bitmap? {
        val file = fileOf(name)?.takeIf { it.exists() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun delete(name: String) {
        fileOf(name)?.delete()
    }

    fun read(name: String): ByteArray? = fileOf(name)?.takeIf { it.isFile }?.readBytes()

    /** Guarda un archivo restaurado de una copia. Devuelve false si el nombre no es seguro. */
    fun write(name: String, bytes: ByteArray): Boolean {
        val file = fileOf(name) ?: return false
        file.writeBytes(bytes)
        return true
    }

    fun exists(name: String): Boolean = fileOf(name)?.isFile == true

    /** Borra los archivos que ya no usa ningún adjunto (de notas borradas, adjuntos quitados…). */
    fun sweep(keep: Set<String>) {
        dir.listFiles()?.forEach { if (it.isFile && it.name !in keep) it.delete() }
    }
}
