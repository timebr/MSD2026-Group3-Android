package com.example.oweowe.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Largest edge kept when decoding a picked image, so a big photo cannot OOM the app. */
private const val MAX_IMAGE_DIMENSION = 1024

/** Cache folder that holds a photo between taking it and submitting the form. */
private const val CAMERA_DIR = "camera"

/** Opens the Android photo picker; no permission is required. */
@Composable
fun rememberImagePicker(onPicked: (Uri?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> onPicked(uri) }
    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}

/**
 * FR10: opens a camera app to take the receipt photo. The capture is written
 * to a file in the cache (a FileProvider Uri, because the camera app writes it
 * for us), and [onCaptured] is called with null when the user cancels — the
 * empty file is deleted then. No CAMERA permission is needed: the camera app
 * takes the photo.
 */
@Composable
fun rememberCameraCapture(onCaptured: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    var target by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(TakePictureContract()) { saved ->
        val file = target
        target = null
        when {
            saved && file != null ->
                onCaptured(FileProvider.getUriForFile(context, fileProviderAuthority(context), file))
            else -> {
                file?.delete()
                onCaptured(null)
            }
        }
    }
    return remember(launcher, context) {
        {
            val dir = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            target = file
            launcher.launch(FileProvider.getUriForFile(context, fileProviderAuthority(context), file))
        }
    }
}

/** True when a camera app can handle a capture, so the button can be hidden. */
fun canTakePhoto(context: Context): Boolean =
    context.packageManager
        .queryIntentActivities(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
        .isNotEmpty()

private fun fileProviderAuthority(context: Context) = "${context.packageName}.fileprovider"

/** ACTION_IMAGE_CAPTURE plus the write grant the camera app needs for the output Uri. */
private class TakePictureContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

/** Decodes a content Uri into an ImageBitmap off the main thread, downscaled. */
@Composable
fun rememberImageBitmap(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) { decodeDownscaled(context, uri) }
        }
    }
    return bitmap
}

/** The display name of a content Uri, or null. Used to label a picked file. */
fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
} catch (_: Exception) {
    null
}

private fun decodeDownscaled(context: Context, uri: Uri): ImageBitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (largest / sample > MAX_IMAGE_DIMENSION) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null
    return bitmap.asImageBitmap()
}
