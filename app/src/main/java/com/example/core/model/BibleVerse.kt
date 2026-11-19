package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class BibleVerse(
  val verseId: String = "",
  val churchId: String = "church_main",
  val bookReference: String = "",
  val textEnglish: String = "",
  val textTamil: String = "",
  val devotionalEnglish: String = "",
  val devotionalTamil: String = "",
  val translationSource: String = "Public Domain (BBE & Tamil BSI Public)",
  val scheduledDate: String = "",
  val pinDateKey: String = "",
  val isFavorite: Boolean = false,
  val publishedByPastor: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)
