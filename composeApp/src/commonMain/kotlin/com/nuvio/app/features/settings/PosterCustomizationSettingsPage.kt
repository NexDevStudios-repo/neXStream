package com.nuvio.app.features.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.nuvio.app.core.ui.NuvioActionLabel
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.nuvio.app.core.poster.CustomPosterUrlRepository
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.core.ui.PosterCardStyleUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.settings_poster_card_radius
import nuvio.composeapp.generated.resources.settings_poster_card_style
import nuvio.composeapp.generated.resources.settings_poster_card_width
import nuvio.composeapp.generated.resources.settings_poster_custom
import nuvio.composeapp.generated.resources.settings_poster_description
import nuvio.composeapp.generated.resources.settings_poster_hide_labels
import nuvio.composeapp.generated.resources.settings_poster_always_show_landscape_clearlogo
import nuvio.composeapp.generated.resources.settings_poster_landscape_mode
import nuvio.composeapp.generated.resources.settings_poster_live_preview
import nuvio.composeapp.generated.resources.settings_poster_option_with_value
import nuvio.composeapp.generated.resources.settings_poster_preview_corner_radius
import nuvio.composeapp.generated.resources.settings_poster_preview_height
import nuvio.composeapp.generated.resources.settings_poster_preview_width
import nuvio.composeapp.generated.resources.settings_poster_radius_classic
import nuvio.composeapp.generated.resources.settings_poster_radius_pill
import nuvio.composeapp.generated.resources.settings_poster_radius_rounded
import nuvio.composeapp.generated.resources.settings_poster_radius_sharp
import nuvio.composeapp.generated.resources.settings_poster_radius_subtle
import nuvio.composeapp.generated.resources.settings_poster_width_balanced
import nuvio.composeapp.generated.resources.settings_poster_width_comfort
import nuvio.composeapp.generated.resources.settings_poster_width_compact
import nuvio.composeapp.generated.resources.settings_poster_width_dense
import nuvio.composeapp.generated.resources.settings_poster_width_large
import nuvio.composeapp.generated.resources.settings_poster_width_standard
import nuvio.composeapp.generated.resources.settings_custom_poster_title
import nuvio.composeapp.generated.resources.settings_custom_poster_description
import nuvio.composeapp.generated.resources.settings_custom_poster_placeholder
import nuvio.composeapp.generated.resources.settings_custom_poster_active
import nuvio.composeapp.generated.resources.settings_custom_poster_save
import nuvio.composeapp.generated.resources.settings_custom_poster_clear
import nuvio.composeapp.generated.resources.settings_custom_poster_apply_to
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_home
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_continue_watching
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_collections
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_library
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_search
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_details
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.posterCustomizationSettingsContent(
    isTablet: Boolean,
    uiState: PosterCardStyleUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_poster_card_style),
            isTablet = isTablet,
            actions = {
                NuvioActionLabel(
                    text = stringResource(Res.string.action_reset),
                    onClick = PosterCardStyleRepository::resetToDefaults,
                )
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                PosterCardStyleControls(
                    isTablet = isTablet,
                    widthDp = uiState.widthDp,
                    cornerRadiusDp = uiState.cornerRadiusDp,
                    catalogLandscapeModeEnabled = uiState.catalogLandscapeModeEnabled,
                    hideLabelsEnabled = uiState.hideLabelsEnabled,
                    alwaysShowLandscapeClearlogo = uiState.alwaysShowLandscapeClearlogo,
                    onWidthSelected = PosterCardStyleRepository::setWidthDp,
                    onCornerRadiusSelected = PosterCardStyleRepository::setCornerRadiusDp,
                    onCatalogLandscapeModeChange = PosterCardStyleRepository::setCatalogLandscapeModeEnabled,
                    onHideLabelsChange = PosterCardStyleRepository::setHideLabelsEnabled,
                    onAlwaysShowLandscapeClearlogoChange = PosterCardStyleRepository::setAlwaysShowLandscapeClearlogo,
                )
            }
        }
    }
    item {
        CustomPosterUrlRepository.ensureLoaded()
        val currentPattern by CustomPosterUrlRepository.pattern.collectAsState()
        var editingPattern by rememberSaveable(currentPattern) { mutableStateOf(currentPattern) }
        val isActive = currentPattern.isNotBlank()

        SettingsSection(
            title = stringResource(Res.string.settings_custom_poster_title),
            isTablet = isTablet,
            actions = {
                if (isActive) {
                    NuvioActionLabel(
                        text = stringResource(Res.string.settings_custom_poster_clear),
                        onClick = {
                            editingPattern = ""
                            CustomPosterUrlRepository.clearPattern()
                        },
                    )
                }
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_custom_poster_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = editingPattern,
                        onValueChange = { editingPattern = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.settings_custom_poster_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        singleLine = false,
                        maxLines = 4,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                CustomPosterUrlRepository.setPattern(editingPattern)
                            },
                            enabled = editingPattern.trim() != currentPattern,
                        ) {
                            Text(text = stringResource(Res.string.settings_custom_poster_save))
                        }
                    }
                    if (isActive) {
                        Text(
                            text = "✓ ${stringResource(Res.string.settings_custom_poster_active)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.settings_custom_poster_apply_to),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val enabledScreens by CustomPosterUrlRepository.enabledScreens.collectAsState()
                        val screenEntries = listOf(
                            com.nuvio.app.core.poster.CustomPosterScreen.HOME to stringResource(Res.string.settings_custom_poster_screen_home),
                            com.nuvio.app.core.poster.CustomPosterScreen.CONTINUE_WATCHING to stringResource(Res.string.settings_custom_poster_screen_continue_watching),
                            com.nuvio.app.core.poster.CustomPosterScreen.COLLECTIONS to stringResource(Res.string.settings_custom_poster_screen_collections),
                            com.nuvio.app.core.poster.CustomPosterScreen.LIBRARY to stringResource(Res.string.settings_custom_poster_screen_library),
                            com.nuvio.app.core.poster.CustomPosterScreen.SEARCH to stringResource(Res.string.settings_custom_poster_screen_search),
                            com.nuvio.app.core.poster.CustomPosterScreen.DETAILS to stringResource(Res.string.settings_custom_poster_screen_details),
                        )
                        screenEntries.forEach { (screen, label) ->
                            PosterToggleRow(
                                title = label,
                                checked = screen in enabledScreens,
                                onCheckedChange = { enabled ->
                                    CustomPosterUrlRepository.setScreenEnabled(screen, enabled)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterCardStyleControls(
    isTablet: Boolean,
    widthDp: Int,
    cornerRadiusDp: Int,
    catalogLandscapeModeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
    alwaysShowLandscapeClearlogo: Boolean,
    onWidthSelected: (Int) -> Unit,
    onCornerRadiusSelected: (Int) -> Unit,
    onCatalogLandscapeModeChange: (Boolean) -> Unit,
    onHideLabelsChange: (Boolean) -> Unit,
    onAlwaysShowLandscapeClearlogoChange: (Boolean) -> Unit,
) {
    val widthOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_width_compact), 104),
        PresetOption(stringResource(Res.string.settings_poster_width_dense), 112),
        PresetOption(stringResource(Res.string.settings_poster_width_standard), 120),
        PresetOption(stringResource(Res.string.settings_poster_width_balanced), 126),
        PresetOption(stringResource(Res.string.settings_poster_width_comfort), 134),
        PresetOption(stringResource(Res.string.settings_poster_width_large), 140),
    )
    val radiusOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_radius_sharp), 0),
        PresetOption(stringResource(Res.string.settings_poster_radius_subtle), 4),
        PresetOption(stringResource(Res.string.settings_poster_radius_classic), 8),
        PresetOption(stringResource(Res.string.settings_poster_radius_rounded), 12),
        PresetOption(stringResource(Res.string.settings_poster_radius_pill), 16),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = if (isTablet) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_poster_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PosterCardLivePreview(
            widthDp = widthDp,
            cornerRadiusDp = cornerRadiusDp,
        )
        PosterStyleOptionRow(
            title = stringResource(Res.string.settings_poster_card_width),
            selectedValue = widthDp,
            options = widthOptions,
            onSelected = onWidthSelected,
        )
        PosterStyleOptionRow(
            title = stringResource(Res.string.settings_poster_card_radius),
            selectedValue = cornerRadiusDp,
            options = radiusOptions,
            onSelected = onCornerRadiusSelected,
        )
        PosterLandscapeModeToggleRow(
            checked = catalogLandscapeModeEnabled,
            onCheckedChange = onCatalogLandscapeModeChange,
        )
        PosterToggleRow(
            title = stringResource(Res.string.settings_poster_hide_labels),
            checked = hideLabelsEnabled,
            onCheckedChange = onHideLabelsChange,
        )
        PosterToggleRow(
            title = stringResource(Res.string.settings_poster_always_show_landscape_clearlogo),
            checked = alwaysShowLandscapeClearlogo,
            onCheckedChange = onAlwaysShowLandscapeClearlogoChange,
        )
    }
}

