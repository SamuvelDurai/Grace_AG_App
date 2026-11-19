package com.example.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast

tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

fun Context.launchActivitySafely(
  intent: Intent,
  fallbackMessage: String = "No compatible app available"
) {
  val activity = findActivity()
  val launchIntent = if (activity == null) {
    Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
  } else {
    intent
  }
  try {
    (activity ?: this).startActivity(launchIntent)
  } catch (_: Exception) {
    Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
  }
}

fun Context.launchUpiPayment(
  upiId: String,
  payeeName: String,
  amountInr: Double,
  transactionNote: String
) {
  val cleanUpi = upiId.trim()
  if (cleanUpi.isBlank()) {
    Toast.makeText(this, "Church UPI ID is not configured yet.", Toast.LENGTH_SHORT).show()
    return
  }
  val uri = Uri.Builder()
    .scheme("upi")
    .authority("pay")
    .appendQueryParameter("pa", cleanUpi)
    .appendQueryParameter("pn", payeeName.ifBlank { "GRACE AG" })
    .appendQueryParameter("am", String.format(java.util.Locale.US, "%.2f", amountInr))
    .appendQueryParameter("cu", "INR")
    .appendQueryParameter("tn", transactionNote.take(80))
    .build()
  val intent = Intent(Intent.ACTION_VIEW, uri)
  launchActivitySafely(
    Intent.createChooser(intent, "Pay with UPI App"),
    "No UPI app found on this device. Copy the UPI ID and paste your UTR after paying."
  )
}

object PhoneNumberHelper {

  data class CountryDialOption(
    val isoCode: String,
    val dialCode: String,
    val label: String,
    val minNationalDigits: Int = 7,
    val maxNationalDigits: Int = 12
  )

  val SUPPORTED_COUNTRIES = listOf(
    CountryDialOption("IN", "+91", "🇮🇳 +91 (India)", 10, 10),
    CountryDialOption("US", "+1", "🇺🇸 +1 (USA/CA)", 10, 10),
    CountryDialOption("GB", "+44", "🇬🇧 +44 (UK)", 10, 11),
    CountryDialOption("SG", "+65", "🇸🇬 +65 (Singapore)", 8, 8),
    CountryDialOption("AE", "+971", "🇦🇪 +971 (UAE)", 9, 9),
    CountryDialOption("MY", "+60", "🇲🇾 +60 (Malaysia)", 9, 10),
    CountryDialOption("LK", "+94", "🇱🇰 +94 (Sri Lanka)", 9, 9),
    CountryDialOption("AU", "+61", "🇦🇺 +61 (Australia)", 9, 9)
  )

  fun sanitizeNationalInput(rawInput: String, countryCode: String): String {
    val trimmed = rawInput.trim()
    val digitsOnly = trimmed.filter { it.isDigit() }
    val codeDigits = countryCode.filter { it.isDigit() }
    val option = SUPPORTED_COUNTRIES.firstOrNull { it.dialCode == countryCode }
    val maxLen = option?.maxNationalDigits ?: 12

    val withoutCountryCode = when {
      trimmed.startsWith("+") && codeDigits.isNotEmpty() && digitsOnly.startsWith(codeDigits) && digitsOnly.length > codeDigits.length -> {
        digitsOnly.drop(codeDigits.length)
      }
      codeDigits.isNotEmpty() && digitsOnly.startsWith(codeDigits) && digitsOnly.length > maxLen -> {
        digitsOnly.drop(codeDigits.length)
      }
      digitsOnly.startsWith("0") && digitsOnly.length > maxLen -> {
        digitsOnly.dropWhile { it == '0' }
      }
      else -> digitsOnly
    }
    return withoutCountryCode.take(maxLen)
  }

  fun toE164OrNull(rawInput: String, defaultCountryCode: String = "+91"): String? {
    val trimmed = rawInput.trim()
    if (trimmed.startsWith("+")) {
      val allDigits = trimmed.filter { it.isDigit() }
      return if (allDigits.length in 8..15) "+$allDigits" else null
    }

    val cleanNational = sanitizeNationalInput(trimmed, defaultCountryCode)
    val option = SUPPORTED_COUNTRIES.firstOrNull { it.dialCode == defaultCountryCode }
    val minDigits = option?.minNationalDigits ?: 7
    val maxDigits = option?.maxNationalDigits ?: 12

    val codeDigits = defaultCountryCode.filter { it.isDigit() }
    if (codeDigits.isEmpty() || cleanNational.length !in minDigits..maxDigits) {
      return null
    }
    return "+$codeDigits$cleanNational"
  }
}
