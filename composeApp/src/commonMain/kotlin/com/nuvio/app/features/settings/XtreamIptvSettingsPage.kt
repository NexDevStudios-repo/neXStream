package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogOption
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.xtream.XtreamApi
import com.nuvio.app.features.xtream.XtreamPreferencesRepository
import com.nuvio.app.features.xtream.XtreamRepository
import com.nuvio.app.features.xtream.XtreamServerAccount
import com.nuvio.app.features.xtream.XtreamStorage
import com.nuvio.app.features.xtream.XtreamUserInfo
import kotlinx.coroutines.launch

internal fun LazyListScope.xtreamIptvSettingsContent(
    isTablet: Boolean,
    onManageServersClick: () -> Unit,
) {
    item {
        XtreamIptvSettingsBody(
            isTablet = isTablet,
            onManageServersClick = onManageServersClick,
        )
    }
}

@Composable
private fun XtreamIptvSettingsBody(
    isTablet: Boolean,
    onManageServersClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val preferences by remember {
        XtreamPreferencesRepository.ensureLoaded()
        XtreamPreferencesRepository.state
    }.collectAsStateWithLifecycle()

    val activeServer by XtreamRepository.activeServer.collectAsStateWithLifecycle()
    val servers by XtreamRepository.servers.collectAsStateWithLifecycle()

    var userInfo by remember { mutableStateOf<XtreamUserInfo?>(null) }
    var isCheckingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(activeServer?.id) {
        val server = activeServer
        if (server != null && server.serverType == "xtream") {
            isCheckingStatus = true
            coroutineScope.launch {
                val authRes = XtreamApi.authenticate(server.serverUrl, server.username, server.password)
                userInfo = authRes.getOrNull()?.userInfo
                isCheckingStatus = false
            }
        } else {
            userInfo = null
            isCheckingStatus = false
        }
    }

    var showFormatDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showEpgShiftDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Active Server Card
        ActiveServerHeroCard(
            activeServer = activeServer,
            userInfo = userInfo,
            isCheckingStatus = isCheckingStatus,
            serversCount = servers.size,
            onManageServersClick = onManageServersClick,
        )

        // Sección: Reproducción y Formato
        SettingsSection(
            title = "REPRODUCCIÓN Y FLUJO DE DATOS",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Formato de transmisión preferido",
                    description = when (preferences.outputFormat) {
                        "ts" -> "MPEG-TS (.ts) - Flujo directo"
                        else -> "HLS (.m3u8) - Recomendado"
                    },
                    icon = Icons.Rounded.Tune,
                    isTablet = isTablet,
                    onClick = { showFormatDialog = true },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "Búfer de reproducción",
                    description = when (preferences.bufferSeconds) {
                        0 -> "Sin búfer (0s - Baja latencia en vivo)"
                        2 -> "Normal (2s - Equilibrado)"
                        5 -> "Estable (5s - Recomendado)"
                        10 -> "Búfer Alto (10s - Redes inestables)"
                        else -> "${preferences.bufferSeconds} segundos"
                    },
                    icon = Icons.Rounded.Speed,
                    isTablet = isTablet,
                    onClick = { showBufferDialog = true },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "User-Agent HTTP",
                    description = preferences.userAgent.ifBlank { "neXStream" },
                    icon = Icons.Rounded.Language,
                    isTablet = isTablet,
                    onClick = { showUserAgentDialog = true },
                )
            }
        }

        // Sección: Guía EPG
        SettingsSection(
            title = "GUÍA ELECTRÓNICA DE PROGRAMACIÓN (EPG)",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Desplazamiento horario (EPG Timeshift)",
                    description = when {
                        preferences.epgTimeShiftHours == 0 -> "Sin desplazamiento (0h)"
                        preferences.epgTimeShiftHours > 0 -> "+${preferences.epgTimeShiftHours} horas"
                        else -> "${preferences.epgTimeShiftHours} horas"
                    },
                    icon = Icons.Rounded.Schedule,
                    isTablet = isTablet,
                    onClick = { showEpgShiftDialog = true },
                )
            }
        }

        // Sección: Sincronización y Mantenimiento
        SettingsSection(
            title = "SINCRONIZACIÓN Y CACHÉ",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Sincronizar listas al iniciar",
                    description = "Actualiza automáticamente categorías y canales al abrir la app",
                    checked = preferences.autoSyncOnLaunch,
                    isTablet = isTablet,
                    onCheckedChange = { XtreamPreferencesRepository.setAutoSyncOnLaunch(it) },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "Sincronizar listas ahora",
                    description = "Recarga inmediata de categorías, canales y contenido VOD",
                    icon = Icons.Rounded.Refresh,
                    isTablet = isTablet,
                    onClick = {
                        coroutineScope.launch {
                            val server = activeServer
                            if (server != null) {
                                NuvioToastController.show("Sincronizando listas...")
                                XtreamRepository.clearCache()
                                com.nuvio.app.features.home.HomeRepository.refresh(force = true)
                                com.nuvio.app.features.search.SearchRepository.refreshDiscover(forceRefresh = true)
                                NuvioToastController.show("¡Listas sincronizadas con éxito!")
                            } else {
                                NuvioToastController.show("No hay ningún servidor activo para sincronizar")
                            }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "Limpiar caché local de listas",
                    description = "Elimina copias locales en disco para forzar una descarga limpia",
                    icon = Icons.Rounded.DeleteOutline,
                    isTablet = isTablet,
                    onClick = {
                        coroutineScope.launch {
                            val server = activeServer
                            if (server != null) {
                                XtreamStorage.deleteM3uContent(server.id)
                            }
                            XtreamRepository.clearCache()
                            com.nuvio.app.features.home.HomeRepository.refresh(force = true)
                            NuvioToastController.show("Caché local limpiada correctamente")
                        }
                    },
                )
            }
        }
    }

    // Format Dialog
    if (showFormatDialog) {
        DialogSurface(
            onDismissRequest = { showFormatDialog = false },
            title = "Formato de Transmisión",
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogOption(
                    text = "HLS (.m3u8) - Recomendado para Smart TV y Móvil",
                    selected = preferences.outputFormat == "m3u8",
                    onClick = {
                        XtreamPreferencesRepository.setOutputFormat("m3u8")
                        showFormatDialog = false
                    },
                )
                DialogOption(
                    text = "MPEG-TS (.ts) - Flujo directo sin transcodificación",
                    selected = preferences.outputFormat == "ts",
                    onClick = {
                        XtreamPreferencesRepository.setOutputFormat("ts")
                        showFormatDialog = false
                    },
                )
            }
            DialogButtons {
                DialogButton(
                    text = "Cerrar",
                    style = DialogButtonStyle.Secondary,
                    onClick = { showFormatDialog = false },
                )
            }
        }
    }

    // Buffer Dialog
    if (showBufferDialog) {
        DialogSurface(
            onDismissRequest = { showBufferDialog = false },
            title = "Búfer de Reproducción",
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    0 to "Sin búfer (0s - Baja latencia en vivo)",
                    2 to "Normal (2s - Equilibrado)",
                    5 to "Estable (5s - Recomendado)",
                    10 to "Búfer Alto (10s - Redes inestables)",
                ).forEach { (seconds, label) ->
                    DialogOption(
                        text = label,
                        selected = preferences.bufferSeconds == seconds,
                        onClick = {
                            XtreamPreferencesRepository.setBufferSeconds(seconds)
                            showBufferDialog = false
                        },
                    )
                }
            }
            DialogButtons {
                DialogButton(
                    text = "Cerrar",
                    style = DialogButtonStyle.Secondary,
                    onClick = { showBufferDialog = false },
                )
            }
        }
    }

    // User Agent Dialog
    if (showUserAgentDialog) {
        var customUaInput by remember { mutableStateOf(preferences.userAgent) }
        DialogSurface(
            onDismissRequest = { showUserAgentDialog = false },
            title = "User-Agent HTTP",
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    "neXStream" to "neXStream (Por defecto)",
                    "VLC/3.0.18" to "VLC / 3.0.18 (Alta compatibilidad)",
                    "IPTV Smarters Pro" to "IPTV Smarters Pro",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" to "Navegador Web (Chrome/Firefox)",
                ).forEach { (ua, label) ->
                    DialogOption(
                        text = label,
                        selected = preferences.userAgent == ua,
                        onClick = {
                            XtreamPreferencesRepository.setUserAgent(ua)
                            showUserAgentDialog = false
                        },
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O especifica un User-Agent personalizado:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = customUaInput,
                    onValueChange = { customUaInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
            DialogButtons {
                DialogButton(
                    text = "Guardar",
                    style = DialogButtonStyle.Primary,
                    onClick = {
                        if (customUaInput.isNotBlank()) {
                            XtreamPreferencesRepository.setUserAgent(customUaInput.trim())
                        }
                        showUserAgentDialog = false
                    },
                )
                DialogButton(
                    text = "Cerrar",
                    style = DialogButtonStyle.Secondary,
                    onClick = { showUserAgentDialog = false },
                )
            }
        }
    }

    // EPG Timeshift Dialog
    if (showEpgShiftDialog) {
        DialogSurface(
            onDismissRequest = { showEpgShiftDialog = false },
            title = "Desplazamiento EPG (Timeshift)",
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (-6..6).forEach { hours ->
                    val label = when {
                        hours == 0 -> "0 horas (Hora exacta de la guía)"
                        hours > 0 -> "+$hours horas"
                        else -> "$hours horas"
                    }
                    DialogOption(
                        text = label,
                        selected = preferences.epgTimeShiftHours == hours,
                        onClick = {
                            XtreamPreferencesRepository.setEpgTimeShiftHours(hours)
                            showEpgShiftDialog = false
                        },
                    )
                }
            }
            DialogButtons {
                DialogButton(
                    text = "Cerrar",
                    style = DialogButtonStyle.Secondary,
                    onClick = { showEpgShiftDialog = false },
                )
            }
        }
    }
}

