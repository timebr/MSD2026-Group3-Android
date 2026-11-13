package com.example.oweowe.ui.components

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-private folders. Nothing is uploaded anywhere (FR2, FR10). */
private const val RECEIPT_DIR = "receipts"
private const val AVATAR_DIR = "avatars"

/**
 * Copies a picked image into app-private storage and returns its absolute path,
 * or null when the image cannot be read.
 *
 * Copying (instead of keeping the content Uri) is what makes the image survive
 * leaving the screen and restarting the device: the photo picker only grants
 * temporary read access to the picked file.
 */
suspend fun copyReceiptToAppStorage(context: Context, uri: Uri): String? =
    copyImageToAppStorage(context, uri, RECEIPT_DIR)

/** Same, for the profile photo (FR2). */
suspend fun copyAvatarToAppStorage(context: Context, uri: Uri): String? =
    copyImageToAppStorage(context, uri, AVATAR_DIR)

/**
 * Shared by every image the app keeps on the device (receipts and profile
 * photos): the file name is random, so two picked files can never overwrite
 * each other.
 */
private suspend fun copyImageToAppStorage(context: Context, uri: Uri, folder: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (opened) file.absolutePath else null
        }.getOrNull()
    }
