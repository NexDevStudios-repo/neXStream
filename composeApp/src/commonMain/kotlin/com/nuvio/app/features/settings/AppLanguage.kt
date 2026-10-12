package com.nuvio.app.features.settings

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.lang_english
import nuvio.composeapp.generated.resources.lang_spanish
import nuvio.composeapp.generated.resources.settings_appearance_app_language_device
import org.jetbrains.compose.resources.StringResource

enum class AppLanguage(
    val code: String,
    val labelRes: StringResource,
) {
    DEVICE("device", Res.string.settings_appearance_app_language_device),
    SPANISH("es", Res.string.lang_spanish),
    ENGLISH("en", Res.string.lang_english),
    ;

    fun isRightToLeft(): Boolean = false

    companion object {
        fun fromCode(code: String?): AppLanguage = when {
            code == null -> DEVICE
            code.startsWith("es", ignoreCase = true) -> SPANISH
            code.startsWith("en", ignoreCase = true) -> ENGLISH
            code.equals("device", ignoreCase = true) -> DEVICE
            else -> DEVICE
        }
    }
}
