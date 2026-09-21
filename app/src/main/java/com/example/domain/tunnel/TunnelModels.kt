package com.example.domain.tunnel

enum class TunnelProtocol {
    TCP,
    UDP
}

enum class TunnelStatus {
    IDLE,
    CREATING,
    STARTING,
    CONNECTING,
    ONLINE,
    OFFLINE,
    ERROR
}

data class TunnelConfig(
    val id: String,
    val serverId: String,
    val provider: String, // "playit", "custom", "lan"
    val providerTunnelId: String = "",
    val providerAgentId: String = "",
    val localHost: String = "127.0.0.1",
    val localPort: Int,
    val protocol: TunnelProtocol,
    val publicHost: String = "",
    val publicPort: Int = 0,
    val status: TunnelStatus = TunnelStatus.IDLE,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayEndpoint: String
        get() = if (publicHost.isNotBlank() && publicPort > 0) {
            "$publicHost:$publicPort"
        } else if (publicHost.isNotBlank()) {
            publicHost
        } else {
            ""
        }

    val isOnline: Boolean
        get() = status == TunnelStatus.ONLINE && publicHost.isNotBlank()
}

sealed class TunnelResult {
    data class Success(
        val providerTunnelId: String,
        val publicHost: String,
        val publicPort: Int,
        val protocol: TunnelProtocol
    ) : TunnelResult()

    data class Failure(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : TunnelResult()
}
