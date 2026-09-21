package com.example.domain.tunnel

import android.content.Context

interface TunnelProvider {
    val providerId: String
    val displayName: String

    suspend fun initialize(context: Context): Boolean

    suspend fun createTunnel(
        serverId: String,
        serverName: String,
        localPort: Int,
        protocol: TunnelProtocol
    ): TunnelResult

    suspend fun deleteTunnel(providerTunnelId: String): Boolean

    suspend fun startTunnelSession(
        tunnelConfig: TunnelConfig,
        onStatusChanged: (TunnelStatus, String?) -> Unit
    ): Boolean

    suspend fun stopTunnelSession(providerTunnelId: String)

    suspend fun verifyTunnelEndpoint(
        publicHost: String,
        publicPort: Int
    ): Boolean
}
