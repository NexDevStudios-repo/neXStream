package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioMediaPlaceholder
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.xtream.ChannelDrawerItem
import com.nuvio.app.features.xtream.XtreamCategory
import com.nuvio.app.features.xtream.XtreamRepository
import kotlinx.coroutines.launch

@Composable
fun PlayerChannelsPanel(
    visible: Boolean,
    currentChannelId: String? = null,
    currentChannelName: String,
    onChannelSelected: (id: String, name: String, streamUrl: String, logoUrl: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<String?>("all") }
    var channels by remember { mutableStateOf<List<ChannelDrawerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val libraryState by LibraryRepository.uiState.collectAsStateWithLifecycle()
    val categoryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val channelListState = androidx.compose.foundation.lazy.rememberLazyListState()

    var activeCategoryJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(visible, currentChannelId) {
        if (!visible) return@LaunchedEffect
        activeCategoryJob?.cancel()
        isLoading = true
        try {
            kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                val cats = XtreamRepository.getDrawerCategories()
                categories = cats
                val matchedCategory = XtreamRepository.resolveChannelCategory(currentChannelId, currentChannelName, cats)
                val initialCategory = matchedCategory ?: cats.firstOrNull()?.categoryId ?: "all"
                selectedCategoryId = initialCategory
                val loadedChannels = XtreamRepository.getChannelsForDrawer(initialCategory)
                channels = loadedChannels

                // Auto-scroll category chip into view
                if (initialCategory != "all") {
                    val catIndex = cats.indexOfFirst { it.categoryId == initialCategory }
                    if (catIndex >= 0) {
                        categoryListState.scrollToItem(maxOf(0, catIndex + 1))
                    }
                } else {
                    categoryListState.scrollToItem(0)
                }

                // Auto-scroll active channel into view
                val activeIndex = loadedChannels.indexOfFirst {
                    (!currentChannelId.isNullOrBlank() && it.id == currentChannelId) ||
                        it.name.equals(currentChannelName, ignoreCase = true)
                }
                if (activeIndex >= 0) {
                    channelListState.scrollToItem(maxOf(0, activeIndex - 1))
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        } finally {
            isLoading = false
        }
    }

    PlayerSidePanel(
        visible = visible,
        onDismiss = onDismiss,
        width = 460.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Canales de TV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        text = "Buscar canal...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            val onSelectCategory: (String?) -> Unit = { catId ->
                if (selectedCategoryId != catId) {
                    selectedCategoryId = catId
                    activeCategoryJob?.cancel()
                    activeCategoryJob = coroutineScope.launch {
                        isLoading = true
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(6_000L) {
                                channels = XtreamRepository.getChannelsForDrawer(catId)
                                channelListState.scrollToItem(0)
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }

            // Categories horizontal scroll
            if (categories.isNotEmpty()) {
                LazyRow(
                    state = categoryListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        CategoryChip(
                            title = "Todos",
                            selected = selectedCategoryId == "all" || selectedCategoryId == null,
                            onClick = { onSelectCategory("all") },
                        )
                    }
                    items(categories) { cat ->
                        CategoryChip(
                            title = cat.categoryName,
                            selected = selectedCategoryId == cat.categoryId,
                            onClick = { onSelectCategory(cat.categoryId) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Channel List
            val filteredChannels = remember(channels, searchQuery) {
                if (searchQuery.isBlank()) channels
                else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    NuvioLoadingIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No se encontraron canales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = channelListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isPlaying = (!currentChannelId.isNullOrBlank() && channel.id == currentChannelId) ||
                            channel.name.equals(currentChannelName, ignoreCase = true)
                        val isSaved = LibraryRepository.isSaved(channel.id, "channel")

                        ChannelItemRow(
                            channel = channel,
                            isPlaying = isPlaying,
                            isSaved = isSaved,
                            onToggleFavorite = {
                                val item = LibraryItem(
                                    id = channel.id,
                                    type = "channel",
                                    name = channel.name,
                                    logo = channel.logoUrl,
                                    poster = channel.logoUrl,
                                    savedAtEpochMs = 0L,
                                )
                                LibraryRepository.toggleLocalSaved(item)
                            },
                            onClick = {
                                channel.categoryId?.let { catId ->
                                    XtreamRepository.registerChannelCategory(channel.id, catId)
                                    XtreamRepository.registerChannelCategory(channel.name, catId)
                                }
                                onChannelSelected(channel.id, channel.name, channel.streamUrl, channel.logoUrl)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChannelItemRow(
    channel: ChannelDrawerItem,
    isPlaying: Boolean,
    isSaved: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Channel Logo / Icon
            var imageLoadError by remember(channel.logoUrl) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrBlank() && !imageLoadError) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                        onError = { imageLoadError = true },
                    )
                } else {
                    NuvioMediaPlaceholder(
                        title = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        isChannel = true,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel Title & Number
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (channel.channelNumber != null && channel.channelNumber > 0) {
                        Text(
                            text = "${channel.channelNumber}. ",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isPlaying) {
                    Text(
                        text = "● Reproduciendo ahora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                } else if (!channel.categoryId.isNullOrBlank()) {
                    Text(
                        text = channel.categoryId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (isSaved) "Quitar de biblioteca" else "Guardar en biblioteca",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
