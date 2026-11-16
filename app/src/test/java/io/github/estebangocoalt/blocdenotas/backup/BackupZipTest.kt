package io.github.estebangocoalt.blocdenotas.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupZipTest {

    @Test
    fun packThenUnpackReturnsTheSameBytes() {
        val data = "hola ñandú ✓".toByteArray(Charsets.UTF_8)
        val zip = BackupZip.pack(BACKUP_ENTRY, data)
        assertTrue(BackupZip.isZip(zip))
        assertArrayEquals(data, BackupZip.unpackEntry(zip, BACKUP_ENTRY))
    }

    @Test
    fun plainJsonIsNotDetectedAsZip() {
        assertFalse(BackupZip.isZip("{}".toByteArray()))
        assertFalse(BackupZip.isZip(ByteArray(0)))
    }

    @Test
    fun missingEntryIsNotABackup() {
        val zip = BackupZip.pack("otro.txt", "x".toByteArray())
        try {
            BackupZip.unpackEntry(zip, BACKUP_ENTRY)
            fail("Debía lanzar BackupException")
        } catch (e: BackupException) {
            assertEquals(BackupException.Reason.NOT_A_BACKUP, e.reason)
        }
    }

    @Test
    fun entryWithPathTricksIsNotAccepted() {
        val zip = BackupZip.pack("../backup.json", "x".toByteArray())
        try {
            BackupZip.unpackEntry(zip, BACKUP_ENTRY)
            fail("Debía lanzar BackupException")
        } catch (e: BackupException) {
            assertEquals(BackupException.Reason.NOT_A_BACKUP, e.reason)
        }
    }

    @Test
    fun readLimitedRejectsOversizedInput() {
        val input = ByteArrayInputStream(ByteArray(100))
        try {
            BackupZip.readLimited(input, max = 50)
            fail("Debía lanzar BackupException")
        } catch (e: BackupException) {
            assertEquals(BackupException.Reason.TOO_LARGE, e.reason)
        }
    }

    @Test
    fun readLimitedAcceptsInputAtTheLimit() {
        val input = ByteArrayInputStream(ByteArray(50) { 7 })
        assertEquals(50, BackupZip.readLimited(input, max = 50).size)
    }
}
