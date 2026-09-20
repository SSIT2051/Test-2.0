package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

enum class LogoVariant {
    CREST,
    BLADE_NODE,
    COMPACT
}

/**
 * Original PumpkinMC Host logo renderer - displaying the exact vector asset directly
 * without extra boxes or outer border frames.
 */
@Composable
fun PumpkinLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true,
    variant: LogoVariant = LogoVariant.CREST
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_pumpkin_logo),
            contentDescription = "PumpkinMC Logo",
            modifier = Modifier.size(size)
        )
    }
}
