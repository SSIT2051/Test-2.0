package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ServerConfig
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBorder
import com.example.ui.theme.PumpkinOrange
import com.example.ui.theme.TextPrimary

@Composable
fun ServerIdentityCard(
    draftServer: ServerConfig,
    onServerChange: (ServerConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "SERVER IDENTITY & LOCAL PORTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = draftServer.name,
                onValueChange = { onServerChange(draftServer.copy(name = it)) },
                label = { Text("Server Name") },
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

            OutlinedTextField(
                value = draftServer.motd,
                onValueChange = { onServerChange(draftServer.copy(motd = it)) },
                label = { Text("MOTD (Server Description)") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftServer.port.toString(),
                    onValueChange = {
                        val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 25565
                        onServerChange(draftServer.copy(port = p))
                    },
                    label = { Text("Java Port") },
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

                OutlinedTextField(
                    value = draftServer.bedrockPort.toString(),
                    onValueChange = {
                        val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 19132
                        onServerChange(draftServer.copy(bedrockPort = p))
                    },
                    label = { Text("Bedrock Port") },
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
