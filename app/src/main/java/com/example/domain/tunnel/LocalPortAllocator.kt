package com.example.domain.tunnel

import android.util.Log
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

object LocalPortAllocator {
    private const val TAG = "LocalPortAllocator"
    
    // Default starting ports
    const val JAVA_DEFAULT_START_PORT = 25565
    const val BEDROCK_DEFAULT_START_PORT = 19132
    
    private val allocatedTcpPorts = mutableSetOf<Int>()
    private val allocatedUdpPorts = mutableSetOf<Int>()

    @Synchronized
    fun registerAllocatedPorts(servers: List<com.example.domain.model.ServerConfig>) {
        allocatedTcpPorts.clear()
        allocatedUdpPorts.clear()
        for (server in servers) {
            allocatedTcpPorts.add(server.port)
            allocatedUdpPorts.add(server.bedrockPort)
        }
    }

    @Synchronized
    fun allocatePort(
        protocol: TunnelProtocol,
        preferredPort: Int? = null,
        occupiedPorts: Set<Int> = emptySet()
    ): Int {
        val startPort = preferredPort ?: when (protocol) {
            TunnelProtocol.TCP -> JAVA_DEFAULT_START_PORT
            TunnelProtocol.UDP -> BEDROCK_DEFAULT_START_PORT
        }

        val allocatedSet = when (protocol) {
            TunnelProtocol.TCP -> allocatedTcpPorts
            TunnelProtocol.UDP -> allocatedUdpPorts
        }

        // Try preferred port first
        if (!allocatedSet.contains(startPort) && !occupiedPorts.contains(startPort) && isPortAvailable(startPort, protocol)) {
            allocatedSet.add(startPort)
            return startPort
        }

        // Scan sequential ports up to +100
        for (candidate in startPort + 1..(startPort + 200)) {
            if (!allocatedSet.contains(candidate) && !occupiedPorts.contains(candidate)) {
                if (isPortAvailable(candidate, protocol)) {
                    allocatedSet.add(candidate)
                    return candidate
                }
            }
        }

        // Fallback random high port if standard range is occupied
        val highPort = (30000..45000).random()
        allocatedSet.add(highPort)
        return highPort
    }

    @Synchronized
    fun releasePort(port: Int, protocol: TunnelProtocol) {
        when (protocol) {
            TunnelProtocol.TCP -> allocatedTcpPorts.remove(port)
            TunnelProtocol.UDP -> allocatedUdpPorts.remove(port)
        }
    }

    fun isPortAvailable(port: Int, protocol: TunnelProtocol): Boolean {
        return when (protocol) {
            TunnelProtocol.TCP -> {
                try {
                    val ss = ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
                    ss.reuseAddress = true
                    ss.close()
                    true
                } catch (e: IOException) {
                    false
                }
            }
            TunnelProtocol.UDP -> {
                try {
                    val ds = DatagramSocket(port, InetAddress.getByName("127.0.0.1"))
                    ds.reuseAddress = true
                    ds.close()
                    true
                } catch (e: IOException) {
                    false
                }
            }
        }
    }
}
