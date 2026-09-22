package com.example.domain.tunnel

import android.content.Context
import android.util.Log
import com.example.data.local.dao.TunnelDao
import com.example.data.local.entity.TunnelEntity
import com.example.domain.model.ServerConfig
import com.example.domain.tunnel.playit.PlayitTunnelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TunnelManager(
    private val context: Context,
    private val tunnelDao: TunnelDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val providers = ConcurrentHashMap<String, TunnelProvider>()

    private val playitProvider = PlayitTunnelProvider()

    init {
        // Register supported providers
        registerProvider(com.example.domain.tunnel.gateway.GatewayTunnelProvider())
        registerProvider(playitProvider)
    }

    suspend fun startPlayitClaim(): com.example.domain.tunnel.playit.PlayitClaimInfo? {
        playitProvider.initialize(context)
        return playitProvider.startClaimFlow()
    }

    suspend fun checkPlayitClaimExchange(code: String): String? {
        playitProvider.initialize(context)
        return playitProvider.checkClaimExchange(code)
    }

    suspend fun getPlayitClaimUrl(): String? {
        playitProvider.initialize(context)
        val claim = playitProvider.startClaimFlow()
        return claim?.claimUrl
    }

    suspend fun getPlayitSecretKey(): String {
        playitProvider.initialize(context)
        return playitProvider.getSavedSecretKey()
    }

    suspend fun savePlayitSecretKey(key: String) {
        playitProvider.initialize(context)
        playitProvider.saveSecretKey(key)
    }

    suspend fun clearPlayitAccount() {
        playitProvider.initialize(context)
        playitProvider.clearSecretKey()
    }

    fun registerProvider(provider: TunnelProvider) {
        providers[provider.providerId] = provider
    }

    fun observeTunnelForServer(serverId: String): Flow<TunnelConfig?> {
        return tunnelDao.observeTunnelForServer(serverId).map { it?.toDomain() }
    }

    suspend fun getTunnelForServer(serverId: String): TunnelConfig? {
        return tunnelDao.getTunnelForServer(serverId)?.toDomain()
    }

    suspend fun createAndStartTunnel(
        server: ServerConfig,
        providerId: String = "playit"
    ): TunnelConfig = withContext(Dispatchers.IO) {
        val provider = providers[providerId] ?: providers["playit"]!!
        provider.initialize(context)

        val protocol = if (server.bedrockCrossplayEnabled) TunnelProtocol.UDP else TunnelProtocol.TCP
        val localPort = if (protocol == TunnelProtocol.UDP) server.bedrockPort else server.port

        // Check if tunnel already exists in DB for this server
        val existing = tunnelDao.getTunnelForServer(server.id)?.toDomain()
        if (existing != null && existing.isOnline) {
            return@withContext existing
        }

        // Record Initial Tunnel in State CREATING
        val tunnelId = existing?.id ?: UUID.randomUUID().toString()
        val initialConfig = TunnelConfig(
            id = tunnelId,
            serverId = server.id,
            provider = provider.providerId,
            localPort = localPort,
            protocol = protocol,
            status = TunnelStatus.CREATING
        )
        tunnelDao.insertOrUpdateTunnel(TunnelEntity.fromDomain(initialConfig))

        // Call Provider to create tunnel allocation
        val result = provider.createTunnel(
            serverId = server.id,
            serverName = server.name,
            localPort = localPort,
            protocol = protocol
        )

        val updatedConfig = when (result) {
            is TunnelResult.Success -> {
                TunnelConfig(
                    id = tunnelId,
                    serverId = server.id,
                    provider = provider.providerId,
                    providerTunnelId = result.providerTunnelId,
                    localPort = localPort,
                    protocol = result.protocol,
                    publicHost = result.publicHost,
                    publicPort = result.publicPort,
                    status = TunnelStatus.STARTING
                )
            }
            is TunnelResult.Failure -> {
                // If public cloud provider cannot provision, fallback to clean LAN status without fake endpoints
                TunnelConfig(
                    id = tunnelId,
                    serverId = server.id,
                    provider = provider.providerId,
                    localPort = localPort,
                    protocol = protocol,
                    publicHost = "",
                    publicPort = 0,
                    status = TunnelStatus.OFFLINE,
                    errorMessage = result.errorMessage
                )
            }
        }
        tunnelDao.insertOrUpdateTunnel(TunnelEntity.fromDomain(updatedConfig))

        if (updatedConfig.publicHost.isNotBlank()) {
            provider.startTunnelSession(updatedConfig) { status, error ->
                scope.launch {
                    tunnelDao.updateTunnelStatus(server.id, status.name, error)
                }
            }
        }

        updatedConfig
    }

    suspend fun stopTunnel(serverId: String) = withContext(Dispatchers.IO) {
        val tunnel = tunnelDao.getTunnelForServer(serverId)?.toDomain() ?: return@withContext
        val provider = providers[tunnel.provider]
        if (provider != null && tunnel.providerTunnelId.isNotBlank()) {
            provider.stopTunnelSession(tunnel.providerTunnelId)
        }
        tunnelDao.updateTunnelStatus(serverId, TunnelStatus.OFFLINE.name, null)
    }

    suspend fun deleteTunnel(serverId: String) = withContext(Dispatchers.IO) {
        val tunnel = tunnelDao.getTunnelForServer(serverId)?.toDomain()
        if (tunnel != null) {
            val provider = providers[tunnel.provider]
            if (provider != null && tunnel.providerTunnelId.isNotBlank()) {
                provider.deleteTunnel(tunnel.providerTunnelId)
            }
            tunnelDao.deleteTunnelForServer(serverId)
            LocalPortAllocator.releasePort(tunnel.localPort, tunnel.protocol)
        }
    }
}
