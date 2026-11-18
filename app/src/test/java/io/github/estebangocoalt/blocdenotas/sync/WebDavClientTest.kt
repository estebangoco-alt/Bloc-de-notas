package io.github.estebangocoalt.blocdenotas.sync

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class WebDavClientTest {
    private lateinit var server: HttpServer
    private var stored: ByteArray? = null
    private var etag = 0
    private var lastAuth: String? = null

    private val path = "/remote.php/dav/files/ana/Notas/" + SyncLogic.FILE_NAME

    @Before fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex -> handle(ex) }
        server.start()
    }

    @After fun stop() = server.stop(0)

    private fun reply(ex: HttpExchange, code: Int, body: ByteArray = ByteArray(0)) {
        if (code == 200 || code == 201 || code == 204) ex.responseHeaders.add("ETag", "\"v$etag\"")
        ex.sendResponseHeaders(code, if (body.isEmpty()) -1 else body.size.toLong())
        if (body.isNotEmpty()) ex.responseBody.use { it.write(body) } else ex.close()
    }

    private fun handle(ex: HttpExchange) {
        lastAuth = ex.requestHeaders.getFirst("Authorization")
        if (ex.requestURI.path != path) return reply(ex, 404)
        if (lastAuth != SyncLogic.basicAuth("ana", "clave")) return reply(ex, 401)
        when (ex.requestMethod) {
            "GET" -> stored?.let { reply(ex, 200, it) } ?: reply(ex, 404)
            "PUT" -> {
                val ifMatch = ex.requestHeaders.getFirst("If-Match")
                val ifNone = ex.requestHeaders.getFirst("If-None-Match")
                val body = ex.requestBody.readBytes()
                when {
                    ifMatch != null && ifMatch != "\"v$etag\"" -> reply(ex, 412)
                    ifNone == "*" && stored != null -> reply(ex, 412)
                    else -> {
                        stored = body
                        etag++
                        reply(ex, 201)
                    }
                }
            }
            else -> reply(ex, 405)
        }
    }

    private fun client(password: String = "clave") = WebDavClient(
        SyncConfig("http://127.0.0.1:${server.address.port}/remote.php/dav/files/ana/Notas/", "ana", password),
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
