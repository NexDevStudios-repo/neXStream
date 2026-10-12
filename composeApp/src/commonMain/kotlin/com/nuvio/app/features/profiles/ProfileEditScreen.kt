package com.nuvio.app.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.util.rememberImagePicker
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.availableAppThemes
import com.nuvio.app.features.settings.ThemeSettingsRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    profile: NuvioProfile? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNew = profile == null
    val scope = rememberCoroutineScope()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val currentProfile = remember(profile?.profileIndex, profileState.profiles, profile) {
        profile?.let { snapshot ->
            profileState.profiles.find { it.profileIndex == snapshot.profileIndex } ?: snapshot
        }
    }

    var name by rememberSaveable { mutableStateOf(currentProfile?.name ?: "") }
    var avatarColorHex by rememberSaveable { mutableStateOf(currentProfile?.avatarColorHex ?: DEFAULT_PROFILE_COLOR_HEX) }
    var avatarUrl by rememberSaveable { mutableStateOf(currentProfile?.avatarUrl.orEmpty()) }
    var usesPrimaryAddons by rememberSaveable { mutableStateOf(currentProfile?.usesPrimaryAddons ?: false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinClear by remember { mutableStateOf(false) }

    val pickImage = rememberImagePicker { localUri ->
        avatarUrl = localUri
    }

    val customAvatarUrl = remember(avatarUrl) { normalizedAvatarUrl(avatarUrl) }
    val avatarUrlIsInvalid = avatarUrl.isNotBlank() && customAvatarUrl == null
    val currentColor = remember(avatarColorHex) { parseHexColor(avatarColorHex) }

    NuvioScreen(modifier = modifier) {
        stickyHeader {
            NuvioScreenHeader(
                title = if (isNew) {
                    stringResource(Res.string.profile_edit_add_title)
                } else {
                    stringResource(Res.string.profile_edit_edit_title)
                },
                onBack = onBack,
            )
        }

        item {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                .clickable { pickImage() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (customAvatarUrl != null) {
                                AsyncImage(
                                    model = customAvatarUrl,
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else if (name.isNotBlank()) {
                                Text(
                                    text = name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp),
                                    )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = name.ifBlank {
                                    if (isNew) stringResource(Res.string.profile_new)
                                    else stringResource(Res.string.profile_unnamed)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (customAvatarUrl != null) {
                                    "Foto personalizada activa"
                                } else {
                                    "Toca el círculo para elegir una foto"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    NuvioInputField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(Res.string.profile_name_placeholder),
                    )
                }
            }
        }

        item {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Foto de Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Selecciona una foto desde la galería de tu dispositivo o ingresa una URL web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = pickImage,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Text(
                                text = "Elegir foto local",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (customAvatarUrl != null) {
                            Button(
                                onClick = { avatarUrl = "" },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(
                                    text = "Quitar foto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    NuvioInputField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        placeholder = "O escribe una URL (https://...)",
                    )
                    if (avatarUrlIsInvalid) {
                        Text(
                            text = stringResource(Res.string.profile_avatar_url_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }



        if (!isNew) {
            item {
                NuvioSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(Res.string.profile_security),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (currentProfile?.pinEnabled == true) {
                                stringResource(Res.string.profile_security_pin_enabled)
                            } else {
                                stringResource(Res.string.profile_security_pin_disabled)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (currentProfile?.pinEnabled == true) {
                            NuvioPrimaryButton(
                                text = stringResource(Res.string.profile_remove_pin_lock),
                                onClick = { showPinClear = true },
                            )
                        } else {
                            NuvioPrimaryButton(
                                text = stringResource(Res.string.profile_set_pin_lock),
                                onClick = { showPinSetup = true },
                            )
                        }
                    }
                }
            }
        }

        val canConfigurePrimaryAddons = isNew || (currentProfile?.profileIndex ?: 0) > 1
        if (canConfigurePrimaryAddons) {
            item {
                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_use_primary_addons),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.profile_use_primary_addons_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = usesPrimaryAddons,
                            onCheckedChange = { usesPrimaryAddons = it },
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            NuvioPrimaryButton(
                text = if (isSaving) {
                    stringResource(Res.string.profile_saving)
                } else if (isNew) {
                    stringResource(Res.string.profile_create_profile)
                } else {
                    stringResource(Res.string.collections_editor_save_changes)
                },
                enabled = name.isNotBlank() && !avatarUrlIsInvalid && !isSaving,
                onClick = {
                    isSaving = true
                    scope.launch {
                        if (isNew) {
                            ProfileRepository.createProfile(
                                name = name,
                                avatarColorHex = avatarColorHex,
                                avatarId = null,
                                avatarUrl = customAvatarUrl,
                                usesPrimaryAddons = usesPrimaryAddons,
                            )
                        } else {
                            ProfileRepository.updateProfile(
                                profileIndex = currentProfile!!.profileIndex,
                                name = name,
                                avatarColorHex = avatarColorHex,
                                avatarId = null,
                                avatarUrl = customAvatarUrl,
                                profileBackgroundId = null,
                                profileBackgroundUrl = null,
                                usesPrimaryAddons = if ((currentProfile?.profileIndex ?: 0) <= 1) false else usesPrimaryAddons,
                            )
                        }
                        isSaving = false
                        onSaved()
                    }
                },
            )
        }

        if (!isNew && (currentProfile?.profileIndex ?: 0) > 1) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.profile_delete_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    NuvioStatusModal(
        title = stringResource(Res.string.profile_delete_title),
        message = stringResource(
            Res.string.profile_delete_confirm_message,
            currentProfile?.name.orEmpty(),
        ),
        isVisible = showDeleteConfirm,
        destructive = true,
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            showDeleteConfirm = false
            scope.launch {
                currentProfile?.let { ProfileRepository.deleteProfile(it.profileIndex) }
                onBack()
            }
        },
        onDismiss = { showDeleteConfirm = false },
    )

    if (showPinSetup && currentProfile != null) {
        PinSetupDialog(
            profileIndex = currentProfile.profileIndex,
            hasExistingPin = currentProfile.pinEnabled,
            onDone = {
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false },
        )
    }

    if (showPinClear && currentProfile != null) {
        PinEntryDialog(
            profileName = stringResource(Res.string.profile_remove_pin_for, currentProfile.name),
            onVerify = { pin -> ProfileRepository.clearPin(currentProfile.profileIndex, pin) },
            onVerified = {
                showPinClear = false
            },
            onDismiss = {
                showPinClear = false
            },
        )
    }
}

@Composable
fun PinSetupDialog(
    profileIndex: Int,
    hasExistingPin: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(if (hasExistingPin) "current" else "new") }
    var currentPin by remember { mutableStateOf("") }

    when (step) {
        "current" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_current_pin),
            onVerify = { pin -> ProfileRepository.verifyPin(profileIndex, pin) },
            onVerified = { pin ->
                currentPin = pin
                step = "new"
            },
            onDismiss = onDismiss,
        )

        "new" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_new_pin),
            onVerify = { pin ->
                ProfileRepository.setPin(
                    profileIndex = profileIndex,
                    pin = pin,
                    currentPin = currentPin.ifEmpty { null },
                )
            },
            onVerified = {
                onDone()
            },
            onDismiss = onDismiss,
        )
    }
}
