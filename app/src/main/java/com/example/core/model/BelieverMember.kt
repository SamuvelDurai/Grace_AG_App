package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FamilyMemberEntry(
  val id: String = "",
  val name: String = "",
  val relationship: String = "", // Head, Spouse, Father, Mother, Son, Daughter, Brother, Sister, Sibling, Other
  val dateOfBirth: String = "",
  val gender: String = "",
  val baptismStatus: String = "NOT_BAPTIZED",
  val phoneNumber: String = "",
  val linkedUid: String = "",
  val membershipId: String = "",
  val profilePhotoUrl: String = ""
)

@Keep
@IgnoreExtraProperties
data class BelieverMember(
  val memberId: String = "",
  val uid: String = "",
  val churchId: String = "church_main",
  val membershipId: String = "",
  val fullName: String = "",
  val profilePhotoUrl: String = "",
  val phoneNumber: String = "",
  val email: String = "",
  val dateOfBirth: String = "",
  val gender: String = "",
  val address: String = "",
  val fatherName: String = "",
  val motherName: String = "",
  val spouseName: String = "",
  val familyId: String = "",
  val familyName: String = "",
  val familyRole: String = "FAMILY_HEAD", // FAMILY_HEAD, FAMILY_MEMBER
  val familyMembers: List<FamilyMemberEntry> = emptyList(),
  val churchJoiningDate: String = "",
  val membershipStatus: String = "ACTIVE",
  val baptismStatus: String = "BAPTIZED", // BAPTIZED, NOT_BAPTIZED, PENDING
  val baptismDate: String = "",
  val baptismPlace: String = "",
  val baptizedBy: String = "",
  val ministryGroup: String = "",
  val verificationToken: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun effectiveFamilyId(): String = familyId.ifBlank {
    when {
      uid.isNotBlank() -> "fam_$uid"
      memberId.isNotBlank() -> "fam_$memberId"
      else -> ""
    }
  }

  fun effectiveFamilyName(): String = familyName.ifBlank {
    if (fullName.isNotBlank()) "${fullName.trim()} Family" else "Church Family"
  }

  fun totalFamilyMembersCount(): Int = 1 + familyMembers.size
}
