package io.github.estebangocoalt.blocdenotas.sync

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Error de sincronización con el motivo para mostrarlo al usuario. */
class SyncException(val reason: Reason, cause: Throwable? = null) : IOException(reason.name, cause) {
    enum class Reason { BAD_URL, UNAUTHORIZED, NOT_FOUND, SERVER, NETWORK, CONFLICT, INVALID_REMOTE, NOT_CONFIGURED }
}

class RemoteFile(val bytes: ByteArray, val etag: String?)

/**
 * Cliente WebDAV mínimo (GET y PUT de un solo archivo con autenticación básica).
 * Usa `HttpURLConnection`, sin librerías de terceros.
 */
class WebDavClient(
    private val config: SyncConfig,
    private val allowInsecure: Boolean = false,
    private val timeoutMs: Int = 30_000,
) {
    private val fileUrl = SyncLogic.fileUrl(config.url)

    init {
        if (!SyncLogic.isUrlAllowed(config.url, allowInsecure)) throw SyncException(SyncException.Reason.BAD_URL)
    }

    private fun open(method: String): HttpURLConnection {
        val conn = URL(fileUrl).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Authorization", SyncLogic.basicAuth(config.user, config.password))
        return conn
    }

    private fun fail(code: Int): Nothing = throw SyncException(
        when (code) {
            401, 403 -> SyncException.Reason.UNAUTHORIZED
            404, 409 -> SyncException.Reason.NOT_FOUND
            412 -> SyncException.Reason.CONFLICT
            else -> SyncException.Reason.SERVER
        },
    )

    /** Descarga el archivo remoto; null si todavía no existe. */
    fun get(): RemoteFile? {
        val conn = open("GET")
        try {
            return when (val code = conn.responseCode) {
                200 -> RemoteFile(conn.inputStream.use { it.readBytes() }, conn.getHeaderField("ETag"))
                404 -> null
                else -> fail(code)
            }
        } catch (e: SyncException) {
            throw e
        } catch (e: IOException) {
            throw SyncException(SyncException.Reason.NETWORK, e)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Sube el archivo. Si [etag] no es null solo se sustituye esa versión (If-Match); si es null,
     * solo se crea si no existe (If-None-Match). Lanza CONFLICT si otro dispositivo subió algo mientras tanto.
     * Devuelve el ETag nuevo si el servidor lo envía.
     */
    fun put(bytes: ByteArray, etag: String?): String? {
        val conn = open("PUT")
        try {
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.setRequestProperty("Content-Type", "application/zip")
            if (etag != null) conn.setRequestProperty("If-Match", etag) else conn.setRequestProperty("If-None-Match", "*")
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            if (code !in 200..204) fail(code)
            return conn.getHeaderField("ETag")
        } catch (e: SyncException) {
            throw e
        } catch (e: IOException) {
            throw SyncException(SyncException.Reason.NETWORK, e)
        } finally {
            conn.disconnect()
        }
    }
}
