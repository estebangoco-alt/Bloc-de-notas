package io.github.estebangocoalt.blocdenotas.sync

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Servidor HTTP mínimo hecho con sockets (no usa com.sun.net.httpserver, que no existe en todos los
 * entornos de compilación de pruebas de Android).
 */
class WebDavClientTest {
    private class Request(val method: String, val path: String, val headers: Map<String, String>, val body: ByteArray)

    private lateinit var server: ServerSocket
    private var worker: Thread? = null
    private var stored: ByteArray? = null
    private var etag = 0
    private var lastAuth: String? = null

    private val path = "/remote.php/dav/files/ana/Notas/" + SyncLogic.FILE_NAME

    @Before fun start() {
        server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        worker = Thread {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (e: Exception) { break }
                try { serve(socket) } catch (e: Exception) { /* la prueba ya fallará por su cuenta */ } finally { socket.close() }
            }
        }.apply { isDaemon = true; start() }
    }

    @After fun stop() {
        server.close()
        worker?.join(2000)
    }

    private fun readLine(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (out.size() == 0) null else out.toString("ISO-8859-1")
            if (b == '\n'.code) return out.toString("ISO-8859-1").trimEnd('\r')
            out.write(b)
        }
    }

    private fun readRequest(input: InputStream): Request? {
        val first = readLine(input) ?: return null
        val parts = first.split(" ")
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val i = line.indexOf(':')
            if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
        }
        val body = ByteArrayOutputStream()
        val length = headers["content-length"]?.toIntOrNull()
        if (length != null) {
            repeat(length) { body.write(input.read()) }
        } else if (headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true) {
            while (true) {
                val size = readLine(input)?.substringBefore(';')?.trim()?.toIntOrNull(16) ?: break
                if (size == 0) { readLine(input); break }
                repeat(size) { body.write(input.read()) }
                readLine(input)
            }
        }
        return Request(parts[0], parts.getOrElse(1) { "/" }.substringBefore('?'), headers, body.toByteArray())
    }

    private fun serve(socket: Socket) {
        val request = readRequest(socket.getInputStream()) ?: return
        val (code, body) = handle(request)
        val out = socket.getOutputStream()
        val head = StringBuilder("HTTP/1.1 $code X\r\n")
        if (code == 200 || code == 201 || code == 204) head.append("ETag: \"v$etag\"\r\n")
        head.append("Content-Length: ${body.size}\r\nConnection: close\r\n\r\n")
        out.write(head.toString().toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }

    private fun handle(request: Request): Pair<Int, ByteArray> {
        lastAuth = request.headers["authorization"]
        if (request.path != path) return 404 to ByteArray(0)
        if (lastAuth != SyncLogic.basicAuth("ana", "clave")) return 401 to ByteArray(0)
        return when (request.method) {
            "GET" -> stored?.let { 200 to it } ?: (404 to ByteArray(0))
            "PUT" -> {
                val ifMatch = request.headers["if-match"]
                val ifNone = request.headers["if-none-match"]
                when {
                    ifMatch != null && ifMatch != "\"v$etag\"" -> 412 to ByteArray(0)
                    ifNone == "*" && stored != null -> 412 to ByteArray(0)
                    else -> {
                        stored = request.body
                        etag++
                        201 to ByteArray(0)
                    }
                }
            }
            else -> 405 to ByteArray(0)
        }
    }

    private fun client(password: String = "clave") = WebDavClient(
        SyncConfig("http://127.0.0.1:${server.localPort}/remote.php/dav/files/ana/Notas/", "ana", password),
        allowInsecure = true,
    )

    @Test fun getReturnsNullWhenNothingUploadedYet() {
        assertNull(client().get())
    }

    @Test fun putThenGetRoundTrips() {
        val c = client()
        val newTag = c.put(byteArrayOf(1, 2, 3), null)
        val remote = c.get()!!
        assertArrayEquals(byteArrayOf(1, 2, 3), remote.bytes)
        assertEquals(newTag, remote.etag)
    }

    @Test fun createOnlyIfMissingConflictsWhenItExists() {
        val c = client()
        c.put(byteArrayOf(1), null)
        val e = runCatching { c.put(byteArrayOf(2), null) }.exceptionOrNull() as SyncException
        assertEquals(SyncException.Reason.CONFLICT, e.reason)
    }

    @Test fun staleEtagConflicts() {
        val c = client()
        c.put(byteArrayOf(1), null)
        val first = c.get()!!.etag
        c.put(byteArrayOf(2), first)
        val e = runCatching { c.put(byteArrayOf(3), first) }.exceptionOrNull() as SyncException
        assertEquals(SyncException.Reason.CONFLICT, e.reason)
    }

    @Test fun wrongPasswordIsUnauthorized() {
        val e = runCatching { client("mala").get() }.exceptionOrNull() as SyncException
        assertEquals(SyncException.Reason.UNAUTHORIZED, e.reason)
    }

    @Test fun httpIsRejectedUnlessAllowed() {
        assertFalse(SyncLogic.isUrlAllowed("http://example.com/dav"))
        assertTrue(SyncLogic.isUrlAllowed("https://example.com/dav/"))
        assertTrue(SyncLogic.isUrlAllowed("http://127.0.0.1:80/dav", allowInsecure = true))
        assertFalse(SyncLogic.isUrlAllowed("ftp://example.com"))
        assertFalse(SyncLogic.isUrlAllowed("no es una url"))
        val e = runCatching { WebDavClient(SyncConfig("http://example.com/dav", "a", "b")) }.exceptionOrNull() as SyncException
        assertEquals(SyncException.Reason.BAD_URL, e.reason)
    }

    @Test fun urlHelpers() {
        assertEquals("https://x.org/dav", SyncLogic.normalizeBase("  https://x.org/dav// "))
        assertEquals("https://x.org/dav/" + SyncLogic.FILE_NAME, SyncLogic.fileUrl("https://x.org/dav/"))
        assertEquals("Basic YW5hOmNsYXZl", SyncLogic.basicAuth("ana", "clave"))
    }
}
