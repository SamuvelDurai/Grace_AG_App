package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compresses picked profile photos into compact JPEG Data URIs so they sync cleanly
 * via Cloud Firestore across believer, pastor, and admin devices without requiring Firebase Storage.
 */
object ProfilePhotoHelper {

  private const val TAG = "ProfilePhotoHelper"
  const val MAX_IMAGE_DIMENSION = 256
  const val MAX_DATA_URI_CHARS = 120_000
  private const val MAX_FIRESTORE_SAFE_CHARS = 190_000

  /**
   * Returns true if the URL is a valid, cross-device displayable photo (Base64 data URI or HTTP/HTTPS URL).
   * Rejects device-local content:// or file:// URIs that cannot be rendered on other devices.
   */
  fun isCrossDevicePhotoUrl(photoUrl: String?): Boolean {
    val clean = photoUrl?.trim().orEmpty()
    if (clean.isEmpty()) return false
    return clean.startsWith("data:image/", ignoreCase = true) ||
      clean.startsWith("https://", ignoreCase = true) ||
      clean.startsWith("http://", ignoreCase = true)
  }

  /**
   * Picks the best displayable photo URL from multiple candidate sources.
   * Prefers uploaded Base64 data URIs or valid remote URLs over empty or device-local URIs.
   */
  fun resolvePhotoUrl(vararg candidates: String?): String {
    val cleaned = candidates.mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
    // Prefer an uploaded data URI or HTTPS/HTTP URL first
    val crossDevice = cleaned.firstOrNull { isCrossDevicePhotoUrl(it) }
    if (crossDevice != null) return crossDevice
    return ""
  }

  fun encodeUriToCompressedDataUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    maxDataUriChars: Int = MAX_DATA_URI_CHARS
  ): String? {
    val decoded: Bitmap = decodeUriBitmapSafely(context, uri, maxDimension) ?: return null
    return try {
      encodeBitmapToCompressedDataUri(decoded, maxDimension, maxDataUriChars)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to compress bitmap for $uri", e)
      null
    }
  }

  private fun decodeUriBitmapSafely(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val resolver = context.contentResolver

    // Primary path on Android 9+ (API 28+): ImageDecoder handles HEIC, WebP, PNG, JPEG,
    // Google Photos cloud URIs, and EXIF rotation automatically in a single open.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      try {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
          decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
          decoder.isMutableRequired = false
          val srcW = info.size.width
          val srcH = info.size.height
          val largest = max(srcW, srcH)
          if (largest > maxDimension * 2 && largest > 0) {
            val ratio = (maxDimension * 2).toFloat() / largest.toFloat()
            val targetW = (srcW * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (srcH * ratio).roundToInt().coerceAtLeast(1)
            decoder.setTargetSize(targetW, targetH)
          }
        }
        if (bitmap.width > 0 && bitmap.height > 0) {
          return bitmap
        }
      } catch (e: Exception) {
        Log.w(TAG, "ImageDecoder failed for $uri, falling back to BitmapFactory", e)
      }
    }

    // Fallback path: Read URI bytes ONCE into memory so one-shot/cloud streams never fail on re-open.
    val rawBytes = try {
      resolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
      Log.e(TAG, "Unable to open input stream for $uri", e)
      null
    } ?: return null

    if (rawBytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension * 2 ||
      bounds.outHeight / sampleSize > maxDimension * 2
    ) {
      sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null
    return applyExifRotationFromBytes(rawBytes, decoded)
  }

  fun encodeBitmapToCompressedDataUri(
    source: Bitmap,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    maxDataUriChars: Int = MAX_DATA_URI_CHARS
  ): String {
    val safeLimit = maxDataUriChars.coerceAtMost(MAX_FIRESTORE_SAFE_CHARS)
    val dimensionSteps = listOf(maxDimension, 200, 160, 128).distinct().filter { it > 0 }
    val qualitySteps = intArrayOf(78, 64, 50, 38, 28)

    var bestBytes = ByteArray(0)
    for (dim in dimensionSteps) {
      val scaled = scaleDownIfNeeded(source, dim)
      val opaque = ensureOpaqueSoftwareBitmap(scaled)
      for (q in qualitySteps) {
        val baos = ByteArrayOutputStream()
        opaque.compress(Bitmap.CompressFormat.JPEG, q, baos)
        bestBytes = baos.toByteArray()
        val estimatedLen = 23 + ((bestBytes.size + 2) / 3) * 4
        if (estimatedLen <= safeLimit) {
          val base64 = Base64.encodeToString(bestBytes, Base64.NO_WRAP)
          return "data:image/jpeg;base64,$base64"
        }
      }
    }

    val base64 = Base64.encodeToString(bestBytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
  }

  fun decodeDataUriBitmap(photoUrl: String): Bitmap? {
    val trimmed = photoUrl.trim()
    if (!trimmed.startsWith("data:image", ignoreCase = true)) return null
    val marker = "base64,"
    val idx = trimmed.indexOf(marker, ignoreCase = true)
    if (idx < 0) return null
    val encoded = trimmed.substring(idx + marker.length)
      .replace("\n", "")
      .replace("\r", "")
      .replace(" ", "")
      .trim()
    if (encoded.isEmpty()) return null
    return try {
      val bytes = try {
        Base64.decode(encoded, Base64.DEFAULT)
      } catch (_: IllegalArgumentException) {
        Base64.decode(encoded, Base64.URL_SAFE)
      }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  private fun ensureOpaqueSoftwareBitmap(source: Bitmap): Bitmap {
    val w = source.width.coerceAtLeast(1)
    val h = source.height.coerceAtLeast(1)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.WHITE)
    val safeSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
      source.copy(Bitmap.Config.ARGB_8888, false)
    } else {
      source
    }
    canvas.drawBitmap(safeSource, 0f, 0f, null)
    return out
  }

  private fun scaleDownIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
    val largest = max(source.width, source.height)
    if (largest <= maxDimension || largest <= 0) return source
    val ratio = maxDimension.toFloat() / largest.toFloat()
    val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
    val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
  }

  private fun applyExifRotationFromBytes(rawBytes: ByteArray, bitmap: Bitmap): Bitmap {
    return try {
      val orientation = ByteArrayInputStream(rawBytes).use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
      }
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
