package com.nuvio.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioSurfaceCard
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TELEGRAM_URL = "https://t.me/+C_ZyoLwgMi1iNjRh"
private const val INSTAGRAM_URL = "https://instagram.com/nexdevstudios_"
private const val X_TWITTER_URL = "https://x.com/nexdevstudios_"
private const val EMAIL_URL = "mailto:soportenexdevstudiosoficial@gmail.com"
private const val CAFECITO_URL = "https://cafecito.app/nexdevstudios"

@Composable
fun SupportersContributorsSettingsScreen(
    onBack: () -> Unit,
) {
    NuvioScreen(
        modifier = Modifier.fillMaxSize(),
    ) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.compose_settings_page_supporters_contributors),
                onBack = onBack,
            )
        }
        supportersContributorsContent(isTablet = false)
    }
}

internal fun LazyListScope.supportersContributorsContent(
    isTablet: Boolean,
) {
    item {
        SupportPageBody(isTablet = isTablet)
    }
}

@Composable
private fun SupportPageBody(
    isTablet: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        // Hero Card
        NuvioSurfaceCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.support_hero_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.support_hero_subtitle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.support_hero_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Community & Contact Section
        SettingsSection(
            title = stringResource(Res.string.support_section_community),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SupportActionRow(
                    title = stringResource(Res.string.support_telegram_title),
                    description = stringResource(Res.string.support_telegram_subtitle),
                    iconPainter = painterResource(Res.drawable.ic_telegram),
                    iconContainerColor = Color(0xFF229ED9).copy(alpha = 0.14f),
                    url = TELEGRAM_URL,
                    isTablet = isTablet,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SupportActionRow(
                    title = stringResource(Res.string.support_instagram_title),
                    description = stringResource(Res.string.support_instagram_subtitle),
                    iconPainter = painterResource(Res.drawable.ic_instagram),
                    iconContainerColor = Color(0xFFE1306C).copy(alpha = 0.14f),
                    url = INSTAGRAM_URL,
                    isTablet = isTablet,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SupportActionRow(
                    title = stringResource(Res.string.support_x_title),
                    description = stringResource(Res.string.support_x_subtitle),
                    iconPainter = painterResource(Res.drawable.ic_x_twitter),
                    iconContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    url = X_TWITTER_URL,
                    isTablet = isTablet,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SupportActionRow(
                    title = stringResource(Res.string.support_email_title),
                    description = stringResource(Res.string.support_email_subtitle),
                    iconPainter = painterResource(Res.drawable.ic_email),
                    iconContainerColor = Color(0xFFEA4335).copy(alpha = 0.14f),
                    url = EMAIL_URL,
                    isTablet = isTablet,
                )
            }
        }

        // Sponsorship & Donations Section
        SettingsSection(
            title = stringResource(Res.string.support_section_donations),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SupportActionRow(
                    title = stringResource(Res.string.support_cafecito_title),
                    description = stringResource(Res.string.support_cafecito_subtitle),
                    iconPainter = painterResource(Res.drawable.ic_cafecito),
                    iconContainerColor = Color(0xFFFF813F).copy(alpha = 0.16f),
                    url = CAFECITO_URL,
                    isTablet = isTablet,
                )
            }
        }
    }
}

@Composable
private fun SupportActionRow(
    title: String,
    description: String,
    iconPainter: Painter,
    iconContainerColor: Color,
    url: String,
    isTablet: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 14.dp),
    ) {
        Surface(
            modifier = Modifier.size(if (isTablet) 46.dp else 40.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconContainerColor,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTablet) 26.dp else 22.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            modifier = Modifier
                .size(if (isTablet) 20.dp else 18.dp)
                .alpha(0.6f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
