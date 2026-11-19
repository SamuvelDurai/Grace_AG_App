package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  val uid: String = "",
  val phoneNumber: String = "",
  val email: String = "",
  val displayName: String = "",
  val role: String = UserRole.BELIEVER.name,
  val churchId: String = "church_main",
  val accountStatus: String = AccountStatus.PENDING.name,
  val membershipId: String = "",
  val familyId: String = "",
  val photoUrl: String = "",
  val fcmToken: String = "",
  val profileCompleted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val userRole: UserRole
    get() = UserRole.fromString(role)

  val status: AccountStatus
    get() = AccountStatus.fromString(accountStatus)

  fun effectiveFamilyId(): String = familyId.ifBlank {
    if (uid.isNotBlank()) "fam_$uid" else ""
  }
}
