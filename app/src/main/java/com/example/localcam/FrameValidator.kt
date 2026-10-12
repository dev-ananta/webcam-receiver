package com.example.localcam

object FrameValidator {
    const val MAX_FRAME_BYTES: Int = 2_500_000

    fun validateJpeg(frame: ByteArray): String? {
        if (frame.isEmpty()) return "Empty frame received."
        if (frame.size > MAX_FRAME_BYTES) return "Frame too large (${frame.size} bytes)."
        if (frame.size < 4) return "Frame too small to decode."
        val startsWithJpeg = frame[0] == 0xFF.toByte() && frame[1] == 0xD8.toByte()
        val endsWithJpeg = frame[frame.lastIndex - 1] == 0xFF.toByte() && frame[frame.lastIndex] == 0xD9.toByte()
        if (!startsWithJpeg || !endsWithJpeg) return "Malformed JPEG frame received."
        return null
    }
}
