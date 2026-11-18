package io.github.estebangocoalt.blocdenotas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentLogicTest {
    @Test fun safeNames() {
        assertTrue(AttachmentLogic.isSafeName("a1b2-c3.jpg"))
        assertFalse(AttachmentLogic.isSafeName("../x.jpg"))
        assertFalse(AttachmentLogic.isSafeName("dir/x.jpg"))
        assertFalse(AttachmentLogic.isSafeName(".hidden"))
        assertFalse(AttachmentLogic.isSafeName("a..b"))
        assertFalse(AttachmentLogic.isSafeName(""))
        assertFalse(AttachmentLogic.isSafeName("x".repeat(101)))
    }

    @Test fun generatedNamesAreSafe() {
        AttachmentKind.entries.forEach {
            val name = AttachmentLogic.newFileName(it)
            assertTrue(AttachmentLogic.isSafeName(name))
            assertTrue(name.endsWith("." + it.extension))
        }
    }

    @Test fun scalesLongestSideDown() {
        assertEquals(1600 to 800, AttachmentLogic.scaledSize(4000, 2000))
        assertEquals(800 to 1600, AttachmentLogic.scaledSize(2000, 4000))
        assertEquals(1000 to 500, AttachmentLogic.scaledSize(1000, 500))
        assertEquals(1 to 1, AttachmentLogic.scaledSize(0, 10))
    }

    @Test fun formatsDuration() {
        assertEquals("0:00", AttachmentLogic.formatDuration(0))
        assertEquals("1:05", AttachmentLogic.formatDuration(65_400))
        assertEquals("0:00", AttachmentLogic.formatDuration(-5))
    }

    @Test fun unknownKindIsNull() {
        assertEquals(null, AttachmentKind.fromName("VIDEO"))
        assertEquals(AttachmentKind.AUDIO, AttachmentKind.fromName("AUDIO"))
    }
}
