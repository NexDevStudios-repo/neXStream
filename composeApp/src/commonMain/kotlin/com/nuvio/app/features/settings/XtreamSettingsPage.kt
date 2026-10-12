package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.xtream.XtreamRepository
import com.nuvio.app.features.xtream.XtreamServerAccount
import kotlinx.coroutines.launch

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.nuvio.app.core.util.rememberM3uFilePicker

private enum class IptvSourceMode(val label: String) {
    XTREAM("Xtream Codes"),
    M3U_URL("Enlace M3U"),
    M3U_FILE("Archivo M3U"),
}

internal fun LazyListScope.xtreamSettingsContent(
    isTablet: Boolean,
) {
    item {
        XtreamSettingsBody(isTablet = isTablet)
    }
}

@Composable
private fun XtreamSettingsBody(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val servers by XtreamRepository.servers.collectAsStateWithLifecycle()
    val activeServer by XtreamRepository.activeServer.collectAsStateWithLifecycle()

    var selectedMode by rememberSaveable { mutableStateOf(IptvSourceMode.XTREAM) }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var serverName by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var m3uUrl by rememberSaveable { mutableStateOf("") }
    var m3uContent by rememberSaveable { mutableStateOf("") }

    val pickM3uFile = rememberM3uFilePicker { fileName, content ->
        serverName = fileName
        m3uContent = content
        addError = null
    }

    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // --- SECCIÓN 1: Fuentes / Servidores Configurados ---
        SettingsSection(
            title = "Fuentes IPTV Configuradas",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (servers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No tienes ninguna fuente IPTV configurada.\nAgrega un servidor Xtream o una lista M3U a continuación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    servers.forEachIndexed { index, server ->
                        val isSelected = activeServer?.id == server.id
                        XtreamServerRow(
                            server = server,
                            isSelected = isSelected,
                            isTablet = isTablet,
                            onSelect = { XtreamRepository.selectServer(server.id) },
                            onDelete = { XtreamRepository.deleteServer(server.id) },
                        )
                        if (index < servers.lastIndex) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 2: Selector de Modo y Formulario ---
        SettingsSection(
            title = "Añadir Fuente IPTV",
            isTablet = isTablet,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                    // Selector de Modo Nativo M3 Expressive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IptvSourceMode.entries.forEach { mode ->
                            val isCurrent = mode == selectedMode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .clickable {
                                        selectedMode = mode
                                        addError = null
                                    },
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = RoundedCornerShape(9999.dp),
                                border = if (isCurrent) {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                },
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    when (selectedMode) {
                        IptvSourceMode.XTREAM -> {
                            Text(
                                text = "Ingresa los datos proporcionados por tu proveedor Xtream Codes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Nombre de la Cuenta (opcional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = serverName,
                                    onValueChange = { serverName = it },
                                    placeholder = "Ej: Mi IPTV Principal",
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "URL del Servidor *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it; addError = null },
                                    placeholder = "http://ejemplo.com:8080",
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Usuario *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = username,
                                    onValueChange = { username = it; addError = null },
                                    placeholder = "usuario123",
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Contraseña *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                SettingsSecretTextField(
                                    value = password,
                                    onValueChange = { password = it; addError = null },
                                    label = "Contraseña",
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = addError != null && password.isBlank(),
                                )
                            }
                        }

                        IptvSourceMode.M3U_URL -> {
                            Text(
                                text = "Ingresa el enlace directo a tu lista M3U o M3U8.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Nombre de la Lista (opcional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = serverName,
                                    onValueChange = { serverName = it },
                                    placeholder = "Ej: Mi Lista M3U",
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "URL de la Lista M3U / M3U8 *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = m3uUrl,
                                    onValueChange = { m3uUrl = it; addError = null },
                                    placeholder = "https://ejemplo.com/lista.m3u",
                                )
                            }
                        }

                        IptvSourceMode.M3U_FILE -> {
                            Text(
                                text = "Selecciona un archivo de lista M3U o M3U8 desde el almacenamiento de tu dispositivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Button(
                                onClick = pickM3uFile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            ) {
                                Text(
                                    text = if (m3uContent.isBlank()) "Seleccionar archivo .m3u / .m3u8" else "Cambiar archivo seleccionado",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            if (m3uContent.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "Archivo cargado correctamente",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "Nombre: $serverName (${m3uContent.lines().size} líneas / ${(m3uContent.length / 1024).coerceAtLeast(1)} KB)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Nombre de la Lista",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                NuvioInputField(
                                    value = serverName,
                                    onValueChange = { serverName = it },
                                    placeholder = "Ej: Mi Lista Local",
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = addError != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = addError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val canSubmit = when (selectedMode) {
                        IptvSourceMode.XTREAM -> serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                        IptvSourceMode.M3U_URL -> m3uUrl.isNotBlank()
                        IptvSourceMode.M3U_FILE -> m3uContent.isNotBlank()
                    }

                    NuvioPrimaryButton(
                        text = if (isAdding) "Conectando..." else "Añadir Fuente IPTV",
                        enabled = !isAdding && canSubmit,
                        onClick = {
                            isAdding = true
                            addError = null
                            coroutineScope.launch {
                                val result = when (selectedMode) {
                                    IptvSourceMode.XTREAM -> {
                                        XtreamRepository.addServer(
                                            name = serverName.trim(),
                                            serverUrl = serverUrl.trim(),
                                            username = username.trim(),
                                            password = password.trim(),
                                        )
                                    }
                                    IptvSourceMode.M3U_URL -> {
                                        XtreamRepository.addM3uUrl(
                                            name = serverName.trim(),
                                            m3uUrl = m3uUrl.trim(),
                                        )
                                    }
                                    IptvSourceMode.M3U_FILE -> {
                                        XtreamRepository.addM3uContent(
                                            name = serverName.trim(),
                                            content = m3uContent.trim(),
                                        )
                                    }
                                }
                                isAdding = false
                                if (result.isSuccess) {
                                    serverName = ""
                                    serverUrl = ""
                                    username = ""
                                    password = ""
                                    m3uUrl = ""
                                    m3uContent = ""
                                } else {
                                    addError = result.exceptionOrNull()?.message ?: "Error al procesar la fuente IPTV."
                                }
                            }
                        },
                    )
                }
        }
    }
}

@Composable
private fun XtreamServerRow(
    server: XtreamServerAccount,
    isSelected: Boolean,
    isTablet: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val iconSize = if (isTablet) 42.dp else 36.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = tokens.shapes.compactCard,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(NuvioTokens.Radius.xs),
                    ) {
                        Text(
                            text = "ACTIVO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            val subtitle = when (server.serverType) {
                "m3u_file" -> "Archivo M3U Local"
                "m3u_url" -> "Lista M3U Remota"
                else -> "${server.serverUrl} · ${server.username}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Activo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Eliminar Servidor",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
