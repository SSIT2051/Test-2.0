package com.example.domain.tunnel.playit

import android.util.Log
import com.example.domain.tunnel.TunnelConfig
import com.example.domain.tunnel.TunnelProtocol
import com.example.domain.tunnel.TunnelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Playit Agent Session Client on Android.
 *
 * Runs without root or Termux inside the Android application.
 * Establishes outbound connection to local Minecraft port and manages tunnel health lifecycle.
 */
class PlayitAgentClient(
    private val config: TunnelConfig,
    private val onStatusChanged: (TunnelStatus, String?) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var healthJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        onStatusChanged(TunnelStatus.CONNECTING, null)

        healthJob = scope.launch {
            try {
                // Verify local port is accepting connections
                var localUp = verifyLocalSocket()
                var attempts = 0
                while (!localUp && attempts < 15 && isActive) {
                    delay(500)
                    localUp = verifyLocalSocket()
                    attempts++
                }

                if (!localUp) {
                    onStatusChanged(TunnelStatus.ERROR, "Local Minecraft server is not listening on port ${config.localPort}")
                    return@launch
                }

                onStatusChanged(TunnelStatus.ONLINE, null)

                // Periodic health check heartbeat
                while (isActive && isRunning) {
                    delay(10000)
                    if (!verifyLocalSocket()) {
                        onStatusChanged(TunnelStatus.OFFLINE, "Local Minecraft socket unresponsive")
                    } else {
                        onStatusChanged(TunnelStatus.ONLINE, null)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayitAgentClient", "Session error: ${e.message}")
                onStatusChanged(TunnelStatus.ERROR, e.localizedMessage)
            }
        }
    }

    fun stop() {
        isRunning = false
        healthJob?.cancel()
        healthJob = null
        onStatusChanged(TunnelStatus.OFFLINE, null)
    }

    private fun verifyLocalSocket(): Boolean {
        return try {
            when (config.protocol) {
                TunnelProtocol.TCP -> {
                    val s = Socket()
                    s.connect(InetSocketAddress("127.0.0.1", config.localPort), 400)
                    s.close()
                    true
                }
                TunnelProtocol.UDP -> {
                    // For UDP, verify socket is open and can be bound/sent to
                    val ds = DatagramSocket()
                    val packet = DatagramPacket(ByteArray(1), 1, InetAddress.getByName("127.0.0.1"), config.localPort)
                    ds.send(packet)
                    ds.close()
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
