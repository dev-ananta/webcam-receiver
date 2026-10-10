package com.example.localcam

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private lateinit var addressInput: EditText
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView
    private lateinit var preview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF111111.toInt())
        }

        val title = TextView(this).apply {
            text = "LocalCam"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "Receive your Mac webcam stream over your local network."
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
            setPadding(0, 4, 0, 18)
        }

        addressInput = EditText(this).apply {
            hint = "Mac IP address (e.g. 192.168.1.42)"
            singleLine = true
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9E9E9E.toInt())
            setPadding(14, 8, 14, 8)
            setBackgroundColor(0xFF292929.toInt())
            setText("192.168.1.42")
        }
        connectButton = Button(this).apply {
            text = "Connect"
            isAllCaps = false
            setOnClickListener { if (socket == null) connect() else disconnect() }
        }
        statusText = TextView(this).apply {
            text = "Not connected"
            textSize = 13f
            setTextColor(0xFFFFAB91.toInt())
            setPadding(0, 12, 0, 12)
        }
        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFF202020.toInt())
            contentDescription = "Incoming webcam video"
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(addressInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(connectButton)
        root.addView(statusText)
        root.addView(preview, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun connect() {
        val host = addressInput.text.toString().trim()
        if (host.isBlank() || host.contains("://") || host.contains("/")) {
            updateStatus("Enter the Mac's LAN IP address.")
            return
        }
        val request = Request.Builder().url("ws://$host:8765/").build()
        updateStatus("Connecting to $host:8765…")
        connectButton.isEnabled = false

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    connectButton.isEnabled = true
                    connectButton.text = "Disconnect"
                    updateStatus("Connected — waiting for video frames…")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return
                runOnUiThread {
                    preview.setImageBitmap(bitmap)
                    statusText.text = "Connected — receiving video"
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // The Mac sender is expected to send binary JPEG frames, not text messages.
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                resetConnection("Disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                resetConnection("Connection failed: ${t.localizedMessage ?: "check IP, Wi-Fi, and Mac firewall"}")
            }
        })
    }

    private fun disconnect() {
        socket?.close(1000, "User disconnected")
        socket = null
        connectButton.text = "Connect"
        connectButton.isEnabled = true
        updateStatus("Disconnected")
    }

    private fun resetConnection(message: String) {
        runOnUiThread {
            socket = null
            connectButton.text = "Connect"
            connectButton.isEnabled = true
            updateStatus(message)
        }
    }

    private fun updateStatus(message: String) {
        statusText.text = message
    }

    override fun onDestroy() {
        socket?.close(1000, "App closed")
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        super.onDestroy()
    }
}
