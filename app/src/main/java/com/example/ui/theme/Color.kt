package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// STRICT 3-COLOR SYSTEM: Monochrome Charcoal + Crisp White + Pumpkin Orange
// Zero rainbow colors, zero visual clutter. Modern, sleek, minimalist.
// =====================================================================

// 1. ACCENT COLOR: Pumpkin Orange
val PumpkinOrange = Color(0xFFFF6600)
val PumpkinOrangeLight = Color(0xFFFF8533)
val PumpkinOrangeMuted = Color(0x26FF6600) // 15% opacity tint for badges/accents

// 2. MONOCHROME BASE: Deep Obsidian / Pitch Charcoal
val ObsidianDark = Color(0xFF09090B)           // Canvas background
val ObsidianSurface = Color(0xFF131316)        // Card / section surface
val ObsidianSurfaceElevated = Color(0xFF1A1A1F)// Buttons / input fields
val ObsidianSurfaceBorder = Color(0xFF26262B)  // Razor-thin clean hairline borders

// 3. NEUTRALS: Crisp White & Slate Grays
val TextPrimary = Color(0xFFFFFFFF)            // High-contrast primary text
val TextSecondary = Color(0xFFA1A1AA)          // Subtitles / secondary labels
val TextMuted = Color(0xFF52525B)              // Muted hints / timestamps

// Clean monochromatic terminal
val TerminalBackground = Color(0xFF09090B)
val TerminalText = Color(0xFFE4E4E7)
val TerminalPrompt = Color(0xFFFF6600)
