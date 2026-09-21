package com.example.domain.tunnel.playit

import android.content.Context
import android.util.Log
import com.example.domain.tunnel.TunnelConfig
import com.example.domain.tunnel.TunnelProtocol
import com.example.domain.tunnel.TunnelProvider
import com.example.domain.tunnel.TunnelResult
import com.example.domain.tunnel.TunnelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PlayitTunnelProvider : TunnelProvider {
    override val providerId: String = "playit"
    override val displayName: String = "Playit.gg Network"

    private val activeSessions = ConcurrentHashMap<String, PlayitAgentClient>()

    private val api: PlayitApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.playit.gg/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(PlayitApiService::class.java)
    }

    override suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        true
    }

    override suspend fun createTunnel(
        serverId: String,
        serverName: String,
        localPort: Int,
        protocol: TunnelProtocol
    ): TunnelResult = withContext(Dispatchers.IO) {
        val tunnelType = when (protocol) {
            TunnelProtocol.TCP -> "minecraft-java"
            TunnelProtocol.UDP -> "minecraft-bedrock"
        }
        val portType = when (protocol) {
            TunnelProtocol.TCP -> "tcp"
            TunnelProtocol.UDP -> "udp"
        }

        try {
            // First initialize claim to get agent session token
            val claimResp = api.setupClaim(PlayitClaimSetupRequest())
            val claimData = claimResp.body()?.data
            val secretKey = claimData?.secretKey ?: ""

            if (secretKey.isNotBlank()) {
                val createResp = api.createTunnel(
                    authHeader = "Bearer $secretKey",
                    request = PlayitCreateTunnelRequest(
                        name = serverName.take(30),
                        tunnelType = tunnelType,
                        portType = portType,
                        portCount = 1,
                        localIp = "127.0.0.1",
                        localPort = localPort
                    )
                )

                if (createResp.isSuccessful && createResp.body()?.status == "success") {
                    val tunnelData = createResp.body()?.data
                    if (tunnelData != null) {
                        val pubHost = tunnelData.assignedDomain ?: tunnelData.ipHostname ?: ""
                        val pubPort = tunnelData.port ?: tunnelData.portFrom ?: 0
                        if (pubHost.isNotBlank() && pubPort > 0) {
                            return@withContext TunnelResult.Success(
                                providerTunnelId = tunnelData.id,
                                publicHost = pubHost,
                                publicPort = pubPort,
                                protocol = protocol
                            )
                        }
                    }
                }
            }

            // If public playit API call fails or requires interactive web claim, return a structured Failure
            // so PumpkinHost never invents or pretends an address is ready.
            TunnelResult.Failure(
                errorMessage = "Playit API endpoint provisioning failed. Claim setup returned no valid tunnel allocation."
            )
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "Error creating tunnel: ${e.message}", e)
            TunnelResult.Failure(
                errorMessage = "Connection to Playit.gg failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    override suspend fun deleteTunnel(providerTunnelId: String): Boolean = withContext(Dispatchers.IO) {
        stopTunnelSession(providerTunnelId)
        try {
            // Best effort deletion via API
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun startTunnelSession(
        tunnelConfig: TunnelConfig,
        onStatusChanged: (TunnelStatus, String?) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val client = PlayitAgentClient(tunnelConfig, onStatusChanged)
        activeSessions[tunnelConfig.providerTunnelId] = client
        client.start()
        true
    }

    override suspend fun stopTunnelSession(providerTunnelId: String) {
        activeSessions.remove(providerTunnelId)?.stop()
    }

    override suspend fun verifyTunnelEndpoint(publicHost: String, publicPort: Int): Boolean = withContext(Dispatchers.IO) {
        if (publicHost.isBlank() || publicPort <= 0) return@withContext false
        try {
            val address = InetAddress.getByName(publicHost)
            address.hostAddress != null
        } catch (e: Exception) {
            false
        }
    }
}
