package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.home.HomeCatalogSettingsItem
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.xtream.XtreamRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_homescreen_section_hero
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.homescreenSettingsContent(
    isTablet: Boolean,
    heroEnabled: Boolean,
    showCatalogType: Boolean = true,
    hideUnreleasedContent: Boolean = false,
    items: List<HomeCatalogSettingsItem> = emptyList(),
    isCatalogLoading: Boolean = false,
    catalogErrorMessage: String? = null,
) {
    item {
        HomescreenXtreamSummaryCard(isTablet = isTablet)
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_homescreen_section_hero),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_homescreen_show_hero),
                    description = stringResource(Res.string.settings_homescreen_show_hero_description),
                    checked = heroEnabled,
                    isTablet = isTablet,
                    onCheckedChange = HomeCatalogSettingsRepository::setHeroEnabled,
                )
            }
        }
    }

    item {
        val uiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()

        SettingsSection(
            title = "Secciones de Contenido Xtream",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Canales de TV en Vivo",
                    description = "Cargar categorías y canales en vivo en la pantalla de inicio",
                    checked = uiState.liveStreamsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = HomeCatalogSettingsRepository::setLiveStreamsEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = "Películas (VOD)",
                    description = "Cargar categorías de películas bajo demanda en la pantalla de inicio",
                    checked = uiState.vodEnabled,
                    isTablet = isTablet,
                    onCheckedChange = HomeCatalogSettingsRepository::setVodEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = "Series de TV",
                    description = "Cargar categorías de series en la pantalla de inicio",
                    checked = uiState.seriesEnabled,
                    isTablet = isTablet,
                    onCheckedChange = HomeCatalogSettingsRepository::setSeriesEnabled,
                )
            }
        }
    }

    item {
        val activeServer by XtreamRepository.activeServer.collectAsStateWithLifecycle()

        SettingsSection(
            title = "Servidor Xtream Activo",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (activeServer != null) {
                    val server = activeServer!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = server.name.ifBlank { "Servidor Xtream" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = server.serverUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sin servidor conectado",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Añade tu cuenta en Ajustes > Cuentas para cargar contenido en Inicio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomescreenXtreamSummaryCard(
    isTablet: Boolean,
) {
    SettingsGroup(isTablet = isTablet) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Pantalla de Inicio Xtream",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Configura qué contenido y secciones de tu cuenta Xtream se mostrarán en la pantalla principal de la aplicación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
