package com.example.oweowe.ui.components

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-private folder for receipt images. Nothing is uploaded anywhere (FR10). */
private const val RECEIPT_DIR = "receipts"

/**
 * Copies a picked image into app-private storage and returns its absolute path,
 * or null when the image cannot be read.
 *
 * Copying (instead of keeping the content Uri) is what makes the receipt
 * survive leaving the screen and restarting the device: the photo picker only
 * grants temporary read access to the picked file.
 */
suspend fun copyReceiptToAppStorage(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, RECEIPT_DIR).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (opened) file.absolutePath else null
        }.getOrNull()
    }
