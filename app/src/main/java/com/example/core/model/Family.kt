package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class Family(
  val familyId: String = "",
  val churchId: String = "church_main",
  val familyName: String = "",
  val headUid: String = "",
  val headMemberId: String = "",
  val headName: String = "",
  val headPhone: String = "",
  val address: String = "",
  val dependentMembers: List<FamilyMemberEntry> = emptyList(),
  val linkedMemberUids: List<String> = emptyList(),
  val sharedSubscriptionStatus: SubscriptionStatus = SubscriptionStatus.PENDING,
  val lastSubscriptionId: String = "",
  val lastPaidMonthYear: String = "",
  val lastPaidDate: String = "",
  val lastPaidByUid: String = "",
  val lastPaidByName: String = "",
  val monthlyDuesAmount: Double = 500.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun totalHouseholdCount(): Int {
    val nonHeadDependents = dependentMembers.count {
      it.linkedUid.isBlank() || it.linkedUid != headUid
    }
    val distinctLinkedExtras = linkedMemberUids
      .filter { it.isNotBlank() && it != headUid }
      .distinct()
      .size
    return maxOf(1 + nonHeadDependents, 1 + distinctLinkedExtras)
  }

  fun resolvedFamilyName(): String = familyName.ifBlank {
    if (headName.isNotBlank()) "${headName.trim()} Family" else "Church Family"
  }
}