@Composable
private fun ActiveServerHeroCard(
    activeServer: XtreamServerAccount?,
    userInfo: XtreamUserInfo?,
    isCheckingStatus: Boolean,
    serversCount: Int,
    onManageServersClick: () -> Unit,
) {
    NuvioSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row: Icon + Server Name + Active Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeServer?.name ?: "Sin servidor configurado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (activeServer?.serverType) {
                            "xtream" -> "Xtream Codes API"
                            "m3u_file" -> "Lista M3U Local"
                            "m3u_url" -> "Lista M3U Remota"
                            else -> "No hay servidor activo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Active Status Pill
                if (activeServer != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2ED573)),
                            )
                            Text(
                                text = "Activo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Real Server Info / Metadata Chips
            if (activeServer != null) {
                val server = activeServer
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (server.serverType == "xtream") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ServerInfoBadge(label = "Usuario", value = server.username)
                                ServerInfoBadge(
                                    label = "Estado",
                                    value = when {
                                        isCheckingStatus -> "Verificando..."
                                        userInfo?.status != null -> userInfo.status.orEmpty().replaceFirstChar { it.uppercase() }
                                        else -> "Conectado"
                                    },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ServerInfoBadge(
                                    label = "Conexiones",
                                    value = if (userInfo != null) {
                                        "${userInfo.active_cons ?: "1"} / ${userInfo.max_connections ?: "1"}"
                                    } else {
                                        "1 activa"
                                    },
                                )
                                ServerInfoBadge(
                                    label = "Vencimiento",
                                    value = formatExpiration(userInfo?.exp_date),
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ServerInfoBadge(
                                    label = "Tipo",
                                    value = if (server.serverType == "m3u_file") "Archivo M3U" else "Enlace URL",
                                )
                                ServerInfoBadge(label = "Protocolo", value = "M3U Plus / HLS")
                            }
                        }
                    }
                }
            }

            // Action Button: Gestionar Servidores
            FilledTonalButton(
                onClick = onManageServersClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gestionar Servidores ($serversCount configurado${if (serversCount != 1) "s" else ""})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ServerInfoBadge(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatExpiration(expDate: String?): String {
    if (expDate.isNullOrBlank()) return "Ilimitada"
    val timestampSec = expDate.toLongOrNull() ?: return expDate
    if (timestampSec <= 0L) return "Ilimitada"
    val nowSec = kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
    val diffDays = (timestampSec - nowSec) / 86400L
    return when {
        diffDays > 30 -> "${diffDays / 30} meses restantes"
        diffDays > 0 -> "$diffDays días restantes"
        diffDays == 0L -> "Expira hoy"
        else -> "Expirada"
    }
}
