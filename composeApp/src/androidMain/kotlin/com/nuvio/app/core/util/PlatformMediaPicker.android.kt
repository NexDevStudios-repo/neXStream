package com.nuvio.app.core.util

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onImagePicked(uri.toString())
        }
    }
    return { launcher.launch("image/*") }
}

@Composable
actual fun rememberM3uFilePicker(onFilePicked: (fileName: String, content: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "Lista IPTV"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "Lista IPTV"
                    }
                }
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }.orEmpty()

                if (content.isNotBlank()) {
                    onFilePicked(fileName.substringBeforeLast('.'), content)
                }
            } catch (_: Exception) {
            }
        }
    }
    return { launcher.launch("*/*") }
}
