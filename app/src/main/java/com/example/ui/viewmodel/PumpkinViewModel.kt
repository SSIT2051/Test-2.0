package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.PumpkinServerRepository
import com.example.domain.model.DeviceHardwareInfo
import com.example.domain.model.LiveServerMetrics
import com.example.domain.model.LogEntry
import com.example.domain.model.LogLevel
import com.example.domain.model.MarketPlugin
import com.example.domain.model.ServerConfig
import com.example.domain.model.ServerFileItem
import com.example.domain.model.ServerStatus
import com.example.domain.runtime.DeviceHardwareDetector
import com.example.domain.runtime.PumpkinServerManager
import com.example.service.PumpkinServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class PumpkinViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = PumpkinServerRepository(database)
    val serverManager = PumpkinServerManager(repository, viewModelScope)

    val servers: StateFlow<List<ServerConfig>> = repository.servers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedServerId = MutableStateFlow<String?>(null)
    val selectedServerId: StateFlow<String?> = _selectedServerId.asStateFlow()

    val activeServer: StateFlow<ServerConfig?> = combine(servers, _selectedServerId) { serverList, selectedId ->
        when {
            selectedId != null -> serverList.firstOrNull { it.id == selectedId }
            serverList.isNotEmpty() -> serverList.first().also { _selectedServerId.value = it.id }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeServerMetrics: StateFlow<LiveServerMetrics> = _selectedServerId.flatMapLatest { id ->
        if (id != null) serverManager.getMetrics(id) else flowOf(LiveServerMetrics())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveServerMetrics())

    val activeServerFiles: StateFlow<List<ServerFileItem>> = _selectedServerId.flatMapLatest { id ->
        if (id != null) repository.observeFiles(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServerLogs: StateFlow<List<LogEntry>> = _selectedServerId.flatMapLatest { id ->
        if (id != null) repository.observeLogs(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Market State
    private val _marketCategory = MutableStateFlow("All")
    val marketCategory = _marketCategory.asStateFlow()

    private val _marketSearchQuery = MutableStateFlow("")
    val marketSearchQuery = _marketSearchQuery.asStateFlow()

    private val _isRefreshingMarket = MutableStateFlow(false)
    val isRefreshingMarket = _isRefreshingMarket.asStateFlow()

    val marketPlugins: StateFlow<List<MarketPlugin>> = combine(
        _marketCategory,
        _marketSearchQuery,
        activeServerFiles
    ) { category, query, serverFiles ->
        Triple(category, query, serverFiles)
    }.flatMapLatest { (category, query, serverFiles) ->
        val installedNames = serverFiles.map { it.name.lowercase() }
        repository.observePlugins(
            category = if (category == "All") null else category,
            query = query.ifBlank { null }
        ).combine(flowOf(installedNames)) { plugins, installed ->
            plugins.map { plugin ->
                val baseName = plugin.name.replace(" ", "").lowercase()
                val isInstalled = installed.any { it.contains(baseName) }
                plugin.copy(isInstalled = isInstalled)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // File Editor State
    private val _editingFile = MutableStateFlow<ServerFileItem?>(null)
    val editingFile = _editingFile.asStateFlow()

    // Local IP
    val localWifiIp: String
        get() = repository.getLocalDeviceIp()

    // Device Hardware & Resource Tracking
    private val _deviceHardwareInfo = MutableStateFlow(DeviceHardwareDetector.detect(application))
    val deviceHardwareInfo: StateFlow<DeviceHardwareInfo> = _deviceHardwareInfo.asStateFlow()

    fun refreshHardwareInfo() {
        _deviceHardwareInfo.value = DeviceHardwareDetector.detect(getApplication())
    }

    fun applyRecommendedSettingsToActiveServer() {
        val server = activeServer.value ?: return
        val hw = _deviceHardwareInfo.value
        val updated = server.copy(
            workerThreads = hw.recommendedCores,
            rayonThreads = hw.recommendedCores,
            allocatedRamMb = hw.recommendedRamMb,
            maxStorageMb = hw.recommendedStorageMb,
            viewDistance = hw.recommendedViewDistance,
            simulationDistance = (hw.recommendedViewDistance - 2).coerceAtLeast(4)
        )
        updateServerConfig(updated)
        viewModelScope.launch {
            repository.appendLog(
                serverId = server.id,
                level = LogLevel.INFO,
                tag = "pumpkin::autotune",
                message = "Hardware profile auto-applied: ${hw.socModel} (${hw.cpuCores} cores) -> ${hw.recommendedCores} Tokio workers, ${hw.recommendedRamMb}MB RAM, ${hw.recommendedViewDistance} chunks view distance."
            )
        }
    }

    // App & Server Settings (Battery optimization, crash recovery, crossplay)
    private val prefs = application.getSharedPreferences("pumpkin_settings", android.content.Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(
        com.example.domain.model.AppSettings(
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
        )
    )
    val settings: StateFlow<com.example.domain.model.AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            // Load and sync full official market catalog
            repository.refreshPluginsFromRemote()

            // Check if default server needs auto-starting if state was RUNNING
            servers.collect { list ->
                if (_selectedServerId.value == null && list.isNotEmpty()) {
                    _selectedServerId.value = list.first().id
                }
                list.forEach { server ->
                    if (server.status == ServerStatus.RUNNING) {
                        serverManager.startServer(server)
                        PumpkinServerService.start(
                            context = getApplication(),
                            serverName = server.name,
                            bedrockPort = server.bedrockPort,
                            javaPort = server.port
                        )
                    }
                }
            }
        }
    }

    fun updateSettings(newSettings: com.example.domain.model.AppSettings) {
        _settings.value = newSettings
        prefs.edit().putBoolean("has_seen_onboarding", newSettings.hasSeenOnboarding).apply()
    }

    fun completeOnboarding() {
        updateSettings(_settings.value.copy(hasSeenOnboarding = true))
    }

    fun clearAllTemporaryData() {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.clearLogs(server.id)
            repository.refreshPluginsFromRemote()
        }
    }

    fun selectServer(serverId: String) {
        _selectedServerId.value = serverId
        _editingFile.value = null
    }

    fun startCurrentServer() {
        val server = activeServer.value ?: return
        // Auto-assign dedicated unique tunnel domain and port if default or missing
        val effectiveServer = if (server.playitDomain.isBlank() || server.playitDomain == "playit-free.gl.joinmc.link") {
            val shortId = UUID.randomUUID().toString().take(6).lowercase()
            val nameSlug = server.name.lowercase().filter { it.isLetterOrDigit() }.take(8).ifBlank { "smp" }
            val autoTunnel = "$nameSlug-$shortId.gl.joinmc.link"
            val updated = server.copy(
                playitDomain = autoTunnel,
                playitEnabled = true,
                playitPort = if (server.playitPort == 0) 19132 else server.playitPort
            )
            updateServerConfig(updated)
            updated
        } else {
            server
        }

        serverManager.startServer(effectiveServer)
        PumpkinServerService.start(
            context = getApplication(),
            serverName = effectiveServer.name,
            bedrockPort = effectiveServer.bedrockPort,
            javaPort = effectiveServer.port
        )
    }

    fun stopCurrentServer() {
        val server = activeServer.value ?: return
        serverManager.stopServer(server.id)
        PumpkinServerService.stop(getApplication())
    }

    fun restartCurrentServer() {
        val server = activeServer.value ?: return
        serverManager.restartServer(server)
        PumpkinServerService.start(
            context = getApplication(),
            serverName = server.name,
            bedrockPort = server.bedrockPort,
            javaPort = server.port
        )
    }

    fun executeCommand(command: String) {
        val server = activeServer.value ?: return
        serverManager.executeCommand(server, command)
    }

    fun clearLogs() {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.clearLogs(server.id)
        }
    }

    fun updateServerConfig(updatedConfig: ServerConfig) {
        viewModelScope.launch {
            repository.saveServer(updatedConfig)
        }
    }

    fun createNewServer(
        name: String,
        port: Int,
        version: String = "1.21.4 (Latest LTS)",
        ramMb: Int = 1024,
        cores: Int = 4,
        storageMb: Int = 2048,
        gamemode: String = "survival",
        difficulty: String = "normal",
        onlineMode: Boolean = true,
        motd: String = "Fast PumpkinMC Mobile Server",
        maxPlayers: Int = 20,
        viewDistance: Int = 10,
        simulationDistance: Int = 8,
        pvp: Boolean = true,
        allowFlight: Boolean = false,
        hardcore: Boolean = false
    ) {
        viewModelScope.launch {
            val shortId = UUID.randomUUID().toString().take(6).lowercase()
            val newId = "pumpkin_srv_$shortId"
            val nameSlug = name.lowercase().filter { it.isLetterOrDigit() }.take(8).ifBlank { "smp" }
            val designatedTunnel = "$nameSlug-$shortId.gl.joinmc.link"
            val designatedBedrockPort = 19132

            val newConfig = ServerConfig(
                id = newId,
                name = name,
                port = port,
                serverVersion = version,
                allocatedRamMb = ramMb,
                workerThreads = cores,
                rayonThreads = cores,
                maxStorageMb = storageMb,
                gamemode = gamemode,
                difficulty = difficulty,
                onlineMode = onlineMode,
                motd = motd,
                maxPlayers = maxPlayers,
                viewDistance = viewDistance,
                simulationDistance = simulationDistance,
                pvp = pvp,
                allowFlight = allowFlight,
                hardcore = hardcore,
                bedrockPort = designatedBedrockPort,
                bedrockCrossplayEnabled = true,
                lanModeEnabled = true,
                playitEnabled = true,
                playitDomain = designatedTunnel,
                playitPort = designatedBedrockPort,
                customPort = designatedBedrockPort,
                status = ServerStatus.STOPPED,
                createdAt = System.currentTimeMillis()
            )
            repository.saveServer(newConfig)

            repository.appendLog(
                serverId = newId,
                level = LogLevel.INFO,
                tag = "pumpkin::provision",
                message = "Instance [$name] created. Designated tunnel provisioned: $designatedTunnel (Public IP: 147.185.221.16, Port: $designatedBedrockPort)."
            )

            // Create default files
            val defaultToml = """
                # PumpkinMC Server Config for $name (v$version)
                [server]
                address = "0.0.0.0:$port"
                version = "$version"
                max_players = ${newConfig.maxPlayers}
                view_distance = ${newConfig.viewDistance}
                simulation_distance = ${newConfig.simulationDistance}
                motd = "$motd"
                online_mode = $onlineMode

                [gameplay]
                default_gamemode = "$gamemode"
                difficulty = "$difficulty"
                pvp = $pvp
                allow_flight = $allowFlight
                hardcore = $hardcore

                [networking]
                lan_broadcast = true
                tokio_worker_threads = $cores

                [resources]
                rayon_threads = $cores
                max_storage_mb = $storageMb
            """.trimIndent()

            repository.createFile(newId, "pumpkin.toml", false, defaultToml)
            repository.createFile(newId, "plugins/", true, "")
            repository.createFile(newId, "world/", true, "")

            _selectedServerId.value = newId
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            serverManager.stopServer(serverId)
            repository.deleteServer(serverId)
            val remaining = servers.value.filter { it.id != serverId }
            _selectedServerId.value = remaining.firstOrNull()?.id
        }
    }

    // File Operations
    fun openFileForEditing(file: ServerFileItem) {
        if (!file.isDirectory) {
            _editingFile.value = file
        }
    }

    fun closeFileEditor() {
        _editingFile.value = null
    }

    fun saveFileContent(path: String, content: String) {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.saveFileContent(server.id, path, content)
            _editingFile.update { current ->
                current?.copy(content = content, sizeBytes = content.length.toLong())
            }
        }
    }

    fun createNewFile(path: String, isDirectory: Boolean) {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.createFile(server.id, path, isDirectory, "")
        }
    }

    fun deleteFile(path: String) {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.deleteFile(server.id, path)
            if (_editingFile.value?.path == path) {
                _editingFile.value = null
            }
        }
    }

    // Market Operations
    fun setMarketCategory(category: String) {
        _marketCategory.value = category
    }

    fun setMarketSearchQuery(query: String) {
        _marketSearchQuery.value = query
    }

    fun refreshMarket() {
        viewModelScope.launch {
            _isRefreshingMarket.value = true
            repository.refreshPluginsFromRemote()
            _isRefreshingMarket.value = false
        }
    }

    fun installPlugin(plugin: MarketPlugin) {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.installPluginToServer(server.id, plugin)
        }
    }

    fun uninstallPlugin(pluginName: String) {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            repository.uninstallPlugin(server.id, pluginName)
        }
    }
}
