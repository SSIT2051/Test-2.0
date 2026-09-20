package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ServerConfig
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class GameRuleOption(val value: String, val title: String, val subtitle: String)

@Composable
fun WorldGameplayCard(
    draftServer: ServerConfig,
    onServerChange: (ServerConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val gamemodes = listOf(
        GameRuleOption("survival", "Survival", "Standard"),
        GameRuleOption("creative", "Creative", "Free Build"),
        GameRuleOption("adventure", "Adventure", "No Break"),
        GameRuleOption("spectator", "Spectator", "Noclip")
    )

    val difficulties = listOf(
        GameRuleOption("peaceful", "Peaceful", "No Mobs"),
        GameRuleOption("easy", "Easy", "Low Dmg"),
        GameRuleOption("normal", "Normal", "Standard"),
        GameRuleOption("hard", "Hard", "Starvation")
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
            Text(
                text = "WORLD & GAMEPLAY RULES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )

            // Max Players Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max Players Allowed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${draftServer.maxPlayers} Players", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Slider(
                    value = draftServer.maxPlayers.toFloat(),
                    onValueChange = { onServerChange(draftServer.copy(maxPlayers = it.toInt())) },
                    valueRange = 2f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = PumpkinOrange,
                        activeTrackColor = PumpkinOrange,
                        inactiveTrackColor = ObsidianSurfaceBorder
                    )
                )
            }

            // Gamemode Chips
            Column {
                Text("Default Gamemode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    gamemodes.forEach { opt ->
                        val isSelected = draftServer.gamemode.equals(opt.value, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(gamemode = opt.value)) }
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

            // Difficulty Chips
            Column {
                Text("World Difficulty", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    difficulties.forEach { opt ->
                        val isSelected = draftServer.difficulty.equals(opt.value, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onServerChange(draftServer.copy(difficulty = opt.value)) }
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

            // View Distance Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("View Distance", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${draftServer.viewDistance} Chunks (Render Radius)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Slider(
                    value = draftServer.viewDistance.toFloat(),
                    onValueChange = { onServerChange(draftServer.copy(viewDistance = it.toInt())) },
                    valueRange = 4f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = PumpkinOrange,
                        activeTrackColor = PumpkinOrange,
                        inactiveTrackColor = ObsidianSurfaceBorder
                    )
                )
            }

            // Simulation Distance Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Simulation Distance", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${draftServer.simulationDistance} Chunks (Tick Radius)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                }
                Slider(
                    value = draftServer.simulationDistance.toFloat(),
                    onValueChange = { onServerChange(draftServer.copy(simulationDistance = it.toInt())) },
                    valueRange = 4f..16f,
                    colors = SliderDefaults.colors(
                        thumbColor = PumpkinOrange,
                        activeTrackColor = PumpkinOrange,
                        inactiveTrackColor = ObsidianSurfaceBorder
                    )
                )
            }

            // Switches with clear explanations right in their rows
            RuleToggleRow(
                title = "PvP Combat",
                subtitle = "Enable player-versus-player damage & attacks",
                checked = draftServer.pvp,
                onCheckedChange = { onServerChange(draftServer.copy(pvp = it)) }
            )

            RuleToggleRow(
                title = "Online Mode (Mojang Auth)",
                subtitle = if (draftServer.onlineMode) "Enforce official accounts only" else "Allow offline & cracked client joins",
                checked = draftServer.onlineMode,
                onCheckedChange = { onServerChange(draftServer.copy(onlineMode = it)) }
            )

            RuleToggleRow(
                title = "Allow Flight",
                subtitle = "Permit survival flight without server auto-kick",
                checked = draftServer.allowFlight,
                onCheckedChange = { onServerChange(draftServer.copy(allowFlight = it)) }
            )

            RuleToggleRow(
                title = "Hardcore Mode",
                subtitle = "Single life: permanent ban upon player death",
                checked = draftServer.hardcore,
                onCheckedChange = { onServerChange(draftServer.copy(hardcore = it)) }
            )

            RuleToggleRow(
                title = "Bedrock Crossplay (UDP)",
                subtitle = "Geyser/RakNet bridge for iOS, Android & consoles",
                checked = draftServer.bedrockCrossplayEnabled,
                onCheckedChange = { onServerChange(draftServer.copy(bedrockCrossplayEnabled = it)) }
            )

            RuleToggleRow(
                title = "LAN Discovery Broadcast",
                subtitle = "Announce server automatically on local Wi-Fi",
                checked = draftServer.lanModeEnabled,
                onCheckedChange = { onServerChange(draftServer.copy(lanModeEnabled = it)) }
            )
        }
    }
}

@Composable
private fun RuleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = PumpkinOrange,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurface
            )
        )
    }
}
