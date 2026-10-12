package com.nuvio.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
actual fun appIconPainter(icon: AppIconResource): Painter =
    rememberVectorPainter(
        image = when (icon) {
            AppIconResource.PlayerPlay -> Icons.Rounded.PlayArrow
            AppIconResource.PlayerPause -> Icons.Rounded.Pause
            AppIconResource.PlayerAspectRatio -> Icons.Rounded.AspectRatio
            AppIconResource.PlayerSubtitles -> Icons.Rounded.Subtitles
            AppIconResource.PlayerAudioFilled -> Icons.Rounded.VolumeUp
            AppIconResource.PlayerSource -> Icons.Rounded.Dns
            AppIconResource.PlayerEpisodes -> Icons.Rounded.VideoLibrary
            AppIconResource.LibraryAddPlus -> Icons.Rounded.Add
        }
    )
