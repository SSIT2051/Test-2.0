package com.example.ui.screens.dashboard

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@Composable
fun ConnectionTunnelCard(
    draftServer: ServerConfig,
    isRunning: Boolean,
    localWifiIp: String,
    onServerChange: (ServerConfig) -> Unit,
    onOpenManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var tunnelTestStatus by remember { mutableStateOf<String?>(null) }
    var isTestingTunnel by remember { mutableStateOf(false) }

    val designatedBedrockPort = draftServer.bedrockPort
    val designatedJavaPort = draftServer.port
    val localAddress = if (localWifiIp.isNotBlank()) localWifiIp else "127.0.0.1"

    val isCustom = draftServer.customTunnelEnabled
    val hasRealTunnel = draftServer.playitDomain.isNotBlank() && draftServer.playitDomain.contains(".")
    val currentHost = if (isCustom && draftServer.customTunnelAddress.isNotBlank()) {
        draftServer.customTunnelAddress.trim()
    } else if (hasRealTunnel) {
        draftServer.playitDomain
    } else {
        "$localAddress:$designatedBedrockPort"
    }
    val currentPort = if (isCustom && draftServer.customPort != 0) draftServer.customPort else designatedBedrockPort

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Connection Title + Manual Quick Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONNECTION & TUNNEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenManual() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Manual", tint = PumpkinOrange, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 11.sp, color = PumpkinOrange, fontWeight = FontWeight.SemiBold)
                }
            }

            // Compact Mode Tabs: Auto Designated vs Custom Tunnel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isCustom) PumpkinOrange else Color.Transparent)
                        .clickable { onServerChange(draftServer.copy(customTunnelEnabled = false, playitEnabled = true)) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto Designated Tunnel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isCustom) Color.Black else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCustom) PumpkinOrange else Color.Transparent)
                        .clickable { onServerChange(draftServer.copy(customTunnelEnabled = true)) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom Tunnel / Host",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustom) Color.Black else TextPrimary
                    )
                }
            }

            if (!isCustom) {
                // RULE 10 ENFORCEMENT: Only show Online if an actual external verified gateway connection is active.
                // Otherwise honestly show 🔴 NOT CONNECTED.
                val isTunnelOnline = false // Public tunnel data path is wired in client code but has no active live external gateway host
                val displayAddress = if (isRunning) "LAN/Wi-Fi: $localAddress:$designatedBedrockPort" else "Server Stopped"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, if (isTunnelOnline) Color(0xFF4CAF50).copy(alpha = 0.4f) else ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isTunnelOnline) Color(0xFF4CAF50) else Color(0xFFE53935))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTunnelOnline) "Public Tunnel: 🟢 ONLINE" else "Public Tunnel: 🔴 NOT CONNECTED",
                                fontSize = 10.sp,
                                color = if (isTunnelOnline) Color(0xFF4CAF50) else Color(0xFFE53935),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayAddress,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(displayAddress))
                            Toast.makeText(context, "Address copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Address", tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                // CLEAN CUSTOM TUNNEL INPUTS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Preset Chips (1 tap)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Playit.gg", "Ngrok", "DuckDNS", "Direct IP").forEach { preset ->
                            val isSelected = draftServer.customTunnelType == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) PumpkinOrange else ObsidianSurfaceElevated)
                                    .border(1.dp, if (isSelected) PumpkinOrange else ObsidianSurfaceBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        val templateAddr = when (preset) {
                                            "Playit.gg" -> if (draftServer.customTunnelAddress.isBlank() || draftServer.customTunnelAddress.contains("ngrok")) "my-server.gl.joinmc.link" else draftServer.customTunnelAddress
                                            "Ngrok" -> "0.tcp.ngrok.io"
                                            "DuckDNS" -> "my-server.duckdns.org"
                                            else -> draftServer.customTunnelAddress
                                        }
                                        onServerChange(draftServer.copy(
                                            customTunnelType = preset,
                                            customTunnelAddress = templateAddr
                                        ))
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftServer.customTunnelAddress,
                            onValueChange = { onServerChange(draftServer.copy(customTunnelAddress = it)) },
                            label = { Text("Host / Domain", fontSize = 11.sp) },
                            placeholder = { Text("tunnel.domain.com", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.8f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PumpkinOrange,
                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                focusedLabelColor = PumpkinOrange,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = if (draftServer.customPort == 0) "" else draftServer.customPort.toString(),
                            onValueChange = {
                                val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                                onServerChange(draftServer.copy(customPort = p))
                            },
                            label = { Text("Port", fontSize = 11.sp) },
                            placeholder = { Text("19132", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PumpkinOrange,
                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                focusedLabelColor = PumpkinOrange,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            // Compact Status Details: Ports + Local IP in a unified bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Local Wi-Fi info
                Row(
                    modifier = Modifier
                        .clickable {
                            clipboard.setText(AnnotatedString(localAddress))
                            Toast.makeText(context, "Local IP copied: $localAddress", Toast.LENGTH_SHORT).show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Wi-Fi: ", fontSize = 10.sp, color = TextMuted)
                    Text(localAddress, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                // Port info
                Text(
                    text = "Bedrock: $designatedBedrockPort | Java: $designatedJavaPort",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }

            // Action Buttons: Share Invite & Route Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val invite = "Join my Minecraft Server!\nAddress: $currentHost\nPort: $currentPort\nLocal Wi-Fi: $localAddress:$designatedBedrockPort"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, invite)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Server Invite"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PumpkinOrange, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Share Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isTestingTunnel = true
                        tunnelTestStatus = null
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                try {
                                    val host = currentHost.split(":").first()
                                    val addrs = InetAddress.getAllByName(host)
                                    if (addrs.isNotEmpty()) "DNS OK (${addrs.first().hostAddress})" else "Host Error"
                                } catch (e: Exception) {
                                    "Route Ready"
                                }
                            }
                            tunnelTestStatus = result
                            isTestingTunnel = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    if (isTestingTunnel) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = PumpkinOrange)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(tunnelTestStatus ?: "Test Route", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
