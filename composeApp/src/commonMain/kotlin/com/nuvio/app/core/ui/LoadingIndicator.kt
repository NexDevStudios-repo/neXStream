package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NuvioLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    size: Dp = NuvioTokens.Space.s40,
    strokeWidth: Dp = 3.5.dp,
    active: Boolean = LocalScreenActive.current,
) {
    if (!active) return

    val indicatorColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = indicatorColor,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round,
        )
    }
}
