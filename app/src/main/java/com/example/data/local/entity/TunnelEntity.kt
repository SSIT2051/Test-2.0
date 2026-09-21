package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.tunnel.TunnelConfig
import com.example.domain.tunnel.TunnelProtocol
import com.example.domain.tunnel.TunnelStatus

@Entity(
    tableName = "server_tunnels",
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["serverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["serverId"]),
        Index(value = ["providerTunnelId"])
    ]
)
data class TunnelEntity(
    @PrimaryKey
    val id: String,
    val serverId: String,
    val provider: String,
    val providerTunnelId: String,
    val providerAgentId: String,
    val localHost: String,
    val localPort: Int,
    val protocol: String,
    val publicHost: String,
    val publicPort: Int,
    val status: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): TunnelConfig {
        return TunnelConfig(
            id = id,
            serverId = serverId,
            provider = provider,
            providerTunnelId = providerTunnelId,
            providerAgentId = providerAgentId,
            localHost = localHost,
            localPort = localPort,
            protocol = try { TunnelProtocol.valueOf(protocol) } catch (_: Exception) { TunnelProtocol.UDP },
            publicHost = publicHost,
            publicPort = publicPort,
            status = try { TunnelStatus.valueOf(status) } catch (_: Exception) { TunnelStatus.IDLE },
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: TunnelConfig): TunnelEntity {
            return TunnelEntity(
                id = domain.id,
                serverId = domain.serverId,
                provider = domain.provider,
                providerTunnelId = domain.providerTunnelId,
                providerAgentId = domain.providerAgentId,
                localHost = domain.localHost,
                localPort = domain.localPort,
                protocol = domain.protocol.name,
                publicHost = domain.publicHost,
                publicPort = domain.publicPort,
                status = domain.status.name,
                errorMessage = domain.errorMessage,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}
