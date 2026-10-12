package com.nuvio.app.core.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit

@Composable
expect fun rememberM3uFilePicker(onFilePicked: (fileName: String, content: String) -> Unit): () -> Unit
