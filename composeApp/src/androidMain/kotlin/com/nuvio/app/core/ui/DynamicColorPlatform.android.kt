package com.nuvio.app.core.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformDynamicColorScheme(isDark: Boolean, amoled: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (amoled && isDark) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
            )
        } else {
            base
        }
    }
    return null
}
