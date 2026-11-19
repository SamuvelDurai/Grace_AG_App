package com.example.core.model

data class FellowshipGroup(
  val groupId: String = "",
  val churchId: String = "church_main",
  val nameEnglish: String = "",
  val nameTamil: String = "",
  val areaZone: String = "",
  val leaderName: String = "",
  val leaderPhone: String = "",
  val meetingSchedule: String = "",
  val meetingLocation: String = "",
  val memberCount: Int = 0,
  val memberIds: List<String> = emptyList(),
  val currentStudyTopicEnglish: String = "",
  val currentStudyTopicTamil: String = "",
  val category: FellowshipCategory = FellowshipCategory.AREA_CARE_CELL
)

enum class FellowshipCategory {
  AREA_CARE_CELL,
  YOUTH_MINISTRY,
  SUNDAY_SCHOOL,
  WOMENS_FELLOWSHIP,
  MENS_FELLOWSHIP,
  CHOIR_WORSHIP,
  OTHER_GROUP;

  fun displayLabel(isTamil: Boolean): String = when (this) {
    AREA_CARE_CELL -> if (isTamil) "பகுதி ஐக்கியம்" else "Care Cell"
    YOUTH_MINISTRY -> if (isTamil) "வாலிபர் ஐக்கியம்" else "Youth Group"
    SUNDAY_SCHOOL -> if (isTamil) "ஓய்வுநாள் பள்ளி" else "Sunday School"
    WOMENS_FELLOWSHIP -> if (isTamil) "மகளிர் ஊழியம்" else "Women's Wing"
    MENS_FELLOWSHIP -> if (isTamil) "ஆண்கள் ஐக்கியம்" else "Men's Fellowship"
    CHOIR_WORSHIP -> if (isTamil) "பாடகர் & ஆராதனை" else "Choir & Worship"
    OTHER_GROUP -> if (isTamil) "மற்ற குழு" else "Other Group"
  }
}

data class MinistryEvent(
  val eventId: String = "",
  val groupId: String = "",
  val titleEnglish: String = "",
  val titleTamil: String = "",
  val dateSchedule: String = "",
  val venue: String = "",
  val coordinator: String = ""
)
