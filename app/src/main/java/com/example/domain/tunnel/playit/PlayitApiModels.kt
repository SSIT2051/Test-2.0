package com.example.domain.tunnel.playit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayitClaimSetupRequest(
    @Json(name = "agent_type") val agentType: String = "self-managed",
    @Json(name = "version") val version: String = "0.15.26"
)

@JsonClass(generateAdapter = true)
data class PlayitClaimSetupResponse(
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: PlayitClaimSetupData? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class PlayitClaimSetupData(
    @Json(name = "code") val code: String,
    @Json(name = "secret_key") val secretKey: String? = null,
    @Json(name = "claim_url") val claimUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class PlayitCreateTunnelRequest(
    @Json(name = "name") val name: String,
    @Json(name = "tunnel_type") val tunnelType: String, // "minecraft-java" or "minecraft-bedrock"
    @Json(name = "port_type") val portType: String, // "tcp" or "udp"
    @Json(name = "port_count") val portCount: Int = 1,
    @Json(name = "local_ip") val localIp: String = "127.0.0.1",
    @Json(name = "local_port") val localPort: Int
)

@JsonClass(generateAdapter = true)
data class PlayitCreateTunnelResponse(
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: PlayitTunnelData? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class PlayitTunnelData(
    @Json(name = "id") val id: String,
    @Json(name = "ip_hostname") val ipHostname: String? = null,
    @Json(name = "assigned_domain") val assignedDomain: String? = null,
    @Json(name = "port") val port: Int? = null,
    @Json(name = "port_from") val portFrom: Int? = null,
    @Json(name = "port_to") val portTo: Int? = null,
    @Json(name = "tunnel_type") val tunnelType: String? = null,
    @Json(name = "enabled") val enabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PlayitDeleteTunnelRequest(
    @Json(name = "tunnel_id") val tunnelId: String
)

@JsonClass(generateAdapter = true)
data class PlayitDeleteTunnelResponse(
    @Json(name = "status") val status: String,
    @Json(name = "error") val error: String? = null
)
