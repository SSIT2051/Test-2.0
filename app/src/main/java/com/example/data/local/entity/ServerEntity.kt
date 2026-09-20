package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ServerConfig
import com.example.domain.model.ServerStatus

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val port: Int,
    val serverVersion: String = "1.21.4 (Latest)",
    val motd: String,
    val maxPlayers: Int,
    val gamemode: String,
    val difficulty: String,
    val pvp: Boolean,
    val onlineMode: Boolean,
    val allocatedRamMb: Int,
    val viewDistance: Int,
    val simulationDistance: Int,
    val workerThreads: Int,
    val rayonThreads: Int = 4,
    val maxStorageMb: Int = 2048,
    val autoSaveMinutes: Int = 5,
    val allowFlight: Boolean = false,
    val hardcore: Boolean = false,
    val lanModeEnabled: Boolean,
    val bedrockCrossplayEnabled: Boolean = true,
    val bedrockPort: Int = 19132,
    val playitEnabled: Boolean,
    val playitDomain: String,
    val playitPort: Int = 19132,
    val customTunnelEnabled: Boolean,
    val customTunnelType: String,
    val customTunnelAddress: String,
    val customPort: Int = 19132,
    val status: String,
    val createdAt: Long
) {
    fun toDomain(): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            port = port,
            serverVersion = serverVersion,
            motd = motd,
            maxPlayers = maxPlayers,
            gamemode = gamemode,
            difficulty = difficulty,
            pvp = pvp,
            onlineMode = onlineMode,
            allocatedRamMb = allocatedRamMb,
            viewDistance = viewDistance,
            simulationDistance = simulationDistance,
            workerThreads = workerThreads,
            rayonThreads = rayonThreads,
            maxStorageMb = maxStorageMb,
            autoSaveMinutes = autoSaveMinutes,
            allowFlight = allowFlight,
            hardcore = hardcore,
            lanModeEnabled = lanModeEnabled,
            bedrockCrossplayEnabled = bedrockCrossplayEnabled,
            bedrockPort = bedrockPort,
            playitEnabled = playitEnabled,
            playitDomain = playitDomain,
            playitPort = playitPort,
            customTunnelEnabled = customTunnelEnabled,
            customTunnelType = customTunnelType,
            customTunnelAddress = customTunnelAddress,
            customPort = customPort,
            status = try { ServerStatus.valueOf(status) } catch (e: Exception) { ServerStatus.STOPPED },
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(config: ServerConfig): ServerEntity {
            return ServerEntity(
                id = config.id,
                name = config.name,
                port = config.port,
                serverVersion = config.serverVersion,
                motd = config.motd,
                maxPlayers = config.maxPlayers,
                gamemode = config.gamemode,
                difficulty = config.difficulty,
                pvp = config.pvp,
                onlineMode = config.onlineMode,
                allocatedRamMb = config.allocatedRamMb,
                viewDistance = config.viewDistance,
                simulationDistance = config.simulationDistance,
                workerThreads = config.workerThreads,
                rayonThreads = config.rayonThreads,
                maxStorageMb = config.maxStorageMb,
                autoSaveMinutes = config.autoSaveMinutes,
                allowFlight = config.allowFlight,
                hardcore = config.hardcore,
                lanModeEnabled = config.lanModeEnabled,
                bedrockCrossplayEnabled = config.bedrockCrossplayEnabled,
                bedrockPort = config.bedrockPort,
                playitEnabled = config.playitEnabled,
                playitDomain = config.playitDomain,
                playitPort = config.playitPort,
                customTunnelEnabled = config.customTunnelEnabled,
                customTunnelType = config.customTunnelType,
                customTunnelAddress = config.customTunnelAddress,
                customPort = config.customPort,
                status = config.status.name,
                createdAt = config.createdAt
            )
        }
    }
}
