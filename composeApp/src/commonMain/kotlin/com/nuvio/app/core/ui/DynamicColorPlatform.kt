package com.nuvio.app.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun rememberPlatformDynamicColorScheme(isDark: Boolean, amoled: Boolean): ColorScheme?
