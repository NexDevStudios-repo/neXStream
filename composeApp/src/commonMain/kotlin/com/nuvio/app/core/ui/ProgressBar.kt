package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun NuvioProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = NuvioTokens.Space.s4,
    trackColor: Color = Color.Unspecified,
    fillColor: Color = Color.Unspecified,
) {
    val resolvedTrack = if (trackColor != Color.Unspecified) trackColor else MaterialTheme.colorScheme.surfaceContainerHighest
    val resolvedFill = if (fillColor != Color.Unspecified) fillColor else MaterialTheme.colorScheme.primary
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(resolvedTrack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .width(NuvioTokens.Space.none)
                .height(height)
                .clip(CircleShape)
                .background(resolvedFill),
        )
    }
}

