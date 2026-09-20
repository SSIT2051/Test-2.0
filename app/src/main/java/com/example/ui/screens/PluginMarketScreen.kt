package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MarketPlugin
import com.example.domain.model.ServerConfig
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.PumpkinOrangeMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PluginMarketScreen(
    server: ServerConfig?,
    plugins: List<MarketPlugin>,
    selectedCategory: String,
    searchQuery: String,
    isRefreshing: Boolean,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onInstallPlugin: (MarketPlugin) -> Unit,
    onUninstallPlugin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (server == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active server selected", color = TextSecondary)
        }
        return
    }

    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }
    var showArchitectureNotice by remember { mutableStateOf(false) }
    val categories = listOf("All", "Early Access", "Essentials", "Performance", "Security", "Gameplay", "Administration", "World Management", "Tools", "Economy")

    val displayedPlugins = remember(plugins, activeTab) {
        if (activeTab == 0) plugins else plugins.filter { it.isInstalled }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PLUGIN MARKET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Text("Official PumpkinMC Catalog", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            IconButton(onClick = onRefresh, modifier = Modifier.size(34.dp)) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PumpkinOrange, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = ObsidianSurface,
            contentColor = PumpkinOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = PumpkinOrange
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Catalog (${plugins.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Installed (${plugins.count { it.isInstalled }})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info notice explaining Rust WASM vs Java Mods
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianSurfaceElevated)
                .border(1.dp, PumpkinOrange.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .clickable { showArchitectureNotice = !showArchitectureNotice }
                .padding(10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("ℹ️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Why WASM plugins instead of Java mods?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PumpkinOrange
                        )
                    }
                    Text(
                        text = if (showArchitectureNotice) "Hide" else "Learn why",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (showArchitectureNotice) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PumpkinMC is compiled in native Rust (not Java). Java Forge/Fabric mods (.jar) require a 2GB+ JVM, which quickly overheats phones and causes Android OS to kill background apps. PumpkinMC runs lightning-fast WebAssembly (WASM) and Rust plugins (WorldEdit, ClearLag, Essentials, Geyser Bedrock crossplay) using under 50MB RAM at constant 20 TPS!",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == 0) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search plugins...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PumpkinOrange,
                    unfocusedBorderColor = ObsidianSurfaceBorder,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    cursorColor = PumpkinOrange
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PumpkinOrange,
                            selectedLabelColor = Color.Black,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = ObsidianSurfaceBorder,
                            selectedBorderColor = PumpkinOrange,
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Plugins List
        if (displayedPlugins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = PumpkinOrange.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (activeTab == 1) "No plugins installed on this server." else "No plugins found for \"$searchQuery\" in $selectedCategory.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (activeTab == 1) {
                            "Switch to the Catalog tab to browse and install native WASM & Rust plugins."
                        } else {
                            "PumpkinMC uses native Rust & WASM plugins instead of Forge/Fabric mods. Try searching for essentials, protection, voice, or clear filters."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (activeTab == 0 && (selectedCategory != "All" || searchQuery.isNotBlank())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSelectCategory("All")
                                onSearchQueryChange("")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = PumpkinOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, PumpkinOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Text("Reset Search & Filters", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedPlugins, key = { it.id }) { plugin ->
                    PluginItemCard(
                        plugin = plugin,
                        onInstall = {
                            onInstallPlugin(plugin)
                            Toast.makeText(context, "Installed ${plugin.name}", Toast.LENGTH_SHORT).show()
                        },
                        onUninstall = {
                            onUninstallPlugin(plugin.name)
                            Toast.makeText(context, "Removed ${plugin.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginItemCard(
    plugin: MarketPlugin,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (plugin.category == "Early Access") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF332005))
                                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "EARLY ACCESS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFB300),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "v${plugin.version} by ${plugin.author}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (plugin.isInstalled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PumpkinOrangeMuted)
                                .border(1.dp, PumpkinOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PumpkinOrange)
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(onClick = onUninstall, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Uninstall", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = PumpkinOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plugin.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(plugin.rating.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Text("•", fontSize = 10.sp, color = TextMuted)
                    Text("${plugin.downloads} downloads", fontSize = 11.sp, color = TextMuted)
                }

                Text(plugin.sizeText, fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}