@Composable
private fun PosterLandscapeModeToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PosterToggleRow(
        title = stringResource(Res.string.settings_poster_landscape_mode),
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun PosterToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun PosterCardLivePreview(
    widthDp: Int,
    cornerRadiusDp: Int,
) {
    val targetHeightDp = (widthDp * 3) / 2
    val previewFrameWidthDp = 140
    val previewFrameHeightDp = 210
    val animatedWidth = animateDpAsState(
        targetValue = widthDp.dp,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewWidth",
    )
    val animatedHeight = animateDpAsState(
        targetValue = targetHeightDp.dp,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewHeight",
    )
    val animatedCornerRadius = animateDpAsState(
        targetValue = cornerRadiusDp.dp,
        animationSpec = tween(durationMillis = 220),
        label = "posterPreviewCornerRadius",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_poster_live_preview),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(previewFrameWidthDp.dp)
                    .height(previewFrameHeightDp.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(animatedWidth.value)
                        .height(animatedHeight.value)
                        .clip(RoundedCornerShape(animatedCornerRadius.value))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(animatedCornerRadius.value),
                        ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_poster_preview_width, widthDp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.settings_poster_preview_corner_radius, cornerRadiusDp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.settings_poster_preview_height, targetHeightDp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterStyleOptionRow(
    title: String,
    selectedValue: Int,
    options: List<PresetOption>,
    onSelected: (Int) -> Unit,
) {
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label
        ?: stringResource(Res.string.settings_poster_custom)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_poster_option_with_value, title, selectedLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selectedValue,
                    onClick = { onSelected(option.value) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

private data class PresetOption(
    val label: String,
    val value: Int,
)
