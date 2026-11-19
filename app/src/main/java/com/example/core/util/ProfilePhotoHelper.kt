package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compresses picked profile photos into compact JPEG Data URIs so they sync cleanly
 * via Cloud Firestore across believer, pastor, and admin devices without requiring Firebase Storage.
 */
object ProfilePhotoHelper {

  const val MAX_IMAGE_DIMENSION = 256
  const val MAX_DATA_URI_CHARS = 140_000

  fun encodeUriToCompressedDataUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    maxDataUriChars: Int = MAX_DATA_URI_CHARS
  ): String? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, bounds)
    } ?: return null

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension * 2 ||
      bounds.outHeight / sampleSize > maxDimension * 2
    ) {
      sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: return null

    val rotated = applyExifRotation(context, uri, decoded)
    return encodeBitmapToCompressedDataUri(rotated, maxDimension, maxDataUriChars)
  }

  fun encodeBitmapToCompressedDataUri(
    source: Bitmap,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    maxDataUriChars: Int = MAX_DATA_URI_CHARS
  ): String {
    val scaled = scaleDownIfNeeded(source, maxDimension)
    val qualities = intArrayOf(76, 62, 48, 36)
    var bestBytes = ByteArray(0)
    for (q in qualities) {
      val baos = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, q, baos)
      bestBytes = baos.toByteArray()
      val estimatedLen = 23 + ((bestBytes.size + 2) / 3) * 4
      if (estimatedLen <= maxDataUriChars) {
        break
      }
    }
    val base64 = Base64.encodeToString(bestBytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
  }

  fun decodeDataUriBitmap(photoUrl: String): Bitmap? {
    if (!photoUrl.startsWith("data:image", ignoreCase = true)) return null
    val marker = "base64,"
    val idx = photoUrl.indexOf(marker, ignoreCase = true)
    if (idx < 0) return null
    val encoded = photoUrl.substring(idx + marker.length).trim()
    if (encoded.isEmpty()) return null
    return try {
      val bytes = Base64.decode(encoded, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: IllegalArgumentException) {
      null
    }
  }

  private fun scaleDownIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
    val largest = max(source.width, source.height)
    if (largest <= maxDimension || largest <= 0) return source
    val ratio = maxDimension.toFloat() / largest.toFloat()
    val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
    val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
  }

  private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
      val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
      } ?: ExifInterface.ORIENTATION_NORMAL

      val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
      }
      if (degrees == 0f) {
        bitmap
      } else {
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      }
    } catch (_: Exception) {
      bitmap
    }
  }
}
