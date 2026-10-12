package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

internal actual val floatingNavigationGlowSupported: Boolean
    get() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU

@Composable
internal actual fun FloatingNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier,
    scrollState: NuvioNavBarScrollState?,
    hazeState: HazeState?,
    contentPadding: PaddingValues,
    compactSize: Boolean,
    glowEnabled: Boolean,
) {
    if (items.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val shape = CircleShape
        val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
        val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.30f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f),
                )
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, borderColor, shape),
            shape = shape,
            color = surfaceColor,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = item.selected
                    val haptic = LocalHapticFeedback.current

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "icon_scale",
                    )

                    Surface(
                        modifier = Modifier
                            .height(50.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                item.onClick()
                            },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = if (isSelected) 16.dp else 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (item.content != null) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    item.content.invoke(item.onClick)
                                }
                            } else if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                )
                            }

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                                exit = fadeOut(tween(durationMillis = 100)) + shrinkHorizontally(tween(durationMillis = 150)),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        softWrap = false,
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

internal fun visualNavIndex(logicalIndex: Int, count: Int, isRtl: Boolean): Int =
    if (logicalIndex in 0 until count && isRtl) count - 1 - logicalIndex else logicalIndex

internal fun logicalNavIndex(visualIndex: Int, count: Int, isRtl: Boolean): Int =
    if (visualIndex in 0 until count && isRtl) count - 1 - visualIndex else visualIndex
