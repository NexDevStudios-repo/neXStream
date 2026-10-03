package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.sourcemode.SourceMode
import com.nuvio.app.features.sourcemode.SourceModeRepository
import com.nuvio.app.features.xtream.XtreamRepository
import com.nuvio.app.features.xtream.XtreamServerAccount
import kotlinx.coroutines.launch

@Composable
fun XtreamSettingsPage(
    isTablet: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val coroutineScope = rememberCoroutineScope()
    val sourceMode by SourceModeRepository.mode.collectAsStateWithLifecycle()
    val servers by XtreamRepository.servers.collectAsStateWithLifecycle()
    val activeServer by XtreamRepository.activeServer.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NuvioBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Fuente de Contenido",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = tokens.colors.textPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsGroup(isTablet = isTablet) {
                    Text(
                        text = "Modo de Fuente Activo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.colors.textPrimary
                        ),
                        modifier = Modifier.padding(16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sourceMode == SourceMode.PLUGINS) tokens.colors.surfaceCard else tokens.colors.surface)
                            .clickable { SourceModeRepository.setSourceMode(SourceMode.PLUGINS) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Extension,
                            contentDescription = null,
                            tint = if (sourceMode == SourceMode.PLUGINS) tokens.colors.accent else tokens.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plugins & Addons",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.colors.textPrimary
                                )
                            )
                            Text(
                                text = "Scrapers de torrents, debrid, metadatos y catálogo Stremio",
                                style = MaterialTheme.typography.bodySmall.copy(color = tokens.colors.textSecondary)
                            )
                        }
                        if (sourceMode == SourceMode.PLUGINS) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = tokens.colors.accent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sourceMode == SourceMode.XTREAM) tokens.colors.surfaceCard else tokens.colors.surface)
                            .clickable { SourceModeRepository.setSourceMode(SourceMode.XTREAM) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = if (sourceMode == SourceMode.XTREAM) tokens.colors.accent else tokens.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Xtream Codes (IPTV)",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.colors.textPrimary
                                )
                            )
                            Text(
                                text = "Canales en vivo, películas VOD y series de tu proveedor IPTV",
                                style = MaterialTheme.typography.bodySmall.copy(color = tokens.colors.textSecondary)
                            )
                        }
                        if (sourceMode == SourceMode.XTREAM) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = tokens.colors.accent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (sourceMode == SourceMode.XTREAM) {
                item {
                    SettingsGroup(isTablet = isTablet) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Servidores Xtream Configurados",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = tokens.colors.textPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    serverName = ""
                                    serverUrl = ""
                                    username = ""
                                    password = ""
                                    addError = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.colors.accent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Añadir Servidor")
                            }
                        }

                        if (servers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tienes ningún servidor Xtream configurado.\nToca en 'Añadir Servidor' para ingresar tus credenciales.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = tokens.colors.textSecondary),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(servers, key = { it.id }) { server ->
                    val isSelected = activeServer?.id == server.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { XtreamRepository.selectServer(server.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tokens.colors.surfaceCard else tokens.colors.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dns,
                                contentDescription = null,
                                tint = if (isSelected) tokens.colors.accent else tokens.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.colors.textPrimary
                                    )
                                )
                                Text(
                                    text = server.serverUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = tokens.colors.textSecondary
                                    )
                                )
                                Text(
                                    text = "Usuario: ${server.username}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = tokens.colors.textSecondary
                                    )
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "ACTIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.colors.accent
                                    ),
                                    modifier = Modifier
                                        .background(
                                            tokens.colors.accent.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            IconButton(
                                onClick = { XtreamRepository.deleteServer(server.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = tokens.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = {
                Text(
                    text = "Añadir Servidor Xtream",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Nombre del Servidor") },
                        placeholder = { Text("Ej: Mi IPTV") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL del Servidor") },
                        placeholder = { Text("http://linea.ejemplo.com:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    addError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isAdding) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                            addError = "Completa todos los campos obligatorios"
                            return@Button
                        }
                        isAdding = true
                        addError = null
                        coroutineScope.launch {
                            val result = XtreamRepository.addServer(
                                name = serverName.ifBlank { "Xtream IPTV" },
                                serverUrl = serverUrl.trim(),
                                username = username.trim(),
                                password = password.trim()
                            )
                            isAdding = false
                            if (result.isSuccess) {
                                showAddDialog = false
                            } else {
                                addError = result.exceptionOrNull()?.message ?: "Error al autenticar"
                            }
                        }
                    },
                    enabled = !isAdding
                ) {
                    Text("Conectar y Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isAdding
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
