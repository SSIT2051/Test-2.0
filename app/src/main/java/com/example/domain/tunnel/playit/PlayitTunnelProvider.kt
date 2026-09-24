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

    suspend fun createTunnelInternal(
        serverName: String,
        localPort: Int,
        protocol: TunnelProtocol,
        authHeader: String,
        agentId: String
    ): Boolean {
        val tunnelType = when (protocol) {
            TunnelProtocol.TCP -> "minecraft-java"
            TunnelProtocol.UDP -> "minecraft-bedrock"
        }
        val portType = when (protocol) {
            TunnelProtocol.TCP -> "tcp"
            TunnelProtocol.UDP -> "udp"
        }
        val jsonMedia = "application/json".toMediaTypeOrNull()

        // 1. Try standard schema (used by playit-api / playit-minecraft-plugin)
        val originData = JSONObject().apply {
            if (agentId.isNotBlank()) {
                put("agent_id", agentId)
            }
            put("local_ip", "127.0.0.1")
            put("local_port", localPort)
        }
        val originObj = JSONObject().apply {
            put("type", if (agentId.isNotBlank()) "agent" else "default")
            put("data", originData)
        }

        val createJson = JSONObject().apply {
            put("name", "PumpkinMC ${if (protocol == TunnelProtocol.UDP) "Bedrock" else "Java"}")
            put("tunnel_type", tunnelType)
            put("port_type", portType)
            put("port_count", 1)
            put("origin", originObj)
            put("enabled", true)
        }
        try {
            val resp = api.createTunnelRaw(authHeader, createJson.toString().toRequestBody(jsonMedia))
            val body = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            Log.d("PlayitTunnelProvider", "createTunnelRaw ($tunnelType) code: ${resp.code()}, body: $body")
            if (resp.isSuccessful) return true
        } catch (e: Exception) {
            Log.w("PlayitTunnelProvider", "createTunnelRaw error: ${e.message}")
        }

        // 2. Fallback to v1 schema
        val v1Json = JSONObject().apply {
            put("name", "PumpkinMC ${if (protocol == TunnelProtocol.UDP) "Bedrock" else "Java"}")
            put("protocol", JSONObject().apply {
                put("type", "tunnel-type")
                put("details", tunnelType)
            })
            put("origin", JSONObject().apply {
                put("type", if (agentId.isNotBlank()) "agent" else "default")
                put("data", JSONObject().apply {
                    if (agentId.isNotBlank()) put("agent_id", agentId)
                    put("local_ip", "127.0.0.1")
                    put("local_port", localPort)
                })
            })
            put("endpoint", JSONObject().apply {
                put("type", "region")
                put("details", JSONObject().apply {
                    put("region", "global")
                })
            })
            put("enabled", true)
        }
        try {
            val v1Resp = api.createV1TunnelRaw(authHeader, v1Json.toString().toRequestBody(jsonMedia))
            Log.d("PlayitTunnelProvider", "createV1TunnelRaw ($tunnelType) code: ${v1Resp.code()}")
            return v1Resp.isSuccessful
        } catch (e: Exception) {
            Log.w("PlayitTunnelProvider", "createV1TunnelRaw error: ${e.message}")
        }

        return false
    }

    suspend fun autoProvisionBothTunnels(
        serverName: String = "PumpkinMC Server",
        bedrockPort: Int = 19132,
        javaPort: Int = 25565
    ): PlayitEndpoints = withContext(Dispatchers.IO) {
        val secretKey = getSavedSecretKey()
        if (secretKey.isBlank()) return@withContext PlayitEndpoints(null, null)

        val authHeader = if (secretKey.startsWith("agent-key ", ignoreCase = true)) secretKey else "agent-key $secretKey"
        val runData = fetchAgentRunData(authHeader)
        val agentId = runData?.agentId.orEmpty()

        var currentEndpoints = getDiscoveredEndpoints()
        var needBedrock = currentEndpoints.bedrockTunnel == null
        var needJava = currentEndpoints.javaTunnel == null

        if (needBedrock) {
            createTunnelInternal(serverName, bedrockPort, TunnelProtocol.UDP, authHeader, agentId)
        }
        if (needJava) {
            createTunnelInternal(serverName, javaPort, TunnelProtocol.TCP, authHeader, agentId)
        }

        // Poll for up to 10 seconds (10 x 1000ms) for public addresses to allocate
        for (attempt in 1..10) {
            delay(1000)
            currentEndpoints = getDiscoveredEndpoints()
            if (currentEndpoints.bedrockTunnel != null && currentEndpoints.javaTunnel != null) {
                break
            }
            if (currentEndpoints.bedrockTunnel != null || currentEndpoints.javaTunnel != null) {
                // At least one allocated, keep polling briefly
                if (attempt >= 5) break
            }
        }

        currentEndpoints
    }

    override suspend fun createTunnel(
        serverId: String,
        serverName: String,
        localPort: Int,
        protocol: TunnelProtocol
    ): TunnelResult = withContext(Dispatchers.IO) {
        try {
            val secretKey = getSavedSecretKey()
            if (secretKey.isBlank()) {
                return@withContext TunnelResult.Failure(
                    errorMessage = "Playit account not linked. Please tap 'Link Playit.gg' to link your account."
                )
            }

            val authHeader = if (secretKey.startsWith("agent-key ", ignoreCase = true)) secretKey else "agent-key $secretKey"

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

            // 2. Request creation of new tunnel
            createTunnelInternal(serverName, localPort, protocol, authHeader, agentId)

            // 3. Poll for assigned public address & port
            for (attempt in 1..8) {
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
                errorMessage = "Tunnel created! Allocation propagating on Playit.gg network. Tap 'Sync Live Address' in a moment."
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

    private fun parsePlayitTunnelJson(t: JSONObject): PlayitDiscoveredTunnel? {
        val id = t.optString("id")
        var proto = t.optString("port_type").ifBlank { t.optString("proto") }
        val tunnelType = t.optString("tunnel_type")
        if (proto.isBlank()) {
            proto = when {
                tunnelType.contains("bedrock", ignoreCase = true) -> "udp"
                tunnelType.contains("java", ignoreCase = true) -> "tcp"
                else -> "both"
            }
        }

        var host = ""
        var port = 0

        // 1. Check display_address (e.g. "subdomain.ply.gg:12345" or "147.185.221.16:12345")
        val displayAddress = t.optString("display_address").trim()
        if (displayAddress.contains(":")) {
            val parts = displayAddress.split(":")
            host = parts[0].trim()
            port = parts.getOrNull(1)?.toIntOrNull() ?: 0
        }

        // 2. Check public_allocations
        if (host.isBlank() || port <= 0) {
            val publicAllocArr = t.optJSONArray("public_allocations")
            if (publicAllocArr != null && publicAllocArr.length() > 0) {
                for (j in 0 until publicAllocArr.length()) {
                    val allocItem = publicAllocArr.optJSONObject(j) ?: continue
                    val details = allocItem.optJSONObject("details") ?: allocItem
                    val ipHostname = details.optString("ip_hostname")
                    val autoDomain = details.optString("auto_domain")
                    val ip = details.optString("ip")
                    val allocPort = details.optInt("port", 0).takeIf { it > 0 } ?: details.optInt("port_start", 0)
                    val candidateHost = ipHostname.ifBlank { autoDomain.ifBlank { ip } }
                    if (candidateHost.isNotBlank() && allocPort > 0) {
                        host = candidateHost
                        port = allocPort
                        val allocProto = details.optString("port_type")
                        if (allocProto.isNotBlank()) proto = allocProto
                        break
                    }
                }
            }
        }

        // 3. Check connect_addresses
        if (host.isBlank() || port <= 0) {
            val connectArr = t.optJSONArray("connect_addresses")
            if (connectArr != null && connectArr.length() > 0) {
                for (j in 0 until connectArr.length()) {
                    val conn = connectArr.optJSONObject(j) ?: continue
                    val valObj = conn.optJSONObject("value")
                    val addr = valObj?.optString("address") ?: conn.optString("address")
                    val domain = valObj?.optString("domain") ?: conn.optString("domain")
                    if (addr.isNotBlank() && addr.contains(":")) {
                        val parts = addr.split(":")
                        if (host.isBlank()) host = parts[0].trim()
                        if (port <= 0) port = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    }
                    if (domain.isNotBlank()) {
                        host = domain.trim()
                    }
                }
            }
        }

        // 4. Check alloc.data or alloc
        if (host.isBlank() || port <= 0) {
            val alloc = t.optJSONObject("alloc")
            val allocData = alloc?.optJSONObject("data") ?: alloc
            val assigned = allocData?.optString("assigned_domain")
                ?.ifBlank { allocData.optString("custom_domain") }
                ?.ifBlank { allocData.optString("assigned_ip") }
                ?.ifBlank { allocData.optString("ip_hostname") }
                ?: ""
            var allocPort = allocData?.optInt("port_start", 0)?.takeIf { it > 0 }
                ?: allocData?.optInt("port", 0)
                ?: 0
            if (allocPort <= 0) {
                val pObj = allocData?.optJSONObject("port")
                allocPort = pObj?.optInt("from", 0)?.takeIf { it > 0 }
                    ?: pObj?.optInt("to", 0)?.takeIf { it > 0 }
                    ?: 0
            }
            if (assigned.isNotBlank() && host.isBlank()) host = assigned.trim()
            if (allocPort > 0 && port <= 0) port = allocPort
        }

        // 5. Check root assigned_domain / custom_domain / assigned_ip / port
        if (host.isBlank()) {
            host = t.optString("assigned_domain")
                .ifBlank { t.optString("custom_domain") }
                .ifBlank { t.optString("assigned_ip") }
                .ifBlank { t.optString("ip_hostname") }
                .trim()
        }
        if (port <= 0) {
            val portObj = t.optJSONObject("port")
            port = portObj?.optInt("from", 0)?.takeIf { it > 0 }
                ?: portObj?.optInt("to", 0)?.takeIf { it > 0 }
                ?: t.optInt("port", 0)
        }

        if (host.isNotBlank() && port > 0) {
            return PlayitDiscoveredTunnel(
                id = id.ifBlank { host },
                host = host,
                port = port,
                proto = proto
            )
        }
        return null
    }

    private suspend fun fetchAgentRunData(authHeader: String): PlayitRunDataResult? {
        try {
            val jsonMedia = "application/json".toMediaTypeOrNull()
            val emptyBody = "{}".toRequestBody(jsonMedia)

            var bodyStr = ""
            try {
                val v1Resp = api.getV1AgentRunData(authHeader, emptyBody)
                if (v1Resp.isSuccessful) {
                    bodyStr = v1Resp.body()?.string() ?: ""
                }
            } catch (_: Exception) {}

            if (bodyStr.isBlank()) {
                val resp = api.getAgentRunData(authHeader, emptyBody)
                bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            }
            if (bodyStr.isBlank()) return null

            val json = JSONObject(bodyStr)
            val dataObj = json.optJSONObject("data") ?: json
            val agentId = dataObj.optString("agent_id")
            val tunnelsArr = dataObj.optJSONArray("tunnels")
                ?: json.optJSONArray("tunnels")
                ?: json.optJSONArray("data")
                ?: JSONArray()
            val tunnels = mutableListOf<PlayitDiscoveredTunnel>()

            for (i in 0 until tunnelsArr.length()) {
                val t = tunnelsArr.optJSONObject(i) ?: continue
                val tunnel = parsePlayitTunnelJson(t)
                if (tunnel != null) {
                    tunnels.add(tunnel)
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

            var bodyStr = ""
            try {
                val v1Resp = api.listV1TunnelsRaw(authHeader, emptyBody)
                if (v1Resp.isSuccessful) {
                    bodyStr = v1Resp.body()?.string() ?: ""
                }
            } catch (_: Exception) {}

            if (bodyStr.isBlank()) {
                val resp = api.listTunnelsRaw(authHeader, emptyBody)
                bodyStr = resp.body()?.string() ?: resp.errorBody()?.string() ?: ""
            }
            if (bodyStr.isBlank()) return emptyList()

            val json = JSONObject(bodyStr)
            val dataObj = json.optJSONObject("data") ?: json
            val tunnelsArr = dataObj.optJSONArray("tunnels")
                ?: json.optJSONArray("tunnels")
                ?: json.optJSONArray("data")
                ?: JSONArray()
            val list = mutableListOf<PlayitDiscoveredTunnel>()

            for (i in 0 until tunnelsArr.length()) {
                val t = tunnelsArr.optJSONObject(i) ?: continue
                val tunnel = parsePlayitTunnelJson(t)
                if (tunnel != null) {
                    list.add(tunnel)
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

