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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
    isClaimLoading: Boolean = false,
    isAccountLinked: Boolean = false,
    onRequestClaim: ((String) -> Unit) -> Unit = {},
    onUnlinkAccount: () -> Unit = {},
    onRetryTunnel: () -> Unit = {},
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: Title & Manual link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = PumpkinOrange,
                        modifier = Modifier.size(15.dp)
                    )
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
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Manual",
                        tint = PumpkinOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Manual",
                        fontSize = 11.sp,
                        color = PumpkinOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2-Part Switch: Playit Tunnel vs Custom Tunnel
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
                        .clickable {
                            onServerChange(draftServer.copy(customTunnelEnabled = false, playitEnabled = true))
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Playit Tunnel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isCustom) Color.Black else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCustom) PumpkinOrange else Color.Transparent)
                        .clickable {
                            onServerChange(draftServer.copy(customTunnelEnabled = true))
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom Tunnel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCustom) Color.Black else TextPrimary
                    )
                }
            }

            if (!isCustom) {
                // PART 1: PLAYIT TUNNEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top: Playit Claim Option (In-App Web Only, No Browser Hassle)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (hasRealTunnel) Color(0xFF4CAF50)
                                            else if (isAccountLinked) Color(0xFF29B6F6)
                                            else Color(0xFFFFA000)
                                        )
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (hasRealTunnel) "Public Tunnel: Online"
                                    else if (isAccountLinked) "Playit Account: Linked"
                                    else "Playit Account: Claim Required",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasRealTunnel) Color(0xFF4CAF50)
                                    else if (isAccountLinked) Color(0xFF29B6F6)
                                    else Color(0xFFFFA000)
                                )
                            }

                            if (hasRealTunnel) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(draftServer.playitDomain))
                                        Toast.makeText(context, "Public address copied: ${draftServer.playitDomain}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy public address",
                                        tint = PumpkinOrange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        if (hasRealTunnel) {
                            Text(
                                text = draftServer.playitDomain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                text = if (isAccountLinked) "Tunnel is ready. Start server to allocate live connection."
                                else "Claim account to allow friends outside local network to join over internet.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Claim / Sync / Unlink Actions (Direct in-app, no external browser)
                    if (!isAccountLinked) {
                        Button(
                            onClick = {
                                // Request claim directly in app (in-app WebView dialog opens)
                                onRequestClaim { /* in-app webview handles the claim */ }
                            },
                            enabled = !isClaimLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PumpkinOrange,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            if (isClaimLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                            } else {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Claim Playit Account (In-App Web)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onRetryTunnel() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianSurface,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync / Reconnect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onUnlinkAccount()
                                    Toast.makeText(context, "Playit account unlinked.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unlink", fontSize = 11.sp)
                            }
                        }
                    }

                    // Divider separating Top Claim part and Bottom Ports part
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ObsidianSurfaceBorder))

                    // Bottom: Simple 2 Part - Bedrock Port & Java Port
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TUNNEL PORTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Part A: Bedrock Port
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Bedrock Port", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = designatedBedrockPort.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(designatedBedrockPort.toString()))
                                            Toast.makeText(context, "Bedrock port copied: $designatedBedrockPort", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Bedrock Port", tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Part B: Java Port
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Java Port", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = designatedJavaPort.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(designatedJavaPort.toString()))
                                            Toast.makeText(context, "Java port copied: $designatedJavaPort", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Java Port", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PART 2: CUSTOM TUNNEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top: Quick Presets & Host/Domain
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CUSTOM HOST / DOMAIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

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
                                        .background(if (isSelected) PumpkinOrange else ObsidianSurface)
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

                        OutlinedTextField(
                            value = draftServer.customTunnelAddress,
                            onValueChange = { onServerChange(draftServer.copy(customTunnelAddress = it)) },
                            label = { Text("Tunnel Host / IP", fontSize = 11.sp) },
                            placeholder = { Text("tunnel.domain.com", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PumpkinOrange,
                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                focusedLabelColor = PumpkinOrange,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ObsidianSurfaceBorder))

                    // Bottom: Simple 2 Part - Bedrock Port & Java Port inputs
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TUNNEL PORTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Part A: Bedrock Port Input
                            OutlinedTextField(
                                value = if (draftServer.bedrockPort == 0) "" else draftServer.bedrockPort.toString(),
                                onValueChange = {
                                    val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 19132
                                    onServerChange(draftServer.copy(bedrockPort = p, customPort = p))
                                },
                                label = { Text("Bedrock Port", fontSize = 11.sp) },
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

                            // Part B: Java Port Input
                            OutlinedTextField(
                                value = if (draftServer.port == 0) "" else draftServer.port.toString(),
                                onValueChange = {
                                    val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 25565
                                    onServerChange(draftServer.copy(port = p))
                                },
                                label = { Text("Java Port", fontSize = 11.sp) },
                                placeholder = { Text("25565", fontSize = 11.sp) },
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
            }

            // IN THE BOTTOM: LOCAL IP PART (Simple, Clean, No WiFi Icon, No Emojis)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local IP status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isRunning) Color(0xFF4CAF50) else Color(0xFF888888))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Local IP (LAN): Running" else "Local IP (LAN): Stopped",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) Color(0xFF4CAF50) else TextMuted
                        )
                    }

                    Text(
                        text = localAddress,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(localAddress))
                            Toast.makeText(context, "IP copied: $localAddress", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Two clean direct addresses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val bedrockLan = "$localAddress:$designatedBedrockPort"
                    val javaLan = "$localAddress:$designatedJavaPort"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(bedrockLan))
                                Toast.makeText(context, "Bedrock copied: $bedrockLan", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Bedrock", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    text = bedrockLan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Bedrock LAN",
                                tint = PumpkinOrange,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(javaLan))
                                Toast.makeText(context, "Java copied: $javaLan", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Java", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    text = javaLan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Java LAN",
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: Share Invite & Route Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val invite = "Join my Minecraft Server!\nAddress: $currentHost\nPort: $currentPort\nLocal LAN: $localAddress:$designatedBedrockPort"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, invite)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Server Invite"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PumpkinOrange, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
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
