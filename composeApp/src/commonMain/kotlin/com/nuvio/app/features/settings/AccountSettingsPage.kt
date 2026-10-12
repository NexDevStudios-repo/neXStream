package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.normalizedAvatarUrl
import com.nuvio.app.features.profiles.parseHexColor

internal fun LazyListScope.accountSettingsContent(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)? = null,
) {
    item {
        AccountSettingsBody(
            isTablet = isTablet,
            onSwitchProfile = onSwitchProfile,
        )
    }
}

@Composable
private fun AccountSettingsBody(
    isTablet: Boolean,
    onSwitchProfile: (() -> Unit)? = null,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val activeProfile = profileState.activeProfile

    val avatarColor = remember(activeProfile?.avatarColorHex) {
        activeProfile?.avatarColorHex?.let { parseHexColor(it) } ?: Color(0xFF6C5CE7)
    }
    val avatarUrl = remember(activeProfile?.avatarUrl) {
        normalizedAvatarUrl(activeProfile?.avatarUrl)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NuvioSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = "Modo Local y Privado",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = "neXStream está diseñada para funcionar 100% de forma local y privada en tu dispositivo. Tus perfiles, servidores IPTV, listas M3U y progreso de reproducción se almacenan exclusivamente en este equipo, sin necesidad de cuentas en la nube ni recopilación de datos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                )
            }
        }

        if (activeProfile != null) {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Perfil Activo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(avatarColor)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = activeProfile.name,
                                    modifier = Modifier.size(56.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else if (activeProfile.name.isNotBlank()) {
                                Text(
                                    text = activeProfile.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = activeProfile.name.ifBlank { "Perfil sin nombre" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (activeProfile.pinEnabled) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Protegido con PIN",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Text(
                                        text = "Sin bloqueo de PIN",
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

        if (onSwitchProfile != null) {
            Spacer(modifier = Modifier.height(4.dp))
            NuvioPrimaryButton(
                text = "Cambiar de Perfil",
                onClick = onSwitchProfile,
            )
        }
    }
}
