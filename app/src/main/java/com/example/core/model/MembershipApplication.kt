package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class MembershipApplication(
  val applicationId: String = "",
  val uid: String = "",
  val churchId: String = "church_main",
  val fullName: String = "",
  val phoneNumber: String = "",
  val email: String = "",
  val dateOfBirth: String = "",
  val gender: String = "",
  val address: String = "",
  val baptismStatus: String = "NOT_BAPTIZED",
  val familyId: String = "",
  val familyName: String = "",
  val familyMembersCount: Int = 1,
  val familyMembers: List<FamilyMemberEntry> = emptyList(),
  val status: String = "PENDING", // PENDING, APPROVED, REJECTED
  val reviewNote: String = "",
  val reviewedBy: String = "",
  @ServerTimestamp val submittedAt: Timestamp? = null,
  @ServerTimestamp val reviewedAt: Timestamp? = null
)
