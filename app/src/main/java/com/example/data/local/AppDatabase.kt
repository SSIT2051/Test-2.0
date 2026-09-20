package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MarketPluginDao
import com.example.data.local.dao.ServerDao
import com.example.data.local.dao.ServerFileDao
import com.example.data.local.dao.ServerLogDao
import com.example.data.local.entity.CachedMarketPluginEntity
import com.example.data.local.entity.ServerEntity
import com.example.data.local.entity.ServerFileEntity
import com.example.data.local.entity.ServerLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ServerEntity::class,
        ServerFileEntity::class,
        CachedMarketPluginEntity::class,
        ServerLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun serverFileDao(): ServerFileDao
    abstract fun marketPluginDao(): MarketPluginDao
    abstract fun serverLogDao(): ServerLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pumpkinmc_host.db"
                ).fallbackToDestructiveMigration()
                 .addCallback(DatabaseCallback())
                 .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val defaultServerId = "server_pumpkin_smp_01"

            val initialServers = listOf(
                ServerEntity(
                    id = defaultServerId,
                    name = "Pumpkin Server",
                    port = 25565,
                    serverVersion = "1.21.4 (Latest)",
                    motd = "PumpkinMC Server",
                    maxPlayers = 20,
                    gamemode = "survival",
                    difficulty = "normal",
                    pvp = true,
                    onlineMode = true,
                    allocatedRamMb = 1024,
                    viewDistance = 10,
                    simulationDistance = 8,
                    workerThreads = 4,
                    rayonThreads = 4,
                    maxStorageMb = 2048,
                    autoSaveMinutes = 5,
                    allowFlight = false,
                    hardcore = false,
                    lanModeEnabled = true,
                    bedrockCrossplayEnabled = true,
                    bedrockPort = 19132,
                    playitEnabled = true,
                    playitDomain = "playit-free.gl.joinmc.link",
                    playitPort = 19132,
                    customTunnelEnabled = false,
                    customTunnelType = "Custom",
                    customTunnelAddress = "",
                    customPort = 19132,
                    status = "STOPPED",
                    createdAt = System.currentTimeMillis() - 86400000L
                )
            )
            database.serverDao().insertAll(initialServers)

            // Initial configuration files for Default Server
            val pumpkinTomlContent = """
# =======================================================
# PumpkinMC Server Configuration (pumpkin.toml)
# Built from scratch in Rust for high throughput & low RAM
# =======================================================

[server]
address = "0.0.0.0:25565"
max_players = 20
view_distance = 10
simulation_distance = 8
motd = "A blazingly fast PumpkinMC Server in Rust!"
online_mode = true
compression_threshold = 256
network_buffer_size = 65536

[gameplay]
default_gamemode = "survival"
difficulty = "normal"
pvp = true
allow_flight = false
hardcore = false
spawn_protection = 16

[networking]
lan_broadcast = true
tokio_worker_threads = 4
packet_batching = true
tcp_nodelay = true

[resources]
rayon_threads = 4
chunk_cache_limit = 4096
""".trimIndent()

            val serverPropertiesContent = """
# Minecraft Server Properties for PumpkinMC
server-port=25565
gamemode=survival
difficulty=normal
pvp=true
max-players=20
motd=A blazingly fast PumpkinMC Server in Rust!
online-mode=true
enable-query=true
enable-rcon=false
""".trimIndent()

            val whitelistContent = """
[
  {
    "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6",
    "name": "PumpkinDev"
  },
  {
    "uuid": "4566e69f-c907-48ee-8d71-d7ba5aa00d20",
    "name": "AlexBuilder"
  }
]
""".trimIndent()

            val opsContent = """
[
  {
    "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6",
    "name": "PumpkinDev",
    "level": 4,
    "bypassesPlayerLimit": true
  }
]
""".trimIndent()

            val initialFiles = listOf(
                ServerFileEntity(
                    compositeId = "$defaultServerId:pumpkin.toml",
                    serverId = defaultServerId,
                    path = "pumpkin.toml",
                    name = "pumpkin.toml",
                    isDirectory = false,
                    sizeBytes = pumpkinTomlContent.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = pumpkinTomlContent
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:server.properties",
                    serverId = defaultServerId,
                    path = "server.properties",
                    name = "server.properties",
                    isDirectory = false,
                    sizeBytes = serverPropertiesContent.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = serverPropertiesContent
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:whitelist.json",
                    serverId = defaultServerId,
                    path = "whitelist.json",
                    name = "whitelist.json",
                    isDirectory = false,
                    sizeBytes = whitelistContent.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = whitelistContent
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:ops.json",
                    serverId = defaultServerId,
                    path = "ops.json",
                    name = "ops.json",
                    isDirectory = false,
                    sizeBytes = opsContent.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = opsContent
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:plugins/",
                    serverId = defaultServerId,
                    path = "plugins/",
                    name = "plugins",
                    isDirectory = true,
                    sizeBytes = 0,
                    lastModified = System.currentTimeMillis(),
                    content = ""
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:plugins/PumpkinAuth.wasm",
                    serverId = defaultServerId,
                    path = "plugins/PumpkinAuth.wasm",
                    name = "PumpkinAuth.wasm",
                    isDirectory = false,
                    sizeBytes = 145000,
                    lastModified = System.currentTimeMillis(),
                    content = "# Binary WebAssembly Plugin: PumpkinAuth v1.2.0"
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:plugins/FastAsyncChunks.wasm",
                    serverId = defaultServerId,
                    path = "plugins/FastAsyncChunks.wasm",
                    name = "FastAsyncChunks.wasm",
                    isDirectory = false,
                    sizeBytes = 289000,
                    lastModified = System.currentTimeMillis(),
                    content = "# Binary WebAssembly Plugin: FastAsyncChunks v2.0.4"
                ),
                ServerFileEntity(
                    compositeId = "$defaultServerId:world/",
                    serverId = defaultServerId,
                    path = "world/",
                    name = "world",
                    isDirectory = true,
                    sizeBytes = 0,
                    lastModified = System.currentTimeMillis(),
                    content = ""
                )
            )
            database.serverFileDao().insertFiles(initialFiles)

            // Initial cached plugins from the PumpkinMC Market
            database.marketPluginDao().insertPlugins(com.example.data.remote.PumpkinMarketCatalog.fullCatalog)

            // Initial logs
            val initialLogs = listOf(
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:01",
                    level = "INFO",
                    tag = "pumpkin::main",
                    message = "Initializing PumpkinMC v0.1.0 server on Android ARM64..."
                ),
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:02",
                    level = "INFO",
                    tag = "pumpkin::config",
                    message = "Loaded configuration from 'pumpkin.toml' (worker_threads = 4, max_players = 20)"
                ),
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:03",
                    level = "INFO",
                    tag = "pumpkin::net",
                    message = "Binding async TCP listener on 0.0.0.0:25565 (tokio multi-thread runtime)"
                ),
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:04",
                    level = "INFO",
                    tag = "pumpkin::playit",
                    message = "Playit.gg tunnel linked: pumpkin-alpha.playit.gg:25565 [status=ACTIVE, ping=18ms]"
                ),
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:05",
                    level = "INFO",
                    tag = "pumpkin::plugins",
                    message = "Loaded 2 plugins: PumpkinAuth v1.2.0, FastAsyncChunks v2.0.4"
                ),
                ServerLogEntity(
                    serverId = defaultServerId,
                    timestamp = "12:00:06",
                    level = "INFO",
                    tag = "pumpkin::server",
                    message = "Server started in 0.42s! Ready for connections on LAN and Playit.gg [TPS: 20.0]"
                )
            )
            database.serverLogDao().insertLogs(initialLogs)
        }
    }
}
