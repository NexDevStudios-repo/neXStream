package com.nuvio.app.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

@Composable
internal fun GlassBarSurface(hazeState: HazeState?, modifier: Modifier = Modifier, glowStrength: Float = 1f) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val shape = RoundedCornerShape(50)

    Box(
        modifier
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape),
    )
}
