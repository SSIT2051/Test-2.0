package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceHardwareInfo
import com.example.domain.model.ServerConfig
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

data class TuningOption<T>(val value: T, val title: String, val subtitle: String)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CoreTuningCard(
    draftServer: ServerConfig,
    hardwareInfo: DeviceHardwareInfo?,
    onServerChange: (ServerConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var versionMenuExpanded by remember { mutableStateOf(false) }

    val ramOptions = listOf(
        TuningOption(256, "256 MB", "Ultra-Low"),
        TuningOption(512, "512 MB", "Budget"),
        TuningOption(1024, "1 GB", "Standard"),
        TuningOption(2048, "2 GB", "High Perf"),
        TuningOption(4096, "4 GB", "Max Cache")
    )

    val workerOptions = listOf(
        TuningOption(1, "1 Core", "Low CPU"),
        TuningOption(2, "2 Cores", "Balanced"),
        TuningOption(4, "4 Cores", "Optimal"),
        TuningOption(6, "6 Cores", "Heavy I/O"),
        TuningOption(8, "8 Cores", "Max Speed")
    )

    val rayonOptions = listOf(
        TuningOption(1, "1 C", "Single"),
        TuningOption(2, "2 C", "Balanced"),
        TuningOption(4, "4 C", "Fast Gen"),
        TuningOption(6, "6 C", "Heavy Gen"),
        TuningOption(8, "8 C", "Instant")
    )

    val storageOptions = listOf(
        TuningOption(1024, "1 GB", "Compact"),
        TuningOption(2048, "2 GB", "Standard"),
        TuningOption(4096, "4 GB", "Extended"),
        TuningOption(8192, "8 GB", "Huge World")
    )

    val autoSaveOptions = listOf(
        TuningOption(1, "1 min", "Real-time"),
        TuningOption(5, "5 min", "Default"),
        TuningOption(10, "10 min", "Balanced"),
        TuningOption(15, "15 min", "Low I/O"),
        TuningOption(30, "30 min", "Minimal")
    )

    val versions = listOf(
        "1.21.4 (Latest LTS)",
        "1.21.1 (Stable)",
        "1.20.6 (Tricky Trials)",
        "1.20.4 (Prior LTS)",
        "26.3 (Latest Release)",
        "1.19.4 (Wild Update)"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CORE & PERFORMANCE TUNING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                if (hardwareInfo != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PumpkinOrange.copy(alpha = 0.15f))
                            .clickable {
                                onServerChange(
                                    draftServer.copy(
                                        allocatedRamMb = hardwareInfo.recommendedRamMb,
                                        workerThreads = hardwareInfo.recommendedCores,
                                        rayonThreads = hardwareInfo.recommendedCores,
                                        maxStorageMb = hardwareInfo.recommendedStorageMb
                                    )
                                )
                                Toast.makeText(context, "SoC auto-tuned for ${hardwareInfo.deviceModel}!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Tune SoC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                        }
                    }
                }
            }

            // Engine Target Version Dropdown
            ExposedDropdownMenuBox(
                expanded = versionMenuExpanded,
                onExpandedChange = { versionMenuExpanded = !versionMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draftServer.serverVersion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Minecraft Target Version") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PumpkinOrange,
                        unfocusedBorderColor = ObsidianSurfaceBorder,
                        focusedLabelColor = PumpkinOrange,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = versionMenuExpanded,
                    onDismissRequest = { versionMenuExpanded = false },
                    modifier = Modifier.background(ObsidianSurface)
                ) {
                    versions.forEach { ver ->
                        DropdownMenuItem(
                            text = { Text(ver, color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                onServerChange(draftServer.copy(serverVersion = ver))
                                versionMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Allocated RAM Chips
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allocated RAM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = if (draftServer.allocatedRamMb >= 1024) "${draftServer.allocatedRamMb / 1024} GB (${draftServer.allocatedRamMb} MB)" else "${draftServer.allocatedRamMb} MB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PumpkinOrange
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ramOptions.forEach { opt ->
                        val isSelected = draftServer.allocatedRamMb == opt.value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(allocatedRamMb = opt.value)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opt.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Tokio Worker Threads (Async I/O)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tokio Workers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text("${draftServer.workerThreads} Cores (Async Network I/O)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    workerOptions.forEach { opt ->
                        val isSelected = draftServer.workerThreads == opt.value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(workerThreads = opt.value)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opt.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Rayon Threads (Parallel Chunks)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rayon Threads", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${draftServer.rayonThreads} Cores (Chunk Generation)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rayonOptions.forEach { opt ->
                        val isSelected = draftServer.rayonThreads == opt.value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(rayonThreads = opt.value)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opt.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Storage Quota
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Storage Limit Quota", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text("${draftServer.maxStorageMb / 1024} GB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    storageOptions.forEach { opt ->
                        val isSelected = draftServer.maxStorageMb == opt.value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(maxStorageMb = opt.value)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opt.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // World Auto-Save Interval
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("World Auto-Save Interval", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${draftServer.autoSaveMinutes} min", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    autoSaveOptions.forEach { opt ->
                        val isSelected = draftServer.autoSaveMinutes == opt.value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(autoSaveMinutes = opt.value)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = opt.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
