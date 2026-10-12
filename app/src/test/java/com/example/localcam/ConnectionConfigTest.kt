package com.example.localcam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionConfigTest {
    @Test
    fun `parse accepts valid host and default port`() {
        val result = ConnectionConfig.parse("192.168.1.42", "")

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()
        assertEquals("192.168.1.42", config.host)
        assertEquals(ConnectionConfig.DEFAULT_PORT, config.port)
        assertEquals("ws://192.168.1.42:8765/", config.webSocketUrl())
    }

    @Test
    fun `parse rejects invalid host`() {
        val result = ConnectionConfig.parse("http://192.168.1.42", "8765")

        assertTrue(result.isFailure)
    }

    @Test
    fun `parse rejects out-of-range port`() {
        val result = ConnectionConfig.parse("my-mac.local", "70000")

        assertTrue(result.isFailure)
    }
}
