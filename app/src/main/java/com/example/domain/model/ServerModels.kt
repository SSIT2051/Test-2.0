package com.example.domain.model

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    RESTARTING
}

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    DEBUG,
    CHAT
}

data class ServerConfig(
    val id: String,
    val name: String,
    val port: Int = 25565,
    val serverVersion: String = "26.3 (Latest 2026)",
    val motd: String = "A blazingly fast PumpkinMC Server in Rust",
    val maxPlayers: Int = 20,
    val gamemode: String = "survival",
    val difficulty: String = "normal",
    val pvp: Boolean = true,
    val onlineMode: Boolean = true,
    val allocatedRamMb: Int = 1024,
    val viewDistance: Int = 10,
    val simulationDistance: Int = 8,
    val workerThreads: Int = 4, // Tokio worker async network threads
    val rayonThreads: Int = 4,  // Rayon parallel chunk calculation threads
    val maxStorageMb: Int = 2048,
    val autoSaveMinutes: Int = 5,
    val allowFlight: Boolean = false,
    val hardcore: Boolean = false,
    val lanModeEnabled: Boolean = true,
    val bedrockCrossplayEnabled: Boolean = true,
    val bedrockPort: Int = 19132,
    val playitEnabled: Boolean = true,
    val playitDomain: String = "playit-free.gl.joinmc.link",
    val playitPort: Int = 19132,
    val customTunnelEnabled: Boolean = false,
    val customTunnelType: String = "Custom",
    val customTunnelAddress: String = "",
    val customPort: Int = 19132,
    val status: ServerStatus = ServerStatus.STOPPED,
    val createdAt: Long = System.currentTimeMillis()
)

data class LiveServerMetrics(
    val cpuPercent: Float = 0f,
    val usedRamMb: Float = 0f,
    val allocatedRamMb: Int = 1024,
    val tps: Float = 20.0f,
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 20,
    val uptimeSeconds: Long = 0,
    val loadedChunks: Int = 0,
    val pingMs: Int = 12
)

data class LogEntry(
    val id: Long = 0,
    val serverId: String,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class ServerFileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val content: String = ""
)

data class MarketPlugin(
    val id: String,
    val slug: String,
    val name: String,
    val description: String,
    val category: String, // Performance, Administration, Gameplay, Security, Economy, Tools, Essentials
    val author: String,
    val version: String,
    val mcVersion: String = "1.21.4",
    val rating: Float = 4.8f,
    val downloads: Int = 1240,
    val iconUrl: String = "",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val isMod: Boolean = false,
    val sizeText: String = "142 KB"
)
