package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class GuideSection(
    val title: String,
    val icon: ImageVector,
    val summary: String,
    val subsections: List<GuideTopic>
)

data class GuideTopic(
    val subtitle: String,
    val description: String,
    val tip: String? = null
)

@Composable
fun UserGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSectionIndex by remember { mutableIntStateOf(0) }

    val sections = remember {
        listOf(
            GuideSection(
                title = "Quick Start",
                icon = Icons.Default.RocketLaunch,
                summary = "Get your native ARM64 Minecraft server online in less than 30 seconds.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "1. Creating Your First Server",
                        description = "Tap the '+' icon on the top navigation bar or the 'Create Server' button on the Dashboard. Choose a custom name, game port (default 25565), Minecraft version (1.21.4), gamemode, and memory budget. PumpkinMC Host automatically creates the directory structure and default server.toml configuration.",
                        tip = "You can create and manage multiple servers simultaneously and switch between them from the top-bar dropdown."
                    ),
                    GuideTopic(
                        subtitle = "2. Starting the Server",
                        description = "On the Dashboard screen, tap the orange 'Start' button. Because PumpkinMC is built in native Rust rather than Java, server boot completes in ~800 milliseconds compared to 45+ seconds on typical Java servers.",
                        tip = "Check the live TPS gauge on the Dashboard. It will immediately stabilize at a perfect 20.0 TPS."
                    ),
                    GuideTopic(
                        subtitle = "3. Joining from Minecraft",
                        description = "Open Minecraft on your PC, tablet, or phone. Go to Multiplayer -> Direct Connection (or Add Server). If you are on the same Wi-Fi, enter your phone's Wi-Fi IP (e.g. 192.168.1.50:25565). For playing over cellular or different networks, enable the Playit.gg tunnel in the Dashboard.",
                        tip = "Your phone's exact local IP address is displayed directly at the top of the Dashboard."
                    )
                )
            ),
            GuideSection(
                title = "Dashboard & Controls",
                icon = Icons.Default.Tune,
                summary = "Master real-time metrics, Rayon thread allocations, and configuration safety.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "Real-Time Telemetry Cards",
                        description = "The top card monitors live server status: TPS (Ticks Per Second, 20 is optimal), Active RAM usage (measured in MB), CPU utilization percentage, and uptime duration.",
                        tip = "Notice that PumpkinMC consumes only 18MB to 45MB of RAM, compared to 1.5GB to 2GB required by Paper/Spigot."
                    ),
                    GuideTopic(
                        subtitle = "Rayon & Worker Thread Tuning",
                        description = "PumpkinMC utilizes Rust's Rayon library for lock-free parallel chunk generation and entity processing. You can fine-tune the number of CPU cores dedicated to server tick routines versus networking routines.",
                        tip = "Tap 'Auto-Tune for this Phone' to let the hardware detector choose ideal thread and RAM allocations automatically based on your SoC tier."
                    ),
                    GuideTopic(
                        subtitle = "Dirty-Checking Save Protection",
                        description = "Whenever you adjust sliders, change gamemodes, or toggle settings on the Dashboard, an orange floating 'Unsaved Changes' bar appears. Settings are only applied to the server when you tap 'Save'. You can tap 'Discard' at any time to revert.",
                        tip = "This prevents accidental misconfigurations from taking effect while your server is actively running."
                    )
                )
            ),
            GuideSection(
                title = "Multiplayer Networking",
                icon = Icons.Default.Public,
                summary = "Connect with friends locally over Wi-Fi or globally via encrypted Playit.gg tunnels.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "LAN Multicast Discovery",
                        description = "When LAN mode is toggled on, PumpkinMC broadcasts UDP beacon packets over your local Wi-Fi router. Minecraft clients connected to the same Wi-Fi network will see your server appear automatically in the 'Friends' or 'LAN Games' tab without needing to type an IP address.",
                        tip = "Ensure your phone is connected to 5GHz Wi-Fi for lowest ping and jitter."
                    ),
                    GuideTopic(
                        subtitle = "Playit.gg Global Tunneling (Zero Port-Forwarding)",
                        description = "Playing with friends who are away from home? Enable Playit.gg in the Dashboard. The server generates a unique public address (e.g., pumpkin-25565.playit.gg:25565). Share this address with anyone in the world to let them join without router configuration or exposing your home IP.",
                        tip = "Tap the copy icon next to the address to copy it straight to your clipboard."
                    ),
                    GuideTopic(
                        subtitle = "Geyser Bedrock Crossplay Bridge",
                        description = "Enable Crossplay under Settings to allow friends playing on Android, iOS, Xbox, PlayStation, and Nintendo Switch (Minecraft Bedrock Edition) to join your Java PumpkinMC world seamlessly.",
                        tip = "Bedrock players connect using the standard Bedrock port (default 19132)."
                    ),
                    GuideTopic(
                        subtitle = "Playing on This Same Phone & Agent Name",
                        description = "Android OS blocks Minecraft Bedrock from connecting to 127.0.0.1 (loopback) between separate apps. To play on this same phone, use your Playit address (e.g. xyz.ply.gg:port), your phone's Wi-Fi IP, or join via Minecraft's 'Friends' tab! Also, on Playit.gg you can enter any Agent Name you want—it has no effect on connection.",
                        tip = "Both Bedrock UDP (19132) and Java TCP (25565) tunnels are automatically created for you."
                    )
                )
            ),
            GuideSection(
                title = "Live Console",
                icon = Icons.Default.Terminal,
                summary = "Full ANSI terminal output, interactive commands, and server administrative control.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "Real-Time Log Stream",
                        description = "The Console streams all server output in real-time, including player joins/leaves, chat messages, block modifications, and error tracebacks. Filter logs instantly by level: ALL, INFO, WARN, or ERROR.",
                        tip = "Use the Search bar in the console to filter logs for specific player names or coordinates."
                    ),
                    GuideTopic(
                        subtitle = "Interactive Command Prompt",
                        description = "Execute server operator commands directly from the bottom text field. Use quick-action chips above the prompt for instant commands like /tps, /help, /whitelist, /op, and /stop.",
                        tip = "To give yourself operator powers in-game, type '/op YourMinecraftUsername' and press send."
                    ),
                    GuideTopic(
                        subtitle = "Log Export & Clearing",
                        description = "Tap the download icon in the top right of the Console to export the entire log session to clipboard or text file for troubleshooting or bug reporting.",
                        tip = "Tap the trash icon to clear the visible log buffer without stopping the server."
                    )
                )
            ),
            GuideSection(
                title = "Plugin Market",
                icon = Icons.Default.Extension,
                summary = "Why WASM & Rust plugins excel on mobile, catalog search, and 1-click installation.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "WASM/Rust Plugins vs Java Mods",
                        description = "Traditional Forge/Fabric mods (.jar) require running a 2GB+ Java Virtual Machine, causing Android to overheat and kill background processes. PumpkinMC uses WebAssembly (WASM) and native Rust plugins that run directly inside the server memory space with negligible RAM footprint (~300KB each).",
                        tip = "You get the full power of Essentials, AntiCheat, Land Claiming, and WorldEdit without any phone slowdown."
                    ),
                    GuideTopic(
                        subtitle = "Browsing & Categorized Catalog",
                        description = "Browse official plugins across 9 curated categories: Essentials, Performance, Security, Gameplay, Administration, World Management, Tools, Economy, and All. Search plugins by name or functionality.",
                        tip = "The catalog synchronizes with market.pumpkinmc.org. Tap the sync icon to fetch the latest additions."
                    ),
                    GuideTopic(
                        subtitle = "1-Click Install & Uninstall",
                        description = "Tap 'Install' on any plugin card to automatically download and stage it into the server's plugins directory. To uninstall, tap the trash icon on the 'Active' badge.",
                        tip = "Restart the server after installing new plugins so the Tokio engine can load their WASM modules."
                    )
                )
            ),
            GuideSection(
                title = "File Manager",
                icon = Icons.Default.Folder,
                summary = "Full filesystem access, custom world imports, and built-in syntax code editor.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "Server File Explorer",
                        description = "Browse the server directory structure: server.toml, ops.json, whitelist.json, pumpkin.log, /plugins, and /worlds. View file sizes, timestamps, and extensions.",
                        tip = "You can create new files or folders using the '+' button in the top right of the File Manager."
                    ),
                    GuideTopic(
                        subtitle = "Built-in Code & Config Editor",
                        description = "Tap on any text file (e.g., server.toml, ops.json) to open the full-screen editor with line numbering and monospace code font. Edit values, add operators, and tap 'Save File'.",
                        tip = "Changes to server.toml take effect the next time the server is started or restarted."
                    )
                )
            ),
            GuideSection(
                title = "Battery & Thermal Guard",
                icon = Icons.Default.Shield,
                summary = "Keep your server running 24/7 safely without draining battery or overheating.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "CPU WakeLock (Background Hosting)",
                        description = "Prevents Android OS from putting the CPU into deep sleep when your screen turns off. Keeps server tick loops and player network connections active 24/7.",
                        tip = "Keep your phone plugged into a charger if hosting prolonged multiplayer sessions."
                    ),
                    GuideTopic(
                        subtitle = "Thermal Protection Guard",
                        description = "Monitors device battery and SoC temperature. If the phone exceeds 42°C, the engine automatically throttles world generation intensity to prevent battery degradation and cooling issues.",
                        tip = "Enabled by default to ensure safe long-term mobile hosting."
                    ),
                    GuideTopic(
                        subtitle = "Low-Power Idle Mode",
                        description = "When 0 players are connected, PumpkinMC throttles tickrate from 20 TPS to 15 TPS and pauses active chunk ticking, cutting mobile battery consumption by over 65%.",
                        tip = "As soon as a player connects, tickrate instantly springs back to 20.0 TPS."
                    )
                )
            ),
            GuideSection(
                title = "PumpkinMC Attribution",
                icon = Icons.Default.Bolt,
                summary = "Official credits, open source licensing, and project links.",
                subsections = listOf(
                    GuideTopic(
                        subtitle = "Powered by PumpkinMC",
                        description = "PumpkinMC Host is powered by the revolutionary open-source PumpkinMC project. Pumpkin is a blazingly fast, modern Minecraft server written from the ground up in Rust by the PumpkinMC organization and community contributors.",
                        tip = "Official Repository: https://github.com/Pumpkin-MC/Pumpkin"
                    ),
                    GuideTopic(
                        subtitle = "Key Technical Milestones",
                        description = "• Written 100% in safe, concurrent Rust.\n• Zero JVM runtime requirement.\n• Rayon work-stealing threadpool for parallel chunk generation.\n• Tokio asynchronous I/O runtime for non-blocking packet serialization.\n• Under 35MB base memory footprint.\n• Licensed under permissive open-source terms.",
                        tip = "Visit pumpkinmc.org for documentation, plugin SDKs, and community Discord."
                    )
                )
            )
        )
    }

    val activeSection = sections[selectedSectionIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, PumpkinOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PumpkinOrange.copy(alpha = 0.15f))
                                .border(1.dp, PumpkinOrange.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PUMPKINMC HOST MANUAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PumpkinOrange,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Complete Feature Guide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceElevated)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section Navigation Tabs (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEachIndexed { index, section ->
                        val isSelected = selectedSectionIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSectionIndex = index },
                            leadingIcon = {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = section.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PumpkinOrange,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary,
                                iconColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = ObsidianSurfaceBorder,
                                selectedBorderColor = PumpkinOrange,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Section Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(activeSection.icon, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(activeSection.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(activeSection.summary, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content of the active section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    activeSection.subsections.forEach { topic ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurfaceElevated)
                                .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = topic.subtitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = topic.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )

                                if (topic.tip != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ObsidianDark)
                                            .border(1.dp, PumpkinOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = PumpkinOrange,
                                                modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = topic.tip,
                                                fontSize = 11.sp,
                                                color = PumpkinOrange,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Links for PumpkinMC
                    if (selectedSectionIndex == sections.lastIndex) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Pumpkin-MC/Pumpkin"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PumpkinOrange,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visit Pumpkin-MC on GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
                ) {
                    Text("Close Guide", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
