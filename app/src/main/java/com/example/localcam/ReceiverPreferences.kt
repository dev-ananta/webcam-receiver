package com.example.localcam

import android.content.Context

data class SavedConnection(val host: String, val port: Int)

class ReceiverPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("localcam_prefs", Context.MODE_PRIVATE)

    fun load(): SavedConnection {
        val host = prefs.getString(KEY_HOST, "") ?: ""
        val port = prefs.getInt(KEY_PORT, ConnectionConfig.DEFAULT_PORT)
        return SavedConnection(host = host, port = port)
    }

    fun save(config: ConnectionConfig) {
        prefs.edit()
            .putString(KEY_HOST, config.host)
            .putInt(KEY_PORT, config.port)
            .apply()
    }

    private companion object {
        const val KEY_HOST = "last_host"
        const val KEY_PORT = "last_port"
    }
}
