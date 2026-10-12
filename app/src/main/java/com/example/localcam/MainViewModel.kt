package com.example.localcam

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val host: String = "",
    val port: String = ConnectionConfig.DEFAULT_PORT.toString(),
    val status: String = "Disconnected",
    val isBusy: Boolean = false,
    val canConnect: Boolean = true,
    val canDisconnect: Boolean = false,
    val canReconnect: Boolean = false,
    val error: String? = null,
    val hasPreviewFrame: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = ReceiverPreferences(application)
    private val streamClient = WebcamStreamClient(viewModelScope)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    init {
        val saved = prefs.load()
        _uiState.value = _uiState.value.copy(
            host = saved.host,
            port = saved.port.toString(),
            status = "Disconnected"
        )

        viewModelScope.launch {
            streamClient.state.collectLatest { state ->
                _uiState.update { current -> current.reduceForState(state) }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            streamClient.frames.collectLatest { frame ->
                val bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.size)
                if (bitmap == null) {
                    _uiState.update { it.copy(error = "Failed to decode JPEG frame.") }
                    return@collectLatest
                }
                _previewBitmap.value = bitmap
                _uiState.update { it.copy(hasPreviewFrame = true) }
            }
        }

        viewModelScope.launch {
            streamClient.nonFatalErrors.collectLatest { error ->
                _uiState.update { it.copy(error = error) }
            }
        }
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(host = host, error = null) }
    }

    fun onPortChanged(port: String) {
        _uiState.update { it.copy(port = port, error = null) }
    }

    fun connect() {
        val state = _uiState.value
        val parsed = ConnectionConfig.parse(state.host, state.port)
        parsed.onFailure { error ->
            _uiState.update { it.copy(error = error.message, status = "Invalid connection settings") }
            return
        }

        val config = parsed.getOrThrow()
        prefs.save(config)
        _uiState.update { it.copy(error = null, hasPreviewFrame = false) }
        streamClient.connect(config)
    }

    fun disconnect() {
        streamClient.disconnect()
    }

    fun reconnect() {
        val state = _uiState.value
        val parsed = ConnectionConfig.parse(state.host, state.port)
        parsed.onFailure { error ->
            _uiState.update { it.copy(error = error.message) }
            return
        }

        val config = parsed.getOrThrow()
        prefs.save(config)
        _uiState.update { it.copy(error = null, hasPreviewFrame = false) }
        streamClient.connect(config, isManualReconnect = true)
    }

    override fun onCleared() {
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        streamClient.release()
        super.onCleared()
    }

    private fun MainUiState.reduceForState(connectionState: StreamConnectionState): MainUiState {
        return when (connectionState) {
            StreamConnectionState.Disconnected -> copy(
                status = "Disconnected",
                isBusy = false,
                canConnect = true,
                canDisconnect = false,
                canReconnect = true
            )

            is StreamConnectionState.Connecting -> copy(
                status = "Connecting to ${connectionState.endpoint}",
                isBusy = true,
                canConnect = false,
                canDisconnect = true,
                canReconnect = false
            )

            is StreamConnectionState.Connected -> copy(
                status = "Connected: ${connectionState.endpoint}",
                isBusy = false,
                canConnect = false,
                canDisconnect = true,
                canReconnect = true,
                error = null
            )

            is StreamConnectionState.Reconnecting -> copy(
                status = "Reconnecting (attempt ${connectionState.attempt}) in ${connectionState.delayMs / 1000.0}s",
                isBusy = true,
                canConnect = false,
                canDisconnect = true,
                canReconnect = false,
                error = connectionState.error
            )

            is StreamConnectionState.Failed -> copy(
                status = if (connectionState.endpoint.isBlank()) "Connection failed" else "Connection failed: ${connectionState.endpoint}",
                isBusy = false,
                canConnect = true,
                canDisconnect = false,
                canReconnect = true,
                error = connectionState.error
            )
        }
    }
}
