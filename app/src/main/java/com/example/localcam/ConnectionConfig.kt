package com.example.localcam

import java.util.Locale

data class ConnectionConfig(val host: String, val port: Int) {
    fun webSocketUrl(): String = "ws://$host:$port/"

    companion object {
        const val DEFAULT_PORT = 8765
        private val HOST_REGEX = Regex("^[a-zA-Z0-9.-]+$")

        fun parse(hostInput: String, portInput: String): Result<ConnectionConfig> {
            val host = hostInput.trim()
            if (host.isBlank()) {
                return Result.failure(IllegalArgumentException("Enter the Mac IP address or hostname."))
            }
            if (host.contains("://") || host.contains("/") || !HOST_REGEX.matches(host)) {
                return Result.failure(IllegalArgumentException("Host format is invalid."))
            }

            val portText = portInput.trim().ifEmpty { DEFAULT_PORT.toString() }
            val port = portText.toIntOrNull()
                ?: return Result.failure(IllegalArgumentException("Port must be a number between 1 and 65535."))
            if (port !in 1..65535) {
                return Result.failure(IllegalArgumentException("Port must be a number between 1 and 65535."))
            }

            return Result.success(ConnectionConfig(host.lowercase(Locale.US), port))
        }
    }
}
