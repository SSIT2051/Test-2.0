package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceHardwareInfo
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateServerDialog(
    hardwareInfo: DeviceHardwareInfo? = null,
    onDismiss: () -> Unit,
    onCreateServer: (
        name: String,
        port: Int,
        version: String,
        ramMb: Int,
        cores: Int,
        storageMb: Int,
        gamemode: String,
        difficulty: String,
        onlineMode: Boolean,
        motd: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("My Minecraft Server") }
    var portText by remember { mutableStateOf("25565") }
    var selectedVersion by remember { mutableStateOf("26.3 (Latest 2026 Release)") }
    var versionExpanded by remember { mutableStateOf(false) }

    var selectedCores by remember { mutableIntStateOf(hardwareInfo?.recommendedCores ?: 4) }
    var ramMb by remember { mutableIntStateOf(hardwareInfo?.recommendedRamMb ?: 1024) }
    var storageMb by remember { mutableIntStateOf(hardwareInfo?.recommendedStorageMb ?: 2048) }
    var gamemode by remember { mutableStateOf("survival") }
    var difficulty by remember { mutableStateOf("normal") }
    var onlineMode by remember { mutableStateOf(true) }
    var motd by remember { mutableStateOf("Fast PumpkinMC Mobile Server") }

    val versions = listOf(
        "26.3 (Latest 2026 Release)",
        "26.2 (Winter Update)",
        "26w Snapshot (Bleeding Edge)",
        "1.21.4 (Latest LTS)",
        "1.21.1 (Stable)",
        "1.20.6 (Tricky Trials)",
        "1.20.4 (Prior LTS)",
        "1.19.4 (Wild Update)",
        "1.16.5 (Legacy Nether)"
    )
    val gamemodes = listOf("survival", "creative", "adventure")
    val ramOptions = listOf(256, 512, 1024, 2048, 4096)
    val coreOptions = listOf(1, 2, 4, 6, 8)
    val storageOptions = listOf(1024, 2048, 4096)

    var gamemodeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianDark,
        title = {
            Column {
                Text(
                    text = "DEPLOY PUMPKINMC SERVER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Configure Instance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PumpkinOrange,
                        unfocusedBorderColor = ObsidianSurfaceBorder,
                        focusedLabelColor = PumpkinOrange,
                        cursorColor = PumpkinOrange,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Version & Port Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Minecraft Version Dropdown
                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = !versionExpanded },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = selectedVersion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("MC Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PumpkinOrange,
                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                focusedLabelColor = PumpkinOrange,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = versionExpanded,
                            onDismissRequest = { versionExpanded = false },
                            modifier = Modifier.background(ObsidianSurface)
                        ) {
                            versions.forEach { ver ->
                                DropdownMenuItem(
                                    text = { Text(ver, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        selectedVersion = ver
                                        versionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Port
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PumpkinOrange,
                            unfocusedBorderColor = ObsidianSurfaceBorder,
                            focusedLabelColor = PumpkinOrange,
                            cursorColor = PumpkinOrange,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Port reminder for Bedrock Mobile and Java PC
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PumpkinOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bedrock (Mobile) port: 19132 • Java (PC) port: $portText",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Option to tune settings based on device resources
                if (hardwareInfo != null) {
                    val isAutoTuned = selectedCores == hardwareInfo.recommendedCores &&
                            ramMb == hardwareInfo.recommendedRamMb &&
                            storageMb == hardwareInfo.recommendedStorageMb

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAutoTuned) PumpkinOrange.copy(alpha = 0.12f) else ObsidianSurfaceElevated)
                            .border(1.dp, if (isAutoTuned) PumpkinOrange.copy(alpha = 0.5f) else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCores = hardwareInfo.recommendedCores
                                ramMb = hardwareInfo.recommendedRamMb
                                storageMb = hardwareInfo.recommendedStorageMb
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Auto-Tune to Device Resources",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PumpkinOrange
                                    )
                                    Text(
                                        text = "${hardwareInfo.manufacturer} ${hardwareInfo.deviceModel} • Rec: ${hardwareInfo.recommendedCores}C, ${if (hardwareInfo.recommendedRamMb >= 1024) "${hardwareInfo.recommendedRamMb / 1024}GB" else "${hardwareInfo.recommendedRamMb}MB"} RAM",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isAutoTuned) PumpkinOrange else ObsidianSurface)
                                    .border(1.dp, if (isAutoTuned) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isAutoTuned) "MATCHED" else "AUTO-TUNE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoTuned) Color.Black else PumpkinOrange
                                )
                            }
                        }
                    }
                }

                // CPU Multi-Threading & Core Allocation
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CPU Cores / Threads", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Text("$selectedCores Cores (Tokio + Rayon)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        coreOptions.forEach { cores ->
                            val isSelected = selectedCores == cores
                            val isRec = hardwareInfo != null && cores == hardwareInfo.recommendedCores
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                    .border(1.dp, if (isSelected) PumpkinOrange else if (isRec) PumpkinOrange.copy(alpha = 0.5f) else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCores = cores }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (cores == 1) "1 Core" else "$cores Cores",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                    if (isRec) {
                                        Text(
                                            text = "★ Best",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else PumpkinOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = PumpkinOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Designated tunnel domain, public IP, and port will be generated automatically.",
                                fontSize = 10.sp,
                                color = PumpkinOrange
                            )
                        }
                    }
                }

                // RAM Allocation Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allocated RAM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (ramMb >= 1024) "${ramMb / 1024} GB ($ramMb MB)" else "$ramMb MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ramOptions.forEach { mb ->
                            val isSelected = ramMb == mb
                            val isRec = hardwareInfo != null && mb == hardwareInfo.recommendedRamMb
                            val label = if (mb >= 1024) "${mb / 1024}GB" else "${mb}MB"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                    .border(1.dp, if (isSelected) PumpkinOrange else if (isRec) PumpkinOrange.copy(alpha = 0.5f) else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { ramMb = mb }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRec) "$label ★" else label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else if (isRec) PumpkinOrange else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Storage Allocation Quota Chips
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Max Storage Quota", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Text("${storageMb / 1024} GB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        storageOptions.forEach { mb ->
                            val isSelected = storageMb == mb
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                    .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { storageMb = mb }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mb / 1024} GB Quota",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Gamemode & Online Mode
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = gamemodeExpanded,
                        onExpandedChange = { gamemodeExpanded = !gamemodeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = gamemode.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gamemode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gamemodeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PumpkinOrange,
                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                focusedLabelColor = PumpkinOrange,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = gamemodeExpanded,
                            onDismissRequest = { gamemodeExpanded = false },
                            modifier = Modifier.background(ObsidianSurface)
                        ) {
                            gamemodes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.replaceFirstChar { it.uppercase() }, color = TextPrimary) },
                                    onClick = {
                                        gamemode = mode
                                        gamemodeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Online Mode (Mojang Authentication)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Online Mode (Mojang Auth)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(if (onlineMode) "Official accounts only" else "Allows cracked & offline clients", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = onlineMode,
                        onCheckedChange = { onlineMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PumpkinOrange,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurfaceElevated
                        )
                    )
                }

                // MOTD
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    label = { Text("MOTD (Server Description)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PumpkinOrange,
                        unfocusedBorderColor = ObsidianSurfaceBorder,
                        focusedLabelColor = PumpkinOrange,
                        cursorColor = PumpkinOrange,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 25565
                    onCreateServer(
                        name.ifBlank { "Pumpkin Server" },
                        port,
                        selectedVersion,
                        ramMb,
                        selectedCores,
                        storageMb,
                        gamemode,
                        difficulty,
                        onlineMode,
                        motd
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PumpkinOrange,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Deploy Server", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianSurfaceBorder)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
