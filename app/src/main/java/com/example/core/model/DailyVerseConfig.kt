package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class DailyVerseConfig(
  val churchId: String = "church_main",
  val autoDailyEnabled: Boolean = true,
  val rotationHour: Int = 6,
  val updatedByName: String = "",
  val updatedByUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
)
