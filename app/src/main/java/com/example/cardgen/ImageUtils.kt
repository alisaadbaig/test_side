package com.example.cardgen

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException

object ImageUtils {

    /** Loads a (downsampled) bitmap from a content [uri] for preview thumbnails. */
    fun loadThumbnail(context: Context, uri: Uri, maxSize: Int = 512): Bitmap? {
        return try {
            // First pass: read bounds only.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val larger = maxOf(bounds.outWidth, bounds.outHeight)
            while (larger / sample > maxSize) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves [bitmap] as a PNG into the device's Pictures/CardGen folder via
     * MediaStore. Returns the new content Uri, or null on failure.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/CardGen"
            )
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("Failed to encode bitmap.")
                }
            } ?: throw IOException("Could not open output stream.")
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }
}
