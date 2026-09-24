package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CachedMarketPluginEntity
import com.example.data.local.entity.ServerEntity
import com.example.data.local.entity.ServerFileEntity
import com.example.data.local.entity.ServerLogEntity
import com.example.data.remote.PumpkinMarketApi
import com.example.domain.model.LogEntry
import com.example.domain.model.LogLevel
import com.example.domain.model.MarketPlugin
import com.example.domain.model.ServerConfig
import com.example.domain.model.ServerFileItem
import com.example.domain.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PumpkinServerRepository(
    private val database: AppDatabase,
    private val marketApi: PumpkinMarketApi = PumpkinMarketApi.create()
) {
    val servers: Flow<List<ServerConfig>> = database.serverDao().getAllServers().map { list ->
        list.map { it.toDomain() }
    }

    fun observeServer(id: String): Flow<ServerConfig?> = database.serverDao().observeServerById(id).map { it?.toDomain() }

    suspend fun getServer(id: String): ServerConfig? = database.serverDao().getServerById(id)?.toDomain()

    suspend fun saveServer(server: ServerConfig) {
        database.serverDao().insertServer(ServerEntity.fromDomain(server))
    }

    suspend fun updateServerStatus(serverId: String, status: ServerStatus) {
        database.serverDao().updateStatus(serverId, status.name)
    }

    suspend fun deleteServer(serverId: String) {
        database.serverDao().deleteServerById(serverId)
        database.serverFileDao().deleteAllForServer(serverId)
        database.serverLogDao().clearLogsForServer(serverId)
    }

    // Files
    fun observeFiles(serverId: String): Flow<List<ServerFileItem>> {
        return database.serverFileDao().getFilesForServer(serverId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getFileContent(serverId: String, path: String): String? {
        val compositeId = "$serverId:$path"
        return database.serverFileDao().getFileByCompositeId(compositeId)?.content
    }

    suspend fun saveFileContent(serverId: String, path: String, content: String) {
        val compositeId = "$serverId:$path"
        val existing = database.serverFileDao().getFileByCompositeId(compositeId)
        val fileEntity = ServerFileEntity(
            compositeId = compositeId,
            serverId = serverId,
            path = path,
            name = path.substringAfterLast('/'),
            isDirectory = false,
            sizeBytes = content.length.toLong(),
            lastModified = System.currentTimeMillis(),
            content = content
        )
        database.serverFileDao().insertFile(fileEntity)
    }

    suspend fun createFile(serverId: String, path: String, isDirectory: Boolean, content: String = "") {
        val compositeId = "$serverId:$path"
        val entity = ServerFileEntity(
            compositeId = compositeId,
            serverId = serverId,
            path = path,
            name = path.substringAfterLast('/').ifEmpty { path },
            isDirectory = isDirectory,
            sizeBytes = content.length.toLong(),
            lastModified = System.currentTimeMillis(),
            content = content
        )
        database.serverFileDao().insertFile(entity)
    }

    suspend fun deleteFile(serverId: String, path: String) {
        val compositeId = "$serverId:$path"
        database.serverFileDao().deleteFile(compositeId)
    }

    // Logs
    fun observeLogs(serverId: String): Flow<List<LogEntry>> {
        return database.serverLogDao().getLogsForServer(serverId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun appendLog(
        serverId: String,
        level: LogLevel,
        tag: String,
        message: String
    ) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val log = LogEntry(
            serverId = serverId,
            timestamp = timeStr,
            level = level,
            tag = tag,
            message = message
        )
        database.serverLogDao().insertLog(ServerLogEntity.fromDomain(log))
    }

    suspend fun clearLogs(serverId: String) {
        database.serverLogDao().clearLogsForServer(serverId)
    }

    // Plugin Market
    fun observePlugins(category: String? = null, query: String? = null): Flow<List<MarketPlugin>> {
        val sourceFlow = when {
            !query.isNullOrBlank() -> database.marketPluginDao().searchPlugins(query)
            category != null && category != "All" -> database.marketPluginDao().getPluginsByCategory(category)
            else -> database.marketPluginDao().getAllPlugins()
        }
        return sourceFlow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshPluginsFromRemote(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // First ensure full verified catalog is populated
            database.marketPluginDao().insertPlugins(com.example.data.remote.PumpkinMarketCatalog.fullCatalog)

            val response = marketApi.getPlugins()
            if (response.isSuccessful && response.body() != null) {
                val apiItems = response.body()?.items ?: emptyList()
                if (apiItems.isNotEmpty()) {
                    val entities = apiItems.mapNotNull { item ->
                        val id = item.id ?: item.slug ?: return@mapNotNull null
                        CachedMarketPluginEntity(
                            id = id,
                            slug = item.slug ?: id,
                            name = item.name ?: id,
                            description = item.description ?: "PumpkinMC Plugin",
                            category = item.category ?: "General",
                            author = item.author ?: "PumpkinDev",
                            version = item.version ?: "1.0.0",
                            mcVersion = item.mcVersion ?: "1.21.4",
                            rating = item.rating ?: 4.8f,
                            downloads = item.downloads ?: 100,
                            iconUrl = item.iconUrl ?: "",
                            isMod = item.isMod ?: false,
                            sizeText = item.size ?: "150 KB"
                        )
                    }
                    database.marketPluginDao().insertPlugins(entities)
                }
            }
            val count = database.marketPluginDao().getCount()
            Result.success(count)
        } catch (e: Exception) {
            // Ensure full catalog is present even when offline
            database.marketPluginDao().insertPlugins(com.example.data.remote.PumpkinMarketCatalog.fullCatalog)
            val count = database.marketPluginDao().getCount()
            Result.success(count)
        }
    }

    suspend fun installPluginToServer(serverId: String, plugin: MarketPlugin) {
        val extension = if (plugin.isMod) "jar" else "wasm"
        val fileName = "${plugin.name.replace(" ", "")}.$extension"
        val pluginPath = "plugins/$fileName"
        val fileContent = """
            # Binary Plugin Artifact: ${plugin.name} v${plugin.version}
            # Author: ${plugin.author}
            # Target Engine: PumpkinMC (Minecraft ${plugin.mcVersion})
            # Category: ${plugin.category}
            [plugin]
            name = "${plugin.name}"
            version = "${plugin.version}"
            description = "${plugin.description}"
            author = "${plugin.author}"
            enabled = true
        """.trimIndent()

        createFile(
            serverId = serverId,
            path = pluginPath,
            isDirectory = false,
            content = fileContent
        )

        appendLog(
            serverId = serverId,
            level = LogLevel.INFO,
            tag = "pumpkin::plugin_market",
            message = "Successfully installed plugin '${plugin.name}' v${plugin.version} into /$pluginPath"
        )
    }

    suspend fun uninstallPlugin(serverId: String, pluginName: String) {
        val files = database.serverFileDao().getFilesForServer(serverId).firstOrNull() ?: emptyList()
        val target = files.firstOrNull { it.path.contains(pluginName, ignoreCase = true) }
        if (target != null) {
            database.serverFileDao().deleteFile(target.compositeId)
            appendLog(
                serverId = serverId,
                level = LogLevel.WARN,
                tag = "pumpkin::plugins",
                message = "Plugin file '${target.path}' removed from server instance."
            )
        }
    }

    // Network Utilities
    fun getLocalDeviceIp(context: Context? = null): String {
        // 1. Try ConnectivityManager if context is available
        if (context != null) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                if (activeNetwork != null) {
                    val linkProps = cm.getLinkProperties(activeNetwork)
                    linkProps?.linkAddresses?.forEach { linkAddress ->
                        val addr = linkAddress.address
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                val ipInt = wm?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    val ipStr = String.format(
                        Locale.US,
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                    if (ipStr != "0.0.0.0") return ipStr
                }
            } catch (_: Exception) {}
        }

        // 2. Iterate network interfaces safely without calling iface.isUp (prevents SocketException on Android 10+)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            val candidates = mutableListOf<Pair<String, String>>()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val isLoopback = try { iface.isLoopback } catch (_: Exception) { false }
                if (isLoopback) continue

                val addresses = try { iface.inetAddresses } catch (_: Exception) { null } ?: continue
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) {
                        val isLoopbackAddr = try { addr.isLoopbackAddress } catch (_: Exception) { false }
                        if (!isLoopbackAddr) {
                            val host = addr.hostAddress ?: continue
                            if (!host.startsWith("127.")) {
                                candidates.add(iface.name.lowercase() to host)
                            }
                        }
                    }
                }
            }
            // 1. Wi-Fi interface (wlan, etc.)
            candidates.firstOrNull { it.first.contains("wlan") }?.let { return it.second }
            // 2. Hotspot / Access Point interface (ap, rndis, p2p, softap)
            candidates.firstOrNull { it.first.contains("ap") || it.first.contains("rndis") || it.first.contains("p2p") }?.let { return it.second }
            // 3. Ethernet (eth)
            candidates.firstOrNull { it.first.contains("eth") }?.let { return it.second }
            // 4. Any private LAN range (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
            candidates.firstOrNull {
                it.second.startsWith("192.168.") || it.second.startsWith("10.") || it.second.startsWith("172.")
            }?.let { return it.second }
            // 5. Any candidate
            candidates.firstOrNull()?.let { return it.second }
        } catch (_: Exception) {
        }
        return ""
    }
}
