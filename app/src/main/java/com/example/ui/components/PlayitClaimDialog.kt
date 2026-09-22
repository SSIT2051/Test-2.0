package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.ui.viewmodel.PlayitClaimUiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayitClaimDialog(
    state: PlayitClaimUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSaveManualKey: (String) -> Unit
) {
    if (!state.isOpen) return

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Options & Browser, 1 = In-App WebView
    var manualKeyInput by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Auto-launch browser when claim URL becomes available
    LaunchedEffect(state.claimUrl) {
        if (state.claimUrl.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.claimUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Browser intent failed or unavailable; user will see the dialog with in-app webview & manual copy
                selectedTab = 1 // Auto-switch to In-App WebView if external browser didn't launch
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = PumpkinOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Claim Playit.gg Account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Public Tunnel Setup & Verification",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ObsidianSurfaceBorder)
                )

                // Dialog Content Body
                if (state.isLoading) {
                    // 1. Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = PumpkinOrange,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Contacting Playit.gg API...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Generating secure authorization code for this device",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else if (state.isLinked) {
                    // 2. Success / Linked State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Account Successfully Linked!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Your Playit.gg Agent is authenticated. Public tunnel endpoints will now allocate and route players over the internet.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PumpkinOrange,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (state.errorMessage != null) {
                    // 3. Error State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Claim Connection Issue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = state.errorMessage,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PumpkinOrange,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Claim", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("Cancel")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Fallback manual key input even on network failure
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceElevated)
                                    .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Or paste your Agent Secret Key manually:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = manualKeyInput,
                                            onValueChange = { manualKeyInput = it },
                                            placeholder = { Text("Secret Key from playit dashboard", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PumpkinOrange,
                                                unfocusedBorderColor = ObsidianSurfaceBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
                                        Button(
                                            onClick = { onSaveManualKey(manualKeyInput) },
                                            enabled = manualKeyInput.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PumpkinOrange,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 4. Ready State: Claim Code Generated & Polling for Approval
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Navigation Tab Bar: [ Browser & Link ] | [ In-App WebView ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianSurfaceElevated)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 0) PumpkinOrange else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) Color.Black else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Browser & Link",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 0) Color.Black else TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 1) PumpkinOrange else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (selectedTab == 1) Color.Black else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "In-App WebView",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }

                        // Tab 0: Browser & Quick Link Controls
                        if (selectedTab == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Claim Code Badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianSurfaceElevated)
                                        .border(1.dp, PumpkinOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "PLAYIT CLAIM CODE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PumpkinOrange,
                                            letterSpacing = 1.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.code,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            )

                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(state.code))
                                                    Toast.makeText(context, "Claim code copied: ${state.code}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = PumpkinOrange, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = "Approve this code on Playit.gg to link your host device.",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                // Primary Action: Open in External Browser
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.claimUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open external browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                            selectedTab = 1 // Switch to In-App WebView
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PumpkinOrange,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Claim Link in Browser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Secondary Action: Switch to in-app WebView
                                OutlinedButton(
                                    onClick = { selectedTab = 1 },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Claim Here Inside App (WebView)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                // Full URL Copy Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianSurface)
                                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Direct URL", fontSize = 10.sp, color = TextMuted)
                                            Text(
                                                text = state.claimUrl,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(state.claimUrl))
                                                Toast.makeText(context, "Claim URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }

                                // Live Polling Status Indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianSurfaceElevated)
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = PumpkinOrange
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Waiting for your approval on Playit.gg...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Once you log in and confirm, this app will automatically sync!",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                // Manual Agent Secret Key Section
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianSurface)
                                        .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Key, contentDescription = null, tint = PumpkinOrange, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Already have an Agent Secret Key?",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "If you created an agent on playit.gg/manage/agents, paste the secret key here:",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = manualKeyInput,
                                                onValueChange = { manualKeyInput = it },
                                                placeholder = { Text("Paste Agent Secret Key", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PumpkinOrange,
                                                    unfocusedBorderColor = ObsidianSurfaceBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            Button(
                                                onClick = {
                                                    onSaveManualKey(manualKeyInput)
                                                    Toast.makeText(context, "Agent key saved!", Toast.LENGTH_SHORT).show()
                                                },
                                                enabled = manualKeyInput.isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PumpkinOrange,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Tab 1: In-App WebView
                            Column(modifier = Modifier.fillMaxSize()) {
                                // WebView Control Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ObsidianSurface)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "In-App Browser: playit.gg/claim",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { webViewRef?.reload() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = PumpkinOrange, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.claimUrl)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open external browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open External", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Embedded WebView
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.databaseEnabled = true
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            settings.setSupportZoom(true)
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false

                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    return false
                                                }
                                            }
                                            loadUrl(state.claimUrl)
                                            webViewRef = this
                                        }
                                    },
                                    update = { wv ->
                                        if (wv.url != state.claimUrl && state.claimUrl.isNotBlank()) {
                                            wv.loadUrl(state.claimUrl)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )

                                // Bottom helper banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ObsidianSurfaceElevated)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = PumpkinOrange
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Listening for authorization... Will automatically complete setup upon claim.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
