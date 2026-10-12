package com.example.localcam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebcamStreamClient(
    private val scope: CoroutineScope,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    private val _state = MutableStateFlow<StreamConnectionState>(StreamConnectionState.Disconnected)
    val state: StateFlow<StreamConnectionState> = _state.asStateFlow()

    private val _frames = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val frames: SharedFlow<ByteArray> = _frames.asSharedFlow()

    private val _nonFatalErrors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val nonFatalErrors: SharedFlow<String> = _nonFatalErrors.asSharedFlow()

    private var activeSocket: WebSocket? = null
    private var retryJob: Job? = null
    private var desiredConfig: ConnectionConfig? = null
    private var autoRetryEnabled = false
    private var reconnectAttempt = 0

    fun connect(config: ConnectionConfig, isManualReconnect: Boolean = false) {
        desiredConfig = config
        autoRetryEnabled = true
        reconnectAttempt = if (isManualReconnect) 1 else 0
        retryJob?.cancel()
        activeSocket?.cancel()
        val endpoint = config.webSocketUrl()
        _state.value = if (isManualReconnect) {
            StreamConnectionState.Reconnecting(endpoint, reconnectAttempt, 0, "Manual reconnect requested")
        } else {
            StreamConnectionState.Connecting(endpoint)
        }
        openSocket(config)
    }

    fun disconnect() {
        autoRetryEnabled = false
        desiredConfig = null
        reconnectAttempt = 0
        retryJob?.cancel()
        retryJob = null
        activeSocket?.close(1000, "User disconnected")
        activeSocket = null
        _state.value = StreamConnectionState.Disconnected
    }

    fun release() {
        disconnect()
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }

    private fun openSocket(config: ConnectionConfig) {
        val request = Request.Builder().url(config.webSocketUrl()).build()
        val socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (webSocket !== activeSocket) return
                reconnectAttempt = 0
                _state.value = StreamConnectionState.Connected(config.webSocketUrl())
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (webSocket !== activeSocket) return
                val payload = bytes.toByteArray()
                val validationError = FrameValidator.validateJpeg(payload)
                if (validationError != null) {
                    _nonFatalErrors.tryEmit(validationError)
                    return
                }
                if (!_frames.tryEmit(payload)) {
                    _nonFatalErrors.tryEmit("Frame dropped to keep preview responsive.")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== activeSocket) return
                _nonFatalErrors.tryEmit("Text message ignored; expected binary JPEG frames.")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== activeSocket) return
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== activeSocket) return
                activeSocket = null
                handleDisconnect("Disconnected (${code}) ${reason.ifBlank { "remote closed" }}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket !== activeSocket) return
                activeSocket = null
                val message = t.localizedMessage ?: "network failure"
                handleDisconnect("Connection failed: $message")
            }
        })
        activeSocket = socket
    }

    private fun handleDisconnect(reason: String) {
        val config = desiredConfig
        if (!autoRetryEnabled || config == null) {
            _state.value = StreamConnectionState.Failed(config?.webSocketUrl() ?: "", reason)
            return
        }

        reconnectAttempt += 1
        if (reconnectAttempt > retryPolicy.maxAttempts) {
            autoRetryEnabled = false
            _state.value = StreamConnectionState.Failed(
                endpoint = config.webSocketUrl(),
                error = "$reason (automatic retries exhausted)"
            )
            return
        }
        val delayMs = retryPolicy.delayForAttempt(reconnectAttempt)
        _state.update {
            StreamConnectionState.Reconnecting(
                endpoint = config.webSocketUrl(),
                attempt = reconnectAttempt,
                delayMs = delayMs,
                error = reason
            )
        }
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)
            if (!autoRetryEnabled) return@launch
            _state.value = StreamConnectionState.Connecting(config.webSocketUrl())
            openSocket(config)
        }
    }
}
