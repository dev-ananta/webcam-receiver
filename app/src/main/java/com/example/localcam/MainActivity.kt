package com.example.localcam

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.localcam.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener { viewModel.connect() }
        binding.disconnectButton.setOnClickListener { viewModel.disconnect() }
        binding.reconnectButton.setOnClickListener { viewModel.reconnect() }

        binding.ipInput.doAfterTextChanged { editable ->
            viewModel.onHostChanged(editable?.toString().orEmpty())
        }
        binding.portInput.doAfterTextChanged { editable ->
            viewModel.onPortChanged(editable?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (binding.ipInput.text?.toString() != state.host) {
                            binding.ipInput.setText(state.host)
                            binding.ipInput.setSelection(state.host.length)
                        }
                        if (binding.portInput.text?.toString() != state.port) {
                            binding.portInput.setText(state.port)
                            binding.portInput.setSelection(state.port.length)
                        }

                        binding.statusText.text = state.status
                        binding.progressBar.visibility = if (state.isBusy) View.VISIBLE else View.GONE
                        binding.connectButton.isEnabled = state.canConnect
                        binding.disconnectButton.isEnabled = state.canDisconnect
                        binding.reconnectButton.isEnabled = state.canReconnect

                        val error = state.error
                        binding.errorText.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
                        binding.errorText.text = error.orEmpty()
                        binding.previewPlaceholder.visibility = if (state.hasPreviewFrame) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.previewBitmap.collect { bitmap ->
                        if (bitmap == null) {
                            binding.previewImage.setImageDrawable(null)
                            currentBitmap?.recycle()
                            currentBitmap = null
                            return@collect
                        }
                        if (currentBitmap !== bitmap) {
                            binding.previewImage.setImageBitmap(bitmap)
                            currentBitmap?.recycle()
                            currentBitmap = bitmap
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        binding.previewImage.setImageDrawable(null)
        super.onDestroy()
    }
}
