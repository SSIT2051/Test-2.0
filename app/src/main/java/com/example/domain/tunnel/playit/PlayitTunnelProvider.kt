package com.example.domain.tunnel.playit

import android.content.Context
import android.content.SharedPreferences
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
    private var prefs: SharedPreferences? = null

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
        prefs = context.getSharedPreferences("pumpkin_playit_prefs", Context.MODE_PRIVATE)
        true
    }

    fun getSavedSecretKey(): String {
        return prefs?.getString("playit_secret_key", "") ?: ""
    }

    fun saveSecretKey(key: String) {
        prefs?.edit()?.putString("playit_secret_key", key)?.apply()
    }

    fun clearSecretKey() {
        prefs?.edit()?.remove("playit_secret_key")?.apply()
    }

    suspend fun getClaimSetup(): PlayitClaimSetupData? = withContext(Dispatchers.IO) {
        try {
            val resp = api.setupClaim(PlayitClaimSetupRequest())
            if (resp.isSuccessful && resp.body()?.status == "success") {
                val data = resp.body()?.data
                if (data?.secretKey != null && data.secretKey.isNotBlank()) {
                    saveSecretKey(data.secretKey)
                }
                data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "setupClaim error: ${e.message}", e)
            null
        }
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
            // First check if an account secret key is already linked/saved
            var secretKey = getSavedSecretKey()

            if (secretKey.isBlank()) {
                // Initialize claim to get agent session token
                val claimResp = api.setupClaim(PlayitClaimSetupRequest())
                val claimData = claimResp.body()?.data
                val newSecret = claimData?.secretKey ?: ""
                if (newSecret.isNotBlank()) {
                    secretKey = newSecret
                    saveSecretKey(newSecret)
                }
            }

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

            TunnelResult.Failure(
                errorMessage = "Playit API endpoint provisioning failed. Claim setup or account linking required."
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
            val secretKey = getSavedSecretKey()
            if (secretKey.isNotBlank() && providerTunnelId.isNotBlank()) {
                val resp = api.deleteTunnel(
                    authHeader = "Bearer $secretKey",
                    request = PlayitDeleteTunnelRequest(tunnelId = providerTunnelId)
                )
                resp.isSuccessful
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "Error deleting tunnel from Playit: ${e.message}", e)
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
