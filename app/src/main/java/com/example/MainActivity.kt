package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CreateServerDialog
import com.example.ui.components.LogoVariant
import com.example.ui.components.PumpkinLogo
import com.example.ui.components.ServerSwitcherSheet
import com.example.ui.components.StatusBadge
import com.example.ui.screens.ConsoleScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.PluginMarketScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PumpkinViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: PumpkinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PumpkinMCApp(viewModel)
            }
        }
    }
}

sealed class ScreenNav(val title: String, val icon: ImageVector) {
    object Dashboard : ScreenNav("Server", Icons.Default.Speed)
    object Console : ScreenNav("Console", Icons.Default.Terminal)
    object Market : ScreenNav("Market", Icons.Default.Storefront)
    object Files : ScreenNav("Files", Icons.Default.FolderOpen)
    object Settings : ScreenNav("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpkinMCApp(viewModel: PumpkinViewModel) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val selectedServerId by viewModel.selectedServerId.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val metrics by viewModel.activeServerMetrics.collectAsStateWithLifecycle()
    val files by viewModel.activeServerFiles.collectAsStateWithLifecycle()
    val logs by viewModel.activeServerLogs.collectAsStateWithLifecycle()
    val editingFile by viewModel.editingFile.collectAsStateWithLifecycle()

    val marketPlugins by viewModel.marketPlugins.collectAsStateWithLifecycle()
    val marketCategory by viewModel.marketCategory.collectAsStateWithLifecycle()
    val marketSearchQuery by viewModel.marketSearchQuery.collectAsStateWithLifecycle()
    val isRefreshingMarket by viewModel.isRefreshingMarket.collectAsStateWithLifecycle()
    val appSettings by viewModel.settings.collectAsStateWithLifecycle()
    val hardwareInfo by viewModel.deviceHardwareInfo.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showServerSheet by remember { mutableStateOf(false) }
    var showCreateServerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(perm)
            }
        }
    }

    val navItems = listOf(
        ScreenNav.Dashboard,
        ScreenNav.Console,
        ScreenNav.Market,
        ScreenNav.Files,
        ScreenNav.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showServerSheet = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        PumpkinLogo(
                            size = 28.dp,
                            showGlow = false,
                            variant = LogoVariant.COMPACT
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeServer?.name ?: "Pumpkin Host",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Server",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceElevated)
                            .border(1.dp, PumpkinOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { showCreateServerDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Server",
                                tint = PumpkinOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    activeServer?.let { srv ->
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            StatusBadge(status = srv.status)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            if (editingFile == null) {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(1.dp, ObsidianSurfaceBorder)
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PumpkinOrange,
                                selectedTextColor = PumpkinOrange,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = ObsidianSurfaceElevated
                            )
                        )
                    }
                }
            }
        },
        containerColor = ObsidianDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> DashboardScreen(
                    server = activeServer,
                    metrics = metrics,
                    localWifiIp = viewModel.localWifiIp,
                    hardwareInfo = hardwareInfo,
                    onStart = { viewModel.startCurrentServer() },
                    onStop = { viewModel.stopCurrentServer() },
                    onRestart = { viewModel.restartCurrentServer() },
                    onUpdateConfig = { viewModel.updateServerConfig(it) },
                    onApplyHardwareRecommendation = { viewModel.applyRecommendedSettingsToActiveServer() },
                    onCreateNewServer = { showCreateServerDialog = true },
                    onNavigateToConsole = { selectedTabIndex = 1 },
                    onNavigateToSettings = { selectedTabIndex = 4 }
                )
                1 -> ConsoleScreen(
                    server = activeServer,
                    logs = logs,
                    onSendCommand = { viewModel.executeCommand(it) },
                    onClearLogs = { viewModel.clearLogs() }
                )
                2 -> PluginMarketScreen(
                    server = activeServer,
                    plugins = marketPlugins,
                    selectedCategory = marketCategory,
                    searchQuery = marketSearchQuery,
                    isRefreshing = isRefreshingMarket,
                    onSelectCategory = { viewModel.setMarketCategory(it) },
                    onSearchQueryChange = { viewModel.setMarketSearchQuery(it) },
                    onRefresh = { viewModel.refreshMarket() },
                    onInstallPlugin = { viewModel.installPlugin(it) },
                    onUninstallPlugin = { viewModel.uninstallPlugin(it) }
                )
                3 -> FileManagerScreen(
                    server = activeServer,
                    files = files,
                    editingFile = editingFile,
                    onOpenFile = { viewModel.openFileForEditing(it) },
                    onCloseEditor = { viewModel.closeFileEditor() },
                    onSaveFile = { path, content -> viewModel.saveFileContent(path, content) },
                    onCreateFile = { path, isDir -> viewModel.createNewFile(path, isDir) },
                    onDeleteFile = { path -> viewModel.deleteFile(path) }
                )
                4 -> SettingsScreen(
                    settings = appSettings,
                    hardwareInfo = hardwareInfo,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onClearCacheAndLogs = { viewModel.clearAllTemporaryData() },
                    onForceSyncMarket = { viewModel.refreshMarket() }
                )
            }
        }
    }

    // Server Selection Modal Bottom Sheet
    if (showServerSheet) {
        ServerSwitcherSheet(
            sheetState = sheetState,
            servers = servers,
            selectedServerId = selectedServerId,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showServerSheet = false
                }
            },
            onSelectServer = { serverId ->
                viewModel.selectServer(serverId)
            },
            onOpenCreateDialog = {
                showCreateServerDialog = true
            },
            onDeleteServer = { serverId ->
                viewModel.deleteServer(serverId)
            }
        )
    }

    // Create New Server Dialog
    if (showCreateServerDialog) {
        CreateServerDialog(
            hardwareInfo = hardwareInfo,
            onDismiss = { showCreateServerDialog = false },
            onCreateServer = { name, port, version, ramMb, cores, storageMb, gamemode, difficulty, onlineMode, motd ->
                viewModel.createNewServer(
                    name = name,
                    port = port,
                    version = version,
                    ramMb = ramMb,
                    cores = cores,
                    storageMb = storageMb,
                    gamemode = gamemode,
                    difficulty = difficulty,
                    onlineMode = onlineMode,
                    motd = motd
                )
                showCreateServerDialog = false
            }
        )
    }
}
