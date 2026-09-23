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

    fun generateClaimCode(): String {
        val bytes = ByteArray(5)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun startClaimFlow(): PlayitClaimInfo? = withContext(Dispatchers.IO) {
        try {
            val code = generateClaimCode()
            val request = PlayitClaimSetupRequest(
                code = code,
                agentType = "self-managed",
                version = "playit 0.15.26"
            )
            val resp = api.setupClaim(request)
            val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            if (bodyStr.isNotBlank()) {
                val json = org.json.JSONObject(bodyStr)
                if (json.optString("status") == "success") {
                    val claimUrl = "https://playit.gg/claim/$code"
                    return@withContext PlayitClaimInfo(code = code, claimUrl = claimUrl)
                }
            }
            null
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "startClaimFlow error: ${e.message}", e)
            null
        }
    }

    /**
     * Sends heartbeat to /claim/setup.
     * Playit's web UI requires continuous /claim/setup polling to transition from
     * "Waiting for agent..." to "Add to Account" / "UserAccepted".
     */
    suspend fun pollClaimSetup(code: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = PlayitClaimSetupRequest(
                code = code,
                agentType = "self-managed",
                version = "playit 0.15.26"
            )
            val resp = api.setupClaim(request)
            val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            if (bodyStr.isNotBlank()) {
                val json = org.json.JSONObject(bodyStr)
                if (json.optString("status") == "success") {
                    return@withContext json.optString("data") // e.g. "WaitingForUserVisit", "WaitingForUser", "UserAccepted"
                }
            }
            null
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "pollClaimSetup error: ${e.message}", e)
            null
        }
    }

    suspend fun checkClaimExchange(code: String): String? = withContext(Dispatchers.IO) {
        try {
            val resp = api.exchangeClaim(PlayitClaimExchangeRequest(code = code))
            val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            if (bodyStr.isNotBlank()) {
                val json = org.json.JSONObject(bodyStr)
                if (json.optString("status") == "success") {
                    val dataObj = json.optJSONObject("data")
                    val secretKey = dataObj?.optString("secret_key")
                    if (!secretKey.isNullOrBlank()) {
                        saveSecretKey(secretKey)
                        return@withContext secretKey
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "checkClaimExchange error: ${e.message}", e)
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
            val secretKey = getSavedSecretKey()
            if (secretKey.isBlank()) {
                return@withContext TunnelResult.Failure(
                    errorMessage = "Playit account not linked. Please tap 'Claim Account' to link your playit.gg account."
                )
            }

            val authHeader = if (secretKey.startsWith("Agent-Key ")) secretKey else "Agent-Key $secretKey"
            val createResp = api.createTunnel(
                authHeader = authHeader,
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

            TunnelResult.Failure(
                errorMessage = "Playit API tunnel allocation failed. Ensure your agent is active."
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
                val authHeader = if (secretKey.startsWith("Agent-Key ")) secretKey else "Agent-Key $secretKey"
                val resp = api.deleteTunnel(
                    authHeader = authHeader,
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
