package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ChurchConfig(
  val churchId: String = "church_main",
  val name: String = "GRACE AG",
  val address: String = "12, Cross Street, Chennai, Tamil Nadu",
  val phone: String = "+91 44 2456 7890",
  val email: String = "office@gracechurch.org",
  val defaultLanguage: String = "en",
  val serviceTimings: String = "Sunday 09:00 AM (Tamil), 11:00 AM (English)",
  val monthlyDuesAmount: Double = 500.0,
  val upiId: String = "",
  val bankAccountDetails: String = "",
  val latestVersionCode: Int = 1,
  val latestVersionName: String = "1.0",
  val updateDownloadUrl: String = "",
  val updateReleaseNotes: String = "",
  val forceUpdate: Boolean = false
) {
  fun hasOnlinePaymentConfigured(): Boolean = upiId.isNotBlank() || bankAccountDetails.isNotBlank()

  fun hasUpdateAvailable(currentVersionCode: Int): Boolean {
    val cleanUrl = updateDownloadUrl.trim()
    val hasValidUrl = cleanUrl.startsWith("https://", ignoreCase = true) ||
      cleanUrl.startsWith("http://", ignoreCase = true)
    return latestVersionCode > currentVersionCode && hasValidUrl
  }
}
