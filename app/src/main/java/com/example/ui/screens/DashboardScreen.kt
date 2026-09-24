package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceHardwareInfo
import com.example.domain.model.LiveServerMetrics
import com.example.domain.model.ServerConfig
import com.example.domain.model.ServerStatus
import com.example.ui.components.UserGuideDialog
import com.example.ui.screens.dashboard.ConnectionTunnelCard
import com.example.ui.screens.dashboard.CoreTuningCard
import com.example.ui.screens.dashboard.ServerHeaderCard
import com.example.ui.screens.dashboard.ServerIdentityCard
import com.example.ui.screens.dashboard.WorldGameplayCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    server: ServerConfig?,
    metrics: LiveServerMetrics,
    localWifiIp: String,
    isClaimLoading: Boolean = false,
    isAccountLinked: Boolean = false,
    playitEndpoints: com.example.domain.tunnel.playit.PlayitEndpoints? = null,
    onRequestClaim: ((String) -> Unit) -> Unit = {},
    onUnlinkAccount: () -> Unit = {},
    onRetryTunnel: () -> Unit = {},
    hardwareInfo: DeviceHardwareInfo? = null,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onUpdateConfig: (ServerConfig) -> Unit,
    onApplyHardwareRecommendation: () -> Unit = {},
    onCreateNewServer: () -> Unit = {},
    onNavigateToConsole: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (server == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, PumpkinOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "NO SERVER DEPLOYED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Create and run your native ARM64 PumpkinMC server directly on this device.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onCreateNewServer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PumpkinOrange,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Server", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val isRunning = server.status == ServerStatus.RUNNING

    var draftServer by remember(server.id) { mutableStateOf(server) }
    var showUserGuide by remember { mutableStateOf(false) }

    LaunchedEffect(server.id) {
        draftServer = server
    }

    LaunchedEffect(server.playitDomain, server.playitPort, server.status) {
        draftServer = draftServer.copy(
            playitDomain = server.playitDomain,
            playitPort = server.playitPort,
            status = server.status
        )
    }

    val isDirty = draftServer.name != server.name ||
        draftServer.motd != server.motd ||
        draftServer.port != server.port ||
        draftServer.serverVersion != server.serverVersion ||
        draftServer.allocatedRamMb != server.allocatedRamMb ||
        draftServer.workerThreads != server.workerThreads ||
        draftServer.rayonThreads != server.rayonThreads ||
        draftServer.maxStorageMb != server.maxStorageMb ||
        draftServer.autoSaveMinutes != server.autoSaveMinutes ||
        draftServer.maxPlayers != server.maxPlayers ||
        draftServer.viewDistance != server.viewDistance ||
        draftServer.simulationDistance != server.simulationDistance ||
        draftServer.gamemode != server.gamemode ||
        draftServer.difficulty != server.difficulty ||
        draftServer.onlineMode != server.onlineMode ||
        draftServer.pvp != server.pvp ||
        draftServer.allowFlight != server.allowFlight ||
        draftServer.hardcore != server.hardcore ||
        draftServer.lanModeEnabled != server.lanModeEnabled ||
        draftServer.bedrockCrossplayEnabled != server.bedrockCrossplayEnabled ||
        draftServer.bedrockPort != server.bedrockPort ||
        draftServer.customTunnelEnabled != server.customTunnelEnabled ||
        draftServer.customTunnelType != server.customTunnelType ||
        draftServer.customTunnelAddress != server.customTunnelAddress ||
        draftServer.customPort != server.customPort ||
        draftServer.playitDomain != server.playitDomain ||
        draftServer.playitPort != server.playitPort ||
        draftServer.playitEnabled != server.playitEnabled

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = if (isDirty) 88.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Server Header & Immediate Power Card
            ServerHeaderCard(
                draftServer = draftServer,
                serverStatus = server.status,
                metrics = metrics,
                isRunning = isRunning,
                onStart = onStart,
                onStop = onStop,
                onRestart = onRestart
            )

            // 2. Connection & Tunnel Hub (With Auto Designated & Custom Tunnel Mode)
            ConnectionTunnelCard(
                draftServer = draftServer,
                isRunning = isRunning,
                localWifiIp = localWifiIp,
                isClaimLoading = isClaimLoading,
                isAccountLinked = isAccountLinked,
                playitEndpoints = playitEndpoints,
                onRequestClaim = onRequestClaim,
                onUnlinkAccount = onUnlinkAccount,
                onRetryTunnel = onRetryTunnel,
                onServerChange = { draftServer = it },
                onOpenManual = { showUserGuide = true }
            )

            // 3. Core Performance & System Tuning
            CoreTuningCard(
                draftServer = draftServer,
                hardwareInfo = hardwareInfo,
                onServerChange = { draftServer = it }
            )

            // 4. World & Gameplay Customizations
            WorldGameplayCard(
                draftServer = draftServer,
                onServerChange = { draftServer = it }
            )

            // 5. Server Identity & Ports
            ServerIdentityCard(
                draftServer = draftServer,
                onServerChange = { draftServer = it }
            )
        }

        // Floating Save / Discard Bar (Appears when any config change is made)
        AnimatedVisibility(
            visible = isDirty,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.5.dp, PumpkinOrange, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unsaved Changes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PumpkinOrange
                        )
                        Text(
                            text = "Tap Save to apply customizations",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { draftServer = server },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurface,
                                contentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Discard", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onUpdateConfig(draftServer)
                                Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PumpkinOrange,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Clean User Manual Modal
        if (showUserGuide) {
            UserGuideDialog(onDismiss = { showUserGuide = false })
        }
    }
}
