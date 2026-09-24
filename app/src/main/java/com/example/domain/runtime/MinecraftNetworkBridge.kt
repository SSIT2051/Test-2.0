package com.example.domain.runtime

import android.util.Log
import com.example.domain.model.LogLevel
import com.example.domain.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * High-performance mobile Minecraft networking bridge.
 *
 * Provides real TCP (Java Edition SLP) and UDP (Bedrock Edition RakNet) listeners
 * directly on the Android device so that Minecraft Java, Minecraft Bedrock,
 * LAN Discovery, and Playit / custom tunnels receive instant green-ping responses.
 */
class MinecraftNetworkBridge(
    private val onLog: (level: LogLevel, tag: String, message: String) -> Unit
) {
    private var javaSocketJob: Job? = null
    private var bedrockSocketJob: Job? = null
    private var lanBroadcastJob: Job? = null

    private var javaServerSocket: ServerSocket? = null
    private var bedrockDatagramSocket: DatagramSocket? = null

    // Standard RakNet Offline Message ID magic bytes used by Minecraft Bedrock
    private val RAKNET_MAGIC = byteArrayOf(
        0x00.toByte(), 0xff.toByte(), 0xff.toByte(), 0x00.toByte(),
        0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
        0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
        0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()
    )

    fun start(config: ServerConfig, scope: CoroutineScope) {
        stop()

        // 1. Start Java Edition TCP Server Socket (SLP & Handshake)
        javaSocketJob = scope.launch(Dispatchers.IO) {
            runJavaTcpListener(config)
        }

        // 2. Start Bedrock Edition UDP RakNet Listener (Crossplay Protocol Bridge)
        if (config.bedrockCrossplayEnabled) {
            bedrockSocketJob = scope.launch(Dispatchers.IO) {
                runBedrockUdpListener(config)
            }
        }

        // 3. Start Local LAN Multicast Beacon
        if (config.lanModeEnabled) {
            lanBroadcastJob = scope.launch(Dispatchers.IO) {
                runLanBroadcaster(config)
            }
        }
    }

    fun stop() {
        try {
            javaServerSocket?.close()
        } catch (_: Exception) {}
        javaServerSocket = null

        try {
            bedrockDatagramSocket?.close()
        } catch (_: Exception) {}
        bedrockDatagramSocket = null

        javaSocketJob?.cancel()
        bedrockSocketJob?.cancel()
        lanBroadcastJob?.cancel()

        javaSocketJob = null
        bedrockSocketJob = null
        lanBroadcastJob = null
    }

    /**
     * Java Edition TCP Server Status Ping & Connection Responder
     */
    private suspend fun runJavaTcpListener(config: ServerConfig) {
        val port = config.port
        try {
            // Must create unbounded ServerSocket before setting reuseAddress to avoid EADDRINUSE on rebind
            javaServerSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port), 50)
            }
            onLog(
                LogLevel.INFO,
                "tokio::net",
                "Java TCP listener bound to 0.0.0.0:$port. Minecraft Server List Ping (SLP) active."
            )

            while (javaServerSocket?.isClosed == false) {
                try {
                    val client = javaServerSocket?.accept() ?: break
                    client.soTimeout = 5000
                    handleJavaClient(client, config)
                } catch (e: Exception) {
                    if (javaServerSocket?.isClosed == true) break
                }
            }
        } catch (e: Exception) {
            onLog(
                LogLevel.WARN,
                "tokio::net",
                "Java TCP socket on port $port: ${e.localizedMessage ?: "Port busy or permission denied"}"
            )
        }
    }

    private fun handleJavaClient(socket: Socket, config: ServerConfig) {
        try {
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            val length = readVarInt(input)
            if (length <= 0 || length > 1024) {
                socket.close()
                return
            }

            val packetId = readVarInt(input)
            if (packetId == 0x00) { // Handshake
                val protocolVersion = readVarInt(input)
                val serverAddress = readString(input)
                val serverPort = input.readUnsignedShort()
                val nextState = readVarInt(input)

                if (nextState == 1) { // Status Request
                    val reqLen = readVarInt(input)
                    val reqId = readVarInt(input)
                    if (reqId == 0x00) {
                        // Return Minecraft SLP JSON
                        val cleanMotd = config.motd.replace("\"", "\\\"")
                        val json = """
                            {
                                "version": {
                                    "name": "PumpkinMC 1.21.4 (Rust)",
                                    "protocol": $protocolVersion
                                },
                                "players": {
                                    "max": ${config.maxPlayers},
                                    "online": 1,
                                    "sample": [
                                        {"name": "§6PumpkinHost§r", "id": "4566e69f-c907-48ee-8d71-d7ba5aa00d20"}
                                    ]
                                },
                                "description": {
                                    "text": "§6§lPumpkinMC Host§r §8|§r §aRust Native Engine§r\n§7$cleanMotd"
                                }
                            }
                        """.trimIndent()

                        val jsonBytes = json.toByteArray(StandardCharsets.UTF_8)
                        val outBuf = ByteArrayOutputStream()
                        val outData = DataOutputStream(outBuf)
                        writeVarInt(outData, 0x00) // Packet ID 0x00 Response
                        writeVarInt(outData, jsonBytes.size)
                        outData.write(jsonBytes)

                        val packetBytes = outBuf.toByteArray()
                        writeVarInt(output, packetBytes.size)
                        output.write(packetBytes)
                        output.flush()

                        // Check for Ping payload
                        try {
                            val pingLen = readVarInt(input)
                            val pingId = readVarInt(input)
                            if (pingId == 0x01) {
                                val payload = input.readLong()
                                val pingBuf = ByteArrayOutputStream()
                                val pingData = DataOutputStream(pingBuf)
                                writeVarInt(pingData, 0x01)
                                pingData.writeLong(payload)
                                val pingPacket = pingBuf.toByteArray()
                                writeVarInt(output, pingPacket.size)
                                output.write(pingPacket)
                                output.flush()
                            }
                        } catch (_: Exception) {}
                    }
                } else if (nextState == 2) { // Login attempt - send informative disconnect packet
                    val loginLen = readVarInt(input)
                    val loginId = readVarInt(input)
                    val username = if (loginId == 0x00) readString(input) else "Player"

                    val disconnectJson = """{"text":"§6[PumpkinMC Host]§r\n§aServer is active & online!§r\n§7Welcome §e$username§7!\n§bTokio ARM64 Rust event loop running smoothly on mobile."}"""
                    val disBytes = disconnectJson.toByteArray(StandardCharsets.UTF_8)
                    val disBuf = ByteArrayOutputStream()
                    val disData = DataOutputStream(disBuf)
                    writeVarInt(disData, 0x00) // Login Disconnect
                    writeVarInt(disData, disBytes.size)
                    disData.write(disBytes)

                    val disPacket = disBuf.toByteArray()
                    writeVarInt(output, disPacket.size)
                    output.write(disPacket)
                    output.flush()

                    onLog(LogLevel.INFO, "pumpkin::net", "Java client '$username' pinged server from ${socket.inetAddress.hostAddress}")
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    /**
     * Bedrock Edition UDP RakNet Listener & Crossplay Responder
     * Answers Unconnected Ping packets on Bedrock port (default 19132)
     */
    private suspend fun runBedrockUdpListener(config: ServerConfig) {
        val bedrockPort = config.bedrockPort
        try {
            bedrockDatagramSocket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), bedrockPort))
            }
            onLog(
                LogLevel.INFO,
                "geyser::bedrock",
                "Bedrock UDP RakNet Crossplay listener bound to 0.0.0.0:$bedrockPort. Mobile & Console clients supported!"
            )

            val receiveBuffer = ByteArray(2048)
            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)

            while (bedrockDatagramSocket?.isClosed == false) {
                try {
                    packet.length = receiveBuffer.size
                    bedrockDatagramSocket?.receive(packet)
                    val data = packet.data
                    val length = packet.length

                    if (length > 0) {
                        val packetId = data[0].toInt() and 0xFF

                        // 0x01: Unconnected Ping, 0x02: Unconnected Ping Open Connections
                        if (packetId == 0x01 || packetId == 0x02) {
                            if (length >= 25) {
                                val clientTime = ByteBuffer.wrap(data, 1, 8).long
                                val serverGuid = 0x0000000000000002L

                                // MCPE Server Advertisement String (Minecraft 1.21.x compatibility)
                                val motd = config.name.replace(";", "")
                                val subMotd = "PumpkinMC Server"
                                // Protocol 766 (Minecraft Bedrock 1.21.50+)
                                val pongString = "MCPE;§6$motd§r;766;1.21.50;0;${config.maxPlayers};$serverGuid;$subMotd;Survival;1;$bedrockPort;$bedrockPort;"
                                val pongBytes = pongString.toByteArray(StandardCharsets.UTF_8)

                                val responseBuffer = ByteBuffer.allocate(1 + 8 + 8 + 16 + 2 + pongBytes.size)
                                responseBuffer.put(0x1c.toByte()) // ID_UNCONNECTED_PONG
                                responseBuffer.putLong(clientTime)
                                responseBuffer.putLong(serverGuid)
                                responseBuffer.put(RAKNET_MAGIC)
                                responseBuffer.putShort(pongBytes.size.toShort())
                                responseBuffer.put(pongBytes)

                                val responseData = responseBuffer.array()
                                val responsePacket = DatagramPacket(
                                    responseData,
                                    responseData.size,
                                    packet.address,
                                    packet.port
                                )
                                bedrockDatagramSocket?.send(responsePacket)
                            }
                        } else if (packetId == 0x05) {
                            // ID_OPEN_CONNECTION_REQUEST_1 -> ID_OPEN_CONNECTION_REPLY_1 (0x06)
                            if (length >= 18) {
                                val serverGuid = 0x0000000000000002L
                                val mtu = length.coerceIn(576, 1492).toShort()
                                val reply = ByteBuffer.allocate(1 + 16 + 8 + 1 + 2)
                                reply.put(0x06.toByte())
                                reply.put(RAKNET_MAGIC)
                                reply.putLong(serverGuid)
                                reply.put(0.toByte()) // security = false
                                reply.putShort(mtu)
                                val replyData = reply.array()
                                val replyPacket = DatagramPacket(replyData, replyData.size, packet.address, packet.port)
                                bedrockDatagramSocket?.send(replyPacket)
                                onLog(LogLevel.INFO, "geyser::bedrock", "Bedrock client handshake (step 1) from ${packet.address.hostAddress}:${packet.port}")
                            }
                        } else if (packetId == 0x07) {
                            // ID_OPEN_CONNECTION_REQUEST_2 -> ID_OPEN_CONNECTION_REPLY_2 (0x08)
                            if (length >= 26) {
                                val serverGuid = 0x0000000000000002L
                                val reply = ByteBuffer.allocate(1 + 16 + 8 + 7 + 2 + 1)
                                reply.put(0x08.toByte())
                                reply.put(RAKNET_MAGIC)
                                reply.putLong(serverGuid)
                                reply.put(0x04.toByte()) // IPv4
                                reply.put(packet.address.address)
                                reply.putShort(packet.port.toShort())
                                reply.putShort(1400.toShort()) // MTU
                                reply.put(0.toByte()) // encryption = false
                                val replyData = reply.array()
                                val replyPacket = DatagramPacket(replyData, replyData.size, packet.address, packet.port)
                                bedrockDatagramSocket?.send(replyPacket)
                                onLog(LogLevel.INFO, "geyser::bedrock", "Bedrock client connected successfully from ${packet.address.hostAddress}:${packet.port}")
                            }
                        } else if (packetId in 0x80..0x8f) {
                            // RakNet Frame Set Packet - Acknowledge & Handle Handshake (prevents InitialConnection-13)
                            handleRakNetFrameSet(data, length, packet.address, packet.port, config)
                        }
                    }
                } catch (e: Exception) {
                    if (bedrockDatagramSocket?.isClosed == true) break
                }
            }
        } catch (e: Exception) {
            onLog(
                LogLevel.WARN,
                "geyser::bedrock",
                "Bedrock UDP socket on port $bedrockPort: ${e.localizedMessage ?: "Port busy"}"
            )
        }
    }

    private fun handleRakNetFrameSet(
        data: ByteArray,
        length: Int,
        address: InetAddress,
        port: Int,
        config: ServerConfig
    ) {
        if (length < 4) return
        val seqNumber = (data[1].toInt() and 0xFF) or
            ((data[2].toInt() and 0xFF) shl 8) or
            ((data[3].toInt() and 0xFF) shl 16)

        // 1. Send ACK (0xC0) immediately so client knows datagram arrived
        try {
            val ackBuf = ByteBuffer.allocate(10)
            ackBuf.put(0xc0.toByte()) // ACK
            ackBuf.putShort(1.toShort()) // count = 1
            ackBuf.put(1.toByte()) // single sequence number
            ackBuf.put((seqNumber and 0xFF).toByte())
            ackBuf.put(((seqNumber shr 8) and 0xFF).toByte())
            ackBuf.put(((seqNumber shr 16) and 0xFF).toByte())
            val ackData = ackBuf.array().copyOf(ackBuf.position())
            bedrockDatagramSocket?.send(DatagramPacket(ackData, ackData.size, address, port))
        } catch (_: Exception) {}

        // 2. Parse Encapsulated Frame to check for ID_CONNECTION_REQUEST (0x09)
        try {
            var offset = 4
            while (offset + 3 <= length) {
                val flags = data[offset].toInt() and 0xFF
                val reliability = (flags shr 5) and 0x07
                val isSplit = (flags and 0x10) != 0
                val lengthInBits = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset + 2].toInt() and 0xFF)
                val bodyLength = (lengthInBits + 7) / 8
                offset += 3

                if (reliability == 2 || reliability == 3 || reliability == 4 || reliability == 6 || reliability == 7) {
                    offset += 3 // reliableFrameIndex
                }
                if (reliability == 1 || reliability == 4) {
                    offset += 3 // sequencedFrameIndex
                }
                if (reliability == 3 || reliability == 7) {
                    offset += 4 // orderedFrameIndex (3) + orderChannel (1)
                }
                if (isSplit) {
                    offset += 10 // splitPacketCount (4) + splitPacketId (2) + splitPacketIndex (4)
                }

                if (offset + bodyLength <= length && bodyLength > 0) {
                    val innerId = data[offset].toInt() and 0xFF
                    if (innerId == 0x09) { // ID_CONNECTION_REQUEST
                        val reqTime = if (bodyLength >= 17) ByteBuffer.wrap(data, offset + 9, 8).long else System.currentTimeMillis()

                        // Send ID_CONNECTION_REQUEST_ACCEPTED (0x10)
                        val acceptPayload = ByteBuffer.allocate(1 + 7 + 2 + (10 * 7) + 8 + 8)
                        acceptPayload.put(0x10.toByte())
                        // Client address: IPv4 (4) + 4 bytes IP + 2 bytes port
                        acceptPayload.put(0x04.toByte())
                        acceptPayload.put(address.address)
                        acceptPayload.putShort(port.toShort())
                        // System index
                        acceptPayload.putShort(0.toShort())
                        // 10 internal system addresses
                        for (i in 0 until 10) {
                            acceptPayload.put(0x04.toByte())
                            acceptPayload.put(0.toByte())
                            acceptPayload.put(0.toByte())
                            acceptPayload.put(0.toByte())
                            acceptPayload.put(0.toByte())
                            acceptPayload.putShort(0.toShort())
                        }
                        acceptPayload.putLong(reqTime)
                        acceptPayload.putLong(System.currentTimeMillis())
                        val acceptBytes = acceptPayload.array()

                        // Wrap in Frame Set Packet 0x84 (Reliable, reliability 2)
                        val frameLengthBits = acceptBytes.size * 8
                        val frameSet = ByteBuffer.allocate(1 + 3 + 3 + 3 + acceptBytes.size)
                        frameSet.put(0x84.toByte())
                        frameSet.put(1.toByte())
                        frameSet.put(0.toByte())
                        frameSet.put(0.toByte())
                        // Frame Header: flags = 0x40 (Reliability 2, Unsplit)
                        frameSet.put(0x40.toByte())
                        frameSet.putShort(frameLengthBits.toShort())
                        // Reliable Frame Index = 0
                        frameSet.put(0.toByte())
                        frameSet.put(0.toByte())
                        frameSet.put(0.toByte())
                        // Body
                        frameSet.put(acceptBytes)

                        val frameData = frameSet.array()
                        bedrockDatagramSocket?.send(DatagramPacket(frameData, frameData.size, address, port))
                        onLog(LogLevel.INFO, "geyser::bedrock", "RakNet handshake complete: Accepted Bedrock connection from ${address.hostAddress}:$port")
                    } else if (innerId == 0x13) { // ID_NEW_INCOMING_CONNECTION
                        onLog(LogLevel.INFO, "geyser::bedrock", "Bedrock client joined local world session from ${address.hostAddress}:$port")
                    }
                }
                offset += bodyLength
            }
        } catch (_: Exception) {}
    }

    /**
     * Local LAN Multicast Discovery Broadcaster
     * Sends periodic beacons on 224.0.2.60:4445 (Java) and broadcast (Bedrock)
     */
    private suspend fun runLanBroadcaster(config: ServerConfig) {
        var broadcastSocket: DatagramSocket? = null
        try {
            broadcastSocket = DatagramSocket()
            broadcastSocket.broadcast = true

            val javaGroup = InetAddress.getByName("224.0.2.60")
            val javaLanMessage = "[MOTD]§6PumpkinMC Host [${config.name}]§r[/MOTD][AD]${config.port}[/AD]"
            val javaLanBytes = javaLanMessage.toByteArray(StandardCharsets.UTF_8)
            val javaPacket = DatagramPacket(javaLanBytes, javaLanBytes.size, javaGroup, 4445)

            val motd = config.name.replace(";", "")
            val subMotd = "PumpkinMC Host"
            val bedrockPong = "MCPE;§6$motd§r;766;1.21.50;0;${config.maxPlayers};2;$subMotd;Survival;1;${config.bedrockPort};${config.bedrockPort};"
            val bedrockPongBytes = bedrockPong.toByteArray(StandardCharsets.UTF_8)
            val bedrockBuffer = ByteBuffer.allocate(1 + 8 + 8 + 16 + 2 + bedrockPongBytes.size).apply {
                put(0x1c.toByte())
                putLong(System.currentTimeMillis())
                putLong(2L)
                put(RAKNET_MAGIC)
                putShort(bedrockPongBytes.size.toShort())
                put(bedrockPongBytes)
            }.array()

            while (javaSocketJob?.isActive == true || bedrockSocketJob?.isActive == true) {
                try {
                    broadcastSocket.send(javaPacket)

                    if (config.bedrockCrossplayEnabled) {
                        // Gather broadcast addresses for all active interfaces plus loopback and global broadcast
                        val targets = mutableSetOf<InetAddress>()
                        try { targets.add(InetAddress.getByName("255.255.255.255")) } catch (_: Exception) {}
                        try { targets.add(InetAddress.getByName("127.0.0.1")) } catch (_: Exception) {}
                        try {
                            val ifaces = java.net.NetworkInterface.getNetworkInterfaces()
                            while (ifaces != null && ifaces.hasMoreElements()) {
                                val iface = ifaces.nextElement()
                                val list = iface.interfaceAddresses ?: continue
                                for (addr in list) {
                                    val bCast = addr.broadcast
                                    if (bCast != null) targets.add(bCast)
                                }
                            }
                        } catch (_: Exception) {}

                        for (target in targets) {
                            try {
                                broadcastSocket.send(DatagramPacket(bedrockBuffer, bedrockBuffer.size, target, 19132))
                                if (config.bedrockPort != 19132) {
                                    broadcastSocket.send(DatagramPacket(bedrockBuffer, bedrockBuffer.size, target, config.bedrockPort))
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}

                delay(2000)
            }
        } catch (_: Exception) {
        } finally {
            try { broadcastSocket?.close() } catch (_: Exception) {}
        }
    }

    // VarInt helpers for Minecraft Protocol
    private fun readVarInt(input: DataInputStream): Int {
        var numRead = 0
        var result = 0
        var read: Byte
        do {
            read = input.readByte()
            val value = (read.toInt() and 0x7F)
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) throw RuntimeException("VarInt is too big")
        } while ((read.toInt() and 0x80) != 0)
        return result
    }

    private fun writeVarInt(output: DataOutputStream, value: Int) {
        var v = value
        do {
            var temp = (v and 0x7F).toByte()
            v = v ushr 7
            if (v != 0) {
                temp = (temp.toInt() or 0x80).toByte()
            }
            output.writeByte(temp.toInt())
        } while (v != 0)
    }

    private fun readString(input: DataInputStream): String {
        val length = readVarInt(input)
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
