package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import com.example.ui.components.LogoVariant
import com.example.ui.components.PumpkinLogo
import com.example.ui.components.UserGuideDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettings
import com.example.domain.model.DeviceHardwareInfo
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    hardwareInfo: DeviceHardwareInfo? = null,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearCacheAndLogs: () -> Unit,
    onForceSyncMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var draftSettings by remember(settings) { mutableStateOf(settings) }
    val isDirty = draftSettings != settings
    var showUserGuideDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = if (isDirty) 80.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Battery & Thermal Protection
            SettingsGroup(title = "BATTERY & THERMAL GUARD") {
                SettingsRow(
                    title = "Run in Background (CPU WakeLock)",
                    subtitle = "Prevents Android OS from killing the Tokio runtime when screen turns off",
                    checked = draftSettings.cpuWakeLockEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(cpuWakeLockEnabled = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Thermal Protection Guard",
                    subtitle = "Throttles tickrate if phone reaches 42°C to prevent battery degradation",
                    checked = draftSettings.thermalProtectionEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(thermalProtectionEnabled = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Low-Power Idle Mode",
                    subtitle = "Reduces tickrate to 15 TPS when 0 players are connected",
                    checked = draftSettings.lowPowerIdleMode,
                    onCheckedChange = { draftSettings = draftSettings.copy(lowPowerIdleMode = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Auto-Restart on Crash",
                    subtitle = "Reboots the server automatically if a panic or signal occurs",
                    checked = draftSettings.autoRestartOnCrash,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoRestartOnCrash = it) }
                )
            }

            // Memory & Hardware Footprint Optimization
            SettingsGroup(title = "HARDWARE & MEMORY OPTIMIZATION") {
                SettingsRow(
                    title = "Rayon Multi-Threaded Chunk Engine",
                    subtitle = "Parallelizes terrain generation across all mobile CPU cores",
                    checked = draftSettings.multiThreadRayonEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(multiThreadRayonEnabled = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Compact Memory Allocator (jemalloc)",
                    subtitle = "Prevents fragmentation and lowers heap consumption on 2GB RAM devices",
                    checked = draftSettings.compactMemoryAllocator,
                    onCheckedChange = { draftSettings = draftSettings.copy(compactMemoryAllocator = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Auto-Clear Crash Dumps",
                    subtitle = "Prunes old core dumps and panic traces to preserve internal flash storage",
                    checked = draftSettings.autoClearCrashDumps,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoClearCrashDumps = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Auto-Vacuum Database & Trim Logs",
                    subtitle = "Periodically compresses Room SQLite database to save storage",
                    checked = draftSettings.vacuumDbOnExit,
                    onCheckedChange = { draftSettings = draftSettings.copy(vacuumDbOnExit = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Automatic World Snapshot",
                    subtitle = "Creates a fast lightweight snapshot of region files before server boot",
                    checked = draftSettings.autoBackupBeforeStart,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoBackupBeforeStart = it) }
                )
            }

            // Crossplay & Network Compatibility
            SettingsGroup(title = "CROSSPLAY & MULTIPLAYER") {
                SettingsRow(
                    title = "Bedrock Crossplay (Geyser Protocol)",
                    subtitle = "Translates Bedrock UDP packets so mobile phone players can join Java server",
                    checked = draftSettings.bedrockCrossplayEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(bedrockCrossplayEnabled = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Quick LAN Broadcast",
                    subtitle = "Automatically announces server to nearby friends on the same WiFi network",
                    checked = draftSettings.quickLanBroadcast,
                    onCheckedChange = { draftSettings = draftSettings.copy(quickLanBroadcast = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Player Join & Leave Alerts",
                    subtitle = "Sends high-priority Android notification when friends join your server",
                    checked = draftSettings.playerJoinAlerts,
                    onCheckedChange = { draftSettings = draftSettings.copy(playerJoinAlerts = it) }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Auto-Protocol Handshake (ViaVersion)",
                    subtitle = "Allows legacy and newer Minecraft versions to join seamlessly",
                    checked = draftSettings.protocolAutoNegotiate,
                    onCheckedChange = { draftSettings = draftSettings.copy(protocolAutoNegotiate = it) }
                )
            }

            // Storage & Cache Maintenance
            SettingsGroup(title = "STORAGE MAINTENANCE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Logs & Temp Cache", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("Deletes temporary session logs and frees storage", fontSize = 12.sp, color = TextMuted)
                    }

                    Button(
                        onClick = {
                            onClearCacheAndLogs()
                            Toast.makeText(context, "Logs and cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sync Market Plugins", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("Fetch latest verified plugins from market.pumpkinmc.org", fontSize = 12.sp, color = TextMuted)
                    }

                    Button(
                        onClick = {
                            onForceSyncMarket()
                            Toast.makeText(context, "Plugin catalog synced", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Detailed User Guide & Manual", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("In-depth walkthrough of all features, network options & console", fontSize = 12.sp, color = TextMuted)
                    }

                    Button(
                        onClick = { showUserGuideDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Live Device Hardware & Telemetry Group
            if (hardwareInfo != null) {
                SettingsGroup(title = "DEVICE HARDWARE & CAPACITIES") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chipset / SoC Model", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("${hardwareInfo.manufacturer} ${hardwareInfo.deviceModel} • ${hardwareInfo.abi}", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(hardwareInfo.socModel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PumpkinOrange)
                    }

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CPU Cores & Threads", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Rec. Allocation: ${hardwareInfo.recommendedCores} cores for server", fontSize = 12.sp, color = TextMuted)
                        }
                        Text("${hardwareInfo.cpuCores} Physical Cores", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Device RAM (Memory)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Free: ${hardwareInfo.availableRamMb} MB / Total: ${hardwareInfo.totalRamMb} MB", fontSize = 12.sp, color = TextMuted)
                        }
                        Text("${"%.0f".format(hardwareInfo.ramUsagePercent)}% used", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (hardwareInfo.ramUsagePercent > 80f) Color(0xFFFF5252) else PumpkinOrange)
                    }

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Internal Flash Storage", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Free: ${"%.1f".format(hardwareInfo.freeStorageGb)} GB / Total: ${"%.1f".format(hardwareInfo.totalStorageGb)} GB", fontSize = 12.sp, color = TextMuted)
                        }
                        Text("${"%.0f".format(hardwareInfo.storageUsagePercent)}% used", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PumpkinOrange)
                    }

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calculated Device Tier", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Rec. View Distance: ${hardwareInfo.recommendedViewDistance} Chunks", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(hardwareInfo.deviceTierName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                    }
                }
            }

            // About & Identity Section with Distinctive Vector Logo
            SettingsGroup(title = "ABOUT & CREDITS") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PumpkinLogo(
                        size = 48.dp,
                        showGlow = true,
                        variant = LogoVariant.CREST
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PumpkinMC Host", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PumpkinOrange.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ALPHA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Made with ❤️ by SSIT • Engine: PumpkinMC (Rust)", fontSize = 12.sp, color = TextMuted)
                        Text("World's first native ARM64 Minecraft host for Android", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Floating Save / Discard Bar for Settings
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.5.dp, PumpkinOrange, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unsaved Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PumpkinOrange
                        )
                        Text(
                            text = "Modifications won't apply until saved",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { draftSettings = settings },
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
                                onUpdateSettings(draftSettings)
                                Toast.makeText(context, "App settings saved & applied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PumpkinOrange,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showUserGuideDialog) {
            UserGuideDialog(
                onDismiss = { showUserGuideDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = PumpkinOrange,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurfaceElevated
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(ObsidianSurfaceBorder)
    )
}
