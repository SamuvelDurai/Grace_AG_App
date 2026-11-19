package com.example.core.model

data class Sermon(
  val sermonId: String = "",
  val churchId: String = "church_main",
  val titleEnglish: String = "",
  val titleTamil: String = "",
  val speakerName: String = "",
  val scriptureReference: String = "",
  val dateDelivered: String = "",
  val audioUrl: String = "",
  val videoUrl: String = "",
  val notesSummary: String = "",
  val durationMinutes: Int = 45,
  val category: String = "Sunday Worship"
)
