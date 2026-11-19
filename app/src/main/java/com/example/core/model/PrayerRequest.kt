package com.example.core.model

enum class PrayerPrivacy {
  PASTOR_ONLY,
  PRAYER_INTERCESSORS,
  CHURCH_FAMILY
}

enum class PrayerStatus {
  NEW,
  IN_REVIEW,
  PRAYED_FOR,
  CLOSED
}

data class PrayerRequest(
  val requestId: String = "",
  val churchId: String = "church_main",
  val uid: String = "",
  val requesterName: String = "",
  val isAnonymous: Boolean = false,
  val title: String = "",
  val description: String = "",
  val privacy: PrayerPrivacy = PrayerPrivacy.PASTOR_ONLY,
  val status: PrayerStatus = PrayerStatus.NEW,
  val dateSubmitted: String = "",
  val pastoralNote: String = "",
  val pastoralResponseDate: String = ""
)
