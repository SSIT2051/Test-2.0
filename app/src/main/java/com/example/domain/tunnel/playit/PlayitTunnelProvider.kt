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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class PlayitDiscoveredTunnel(
    val id: String,
    val host: String,
    val port: Int,
    val proto: String
)

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
                    errorMessage = "Playit account not linked. Please tap 'Link Playit.gg' to link your account."
                )
            }

            val authHeader = if (secretKey.startsWith("agent-key ", ignoreCase = true)) secretKey else "agent-key $secretKey"
            val jsonMedia = "application/json".toMediaTypeOrNull()

            // 1. Query rundata and tunnel list for existing allocated tunnels
            val initialRunData = fetchAgentRunData(authHeader)
            val agentId = initialRunData?.agentId.orEmpty()
            val existing = initialRunData?.tunnels?.find { matchesProtocol(it.proto, protocol) }
                ?: fetchTunnelsList(authHeader).find { matchesProtocol(it.proto, protocol) }

            if (existing != null) {
                Log.d("PlayitTunnelProvider", "Found existing tunnel: ${existing.host}:${existing.port}")
                return@withContext TunnelResult.Success(
                    providerTunnelId = existing.id,
                    publicHost = existing.host,
                    publicPort = existing.port,
                    protocol = protocol
                )
            }

            // 2. Request creation of new tunnel with proper origin structure
            val originObj = JSONObject().apply {
                put("type", "agent")
                put("data", JSONObject().apply {
                    if (agentId.isNotBlank()) {
                        put("agent_id", agentId)
                    }
                    put("local_ip", "127.0.0.1")
                    put("local_port", localPort)
                })
            }

            val createJson = JSONObject().apply {
                put("name", serverName.take(30))
                put("tunnel_type", tunnelType)
                put("port_type", portType)
                put("port_count", 1)
                put("origin", originObj)
                put("enabled", true)
            }
            val createBody = createJson.toString().toRequestBody(jsonMedia)
            val createResp = api.createTunnelRaw(authHeader, createBody)
            val createStr = createResp.body()?.string() ?: createResp.errorBody()?.string() ?: ""
            Log.d("PlayitTunnelProvider", "tunnels/create result: $createStr")

            // 3. Poll for assigned public address & port
            for (attempt in 1..6) {
                delay(1000)
                val allocated = fetchAgentRunData(authHeader)?.tunnels?.find { matchesProtocol(it.proto, protocol) }
                    ?: fetchTunnelsList(authHeader).find { matchesProtocol(it.proto, protocol) }
                if (allocated != null) {
                    return@withContext TunnelResult.Success(
                        providerTunnelId = allocated.id,
                        publicHost = allocated.host,
                        publicPort = allocated.port,
                        protocol = protocol
                    )
                }
            }

            // 4. Fallback: check if any tunnel exists at all
            val anyTunnel = fetchAgentRunData(authHeader)?.tunnels?.firstOrNull()
                ?: fetchTunnelsList(authHeader).firstOrNull()
            if (anyTunnel != null) {
                return@withContext TunnelResult.Success(
                    providerTunnelId = anyTunnel.id,
                    publicHost = anyTunnel.host,
                    publicPort = anyTunnel.port,
                    protocol = protocol
                )
            }

            TunnelResult.Failure(
                errorMessage = "Tunnel created! Allocation propagating on Playit.gg network. Tap 'Sync Tunnel' in a moment."
            )
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "Error creating tunnel: ${e.message}", e)
            TunnelResult.Failure(
                errorMessage = "Connection to Playit.gg failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    private data class PlayitRunDataResult(
        val agentId: String,
        val tunnels: List<PlayitDiscoveredTunnel>
    )

    private fun matchesProtocol(proto: String, desired: TunnelProtocol): Boolean {
        return when (desired) {
            TunnelProtocol.UDP -> proto.equals("udp", ignoreCase = true) || proto.equals("both", ignoreCase = true)
            TunnelProtocol.TCP -> proto.equals("tcp", ignoreCase = true) || proto.equals("both", ignoreCase = true)
        }
    }

    private suspend fun fetchAgentRunData(authHeader: String): PlayitRunDataResult? {
        try {
            val jsonMedia = "application/json".toMediaTypeOrNull()
            val emptyBody = "{}".toRequestBody(jsonMedia)
            val resp = api.getAgentRunData(authHeader, emptyBody)
            val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            if (bodyStr.isBlank()) return null
            val json = JSONObject(bodyStr)
            if (json.optString("status") != "success") return null
            val dataObj = json.optJSONObject("data") ?: return null
            val agentId = dataObj.optString("agent_id")
            val tunnelsArr = dataObj.optJSONArray("tunnels") ?: JSONArray()
            val tunnels = mutableListOf<PlayitDiscoveredTunnel>()

            for (i in 0 until tunnelsArr.length()) {
                val t = tunnelsArr.optJSONObject(i) ?: continue
                val id = t.optString("id")
                val assignedDomain = t.optString("assigned_domain").ifBlank { t.optString("custom_domain") }
                val portObj = t.optJSONObject("port")
                val port = portObj?.optInt("from") ?: portObj?.optInt("to") ?: t.optInt("port", 0)
                val proto = t.optString("proto")
                if (assignedDomain.isNotBlank() && port > 0) {
                    tunnels.add(PlayitDiscoveredTunnel(id = id, host = assignedDomain, port = port, proto = proto))
                }
            }
            return PlayitRunDataResult(agentId, tunnels)
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "fetchAgentRunData error: ${e.message}")
            return null
        }
    }

    private suspend fun fetchTunnelsList(authHeader: String): List<PlayitDiscoveredTunnel> {
        try {
            val jsonMedia = "application/json".toMediaTypeOrNull()
            val emptyBody = "{}".toRequestBody(jsonMedia)
            val resp = api.listTunnelsRaw(authHeader, emptyBody)
            val bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            if (bodyStr.isBlank()) return emptyList()
            val json = JSONObject(bodyStr)
            if (json.optString("status") != "success") return emptyList()
            val dataObj = json.optJSONObject("data") ?: return emptyList()
            val tunnelsArr = dataObj.optJSONArray("tunnels") ?: return emptyList()
            val list = mutableListOf<PlayitDiscoveredTunnel>()

            for (i in 0 until tunnelsArr.length()) {
                val t = tunnelsArr.optJSONObject(i) ?: continue
                val id = t.optString("id")
                val proto = t.optString("port_type")
                val alloc = t.optJSONObject("alloc")
                val allocData = alloc?.optJSONObject("data")
                val assignedDomain = allocData?.optString("assigned_domain") ?: ""
                val port = allocData?.optInt("port_start", 0) ?: 0
                if (assignedDomain.isNotBlank() && port > 0) {
                    list.add(PlayitDiscoveredTunnel(id = id, host = assignedDomain, port = port, proto = proto))
                }
            }
            return list
        } catch (e: Exception) {
            Log.e("PlayitTunnelProvider", "fetchTunnelsList error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun queryAllTunnels(): List<PlayitDiscoveredTunnel> = withContext(Dispatchers.IO) {
        try {
            val secretKey = getSavedSecretKey()
            if (secretKey.isBlank()) return@withContext emptyList()
            val authHeader = if (secretKey.startsWith("agent-key ", ignoreCase = true)) secretKey else "agent-key $secretKey"
            val runDataTunnels = fetchAgentRunData(authHeader)?.tunnels.orEmpty()
            val listTunnels = fetchTunnelsList(authHeader)
            val combined = (runDataTunnels + listTunnels).distinctBy { "${it.host}:${it.port}" }
            combined
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteTunnel(providerTunnelId: String): Boolean = withContext(Dispatchers.IO) {
        stopTunnelSession(providerTunnelId)
        try {
            val secretKey = getSavedSecretKey()
            if (secretKey.isNotBlank() && providerTunnelId.isNotBlank()) {
                val authHeader = if (secretKey.startsWith("agent-key ", ignoreCase = true)) secretKey else "agent-key $secretKey"
                val jsonMedia = "application/json".toMediaTypeOrNull()
                val body = JSONObject().apply { put("tunnel_id", providerTunnelId) }.toString().toRequestBody(jsonMedia)
                val resp = api.deleteTunnelRaw(authHeader, body)
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

    suspend fun getDiscoveredEndpoints(): PlayitEndpoints = withContext(Dispatchers.IO) {
        val tunnels = queryAllTunnels()
        val bedrock = tunnels.find { it.proto.equals("udp", true) || it.proto.equals("both", true) }
        val java = tunnels.find { it.proto.equals("tcp", true) || it.proto.equals("both", true) }
        PlayitEndpoints(
            bedrockTunnel = bedrock ?: tunnels.firstOrNull(),
            javaTunnel = java ?: tunnels.lastOrNull()
        )
    }
}

data class PlayitEndpoints(
    val bedrockTunnel: PlayitDiscoveredTunnel?,
    val javaTunnel: PlayitDiscoveredTunnel?
)

