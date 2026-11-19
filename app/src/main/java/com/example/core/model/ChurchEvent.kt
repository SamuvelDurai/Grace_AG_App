package com.example.core.model

data class ChurchEvent(
  val eventId: String = "",
  val churchId: String = "church_main",
  val titleEnglish: String = "",
  val titleTamil: String = "",
  val descriptionEnglish: String = "",
  val descriptionTamil: String = "",
  val eventDate: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val venue: String = "",
  val isRsvpRequired: Boolean = true,
  val capacity: Int = 150,
  val registeredCount: Int = 0,
  val attendeeIds: List<String> = emptyList(),
  val isRegisteredByCurrentUser: Boolean = false,
  val organizer: String = "Church Youth & Fellowship Ministry",
  val category: EventCategory = EventCategory.SPECIAL_FESTIVAL
)

enum class EventCategory {
  SUNDAY_SERVICE,
  FASTING_PRAYER,
  YOUTH_EVENT,
  OUTREACH_MISSION,
  SPECIAL_FESTIVAL,
  OTHER_EVENT;

  fun displayLabel(isTamil: Boolean): String = when (this) {
    SUNDAY_SERVICE -> if (isTamil) "ஞாயிறு ஆராதனை" else "Sunday Service"
    FASTING_PRAYER -> if (isTamil) "உபவாச ஜெபம்" else "Fasting Prayer"
    YOUTH_EVENT -> if (isTamil) "வாலிபர் நிகழ்வு" else "Youth Event"
    OUTREACH_MISSION -> if (isTamil) "சுவிசேஷ ஊழியம்" else "Outreach"
    SPECIAL_FESTIVAL -> if (isTamil) "சிறப்பு பெருவிழா" else "Special Event"
    OTHER_EVENT -> if (isTamil) "மற்ற நிகழ்வு" else "Other Event"
  }
}
