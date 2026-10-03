package com.nuvio.app.core.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.nuvio.app.NuvioApplication

actual fun isTvDevice(): Boolean {
    return try {
        val app = NuvioApplication.instance
        val uiModeManager = app.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        app.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    } catch (_: Throwable) {
        false
    }
}
