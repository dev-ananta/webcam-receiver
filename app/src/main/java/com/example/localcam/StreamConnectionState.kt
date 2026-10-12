package com.example.localcam

sealed interface StreamConnectionState {
    data object Disconnected : StreamConnectionState
    data class Connecting(val endpoint: String) : StreamConnectionState
    data class Connected(val endpoint: String) : StreamConnectionState
    data class Reconnecting(val endpoint: String, val attempt: Int, val delayMs: Long, val error: String) : StreamConnectionState
    data class Failed(val endpoint: String, val error: String) : StreamConnectionState
}
