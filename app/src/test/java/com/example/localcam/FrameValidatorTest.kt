package com.example.localcam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameValidatorTest {
    @Test
    fun `valid jpeg markers pass validation`() {
        val frame = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0xFF.toByte(), 0xD9.toByte())

        assertNull(FrameValidator.validateJpeg(frame))
    }

    @Test
    fun `invalid markers fail validation`() {
        val frame = byteArrayOf(0x00, 0x11, 0x22, 0x33)

        assertEquals("Malformed JPEG frame received.", FrameValidator.validateJpeg(frame))
    }

    @Test
    fun `oversized frame is rejected`() {
        val huge = ByteArray(FrameValidator.MAX_FRAME_BYTES + 1)

        assertEquals(
            "Frame too large (${FrameValidator.MAX_FRAME_BYTES + 1} bytes).",
            FrameValidator.validateJpeg(huge)
        )
    }
}
