package io.github.estebangocoalt.blocdenotas.sync

import java.net.URI
import java.util.Base64

/** Datos de conexión al servidor WebDAV (por ejemplo, una carpeta de Nextcloud). */
data class SyncConfig(val url: String, val user: String, val password: String) {
    val isComplete: Boolean get() = url.isNotBlank() && user.isNotBlank() && password.isNotEmpty()
}

/** Reglas de la sincronización. Lógica pura (sin Android) para probarla con tests de JVM. */
object SyncLogic {
    const val FILE_NAME = "bloc-de-notas-sync.zip"

    /** Quita espacios y barras finales de la dirección de la carpeta. */
    fun normalizeBase(url: String): String = url.trim().trimEnd('/')

    /** Dirección del archivo de sincronización dentro de la carpeta. */
    fun fileUrl(base: String): String = normalizeBase(base) + "/" + FILE_NAME

    /** Solo se admite https (así la contraseña no viaja en claro); http únicamente si [allowInsecure]. */
    fun isUrlAllowed(url: String, allowInsecure: Boolean = false): Boolean {
        val uri = runCatching { URI(normalizeBase(url)) }.getOrNull() ?: return false
        if (uri.host.isNullOrBlank()) return false
        return when (uri.scheme?.lowercase()) {
            "https" -> true
            "http" -> allowInsecure
            else -> false
        }
    }

    fun basicAuth(user: String, password: String): String =
        "Basic " + Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8))
}
