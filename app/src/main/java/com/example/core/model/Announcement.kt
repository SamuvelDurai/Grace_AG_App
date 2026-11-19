package com.example.core.model

enum class AnnouncementCategory {
  SUNDAY_SERVICE,
  FASTING_PRAYER,
  YOUTH_MEETING,
  WOMENS_FELLOWSHIP,
  MENS_FELLOWSHIP,
  CHILDRENS_MINISTRY,
  SPECIAL_EVENT,
  GENERAL_NOTICE,
  EMERGENCY_NOTICE
}

data class Announcement(
  val announcementId: String = "",
  val churchId: String = "church_main",
  val titleEnglish: String = "",
  val titleTamil: String = "",
  val bodyEnglish: String = "",
  val bodyTamil: String = "",
  val category: AnnouncementCategory = AnnouncementCategory.GENERAL_NOTICE,
  val publishDate: String = "",
  val expiryDate: String = "",
  val isEmergency: Boolean = false,
  val authorName: String = "Pastor Office",
  val targetGroup: String = "ALL" // ALL, YOUTH, WOMEN, MEN
)
