package com.example.data.user

import android.content.Context
import com.example.core.model.AccountStatus
import com.example.core.model.UserProfile
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  fun observeUserProfile(uid: String): Flow<UserProfile?> {
    val docRef = db.collection("users").document(uid)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java)?.let {
            if (it.uid.isBlank()) it.copy(uid = snapshot.id) else it
          }
        } else {
          null
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.GET, docRef.path)
        emit(null)
      }
  }

  suspend fun getUserProfile(uid: String): UserProfile? {
    if (auth.currentUser == null) return null
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(UserProfile::class.java)?.let {
          if (it.uid.isBlank()) it.copy(uid = snapshot.id) else it
        }
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  suspend fun isChurchAdminClaimed(churchId: String = "church_main"): Boolean {
    if (auth.currentUser == null) return true
    val docRef = db.collection("churches").document(churchId)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        false
      } else {
        !snapshot.getString("adminUid").isNullOrBlank()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      false
    }
  }

  suspend fun hasMembershipApplication(uid: String): Boolean {
    if (auth.currentUser == null) return false
    return try {
      val snapshot = db.collection("membershipApplications")
        .whereEqualTo("uid", uid)
        .limit(1)
        .get()
        .await()
      !snapshot.isEmpty
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "membershipApplications?uid=$uid")
      false
    }
  }

  suspend fun allocatePastorId(churchId: String = "church_main"): String =
    com.example.core.util.ChurchIdGenerator.nextPastorId(db, churchId)

  suspend fun findMatchingPastorInvite(email: String?, phoneNumber: String?): Map<String, Any?>? {
    if (auth.currentUser == null) return null
    val emailLower = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (emailLower != null) {
      val emailDocRef = db.collection("pastorInvites").document("email_$emailLower")
      try {
        val snap = emailDocRef.get().await()
        if (snap.exists()) return snap.data
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // Ignore if not matching or missing
      }
    }

    val normalizedRawPhone = phoneNumber?.trim()?.takeIf { it.isNotBlank() }
    val cleanDigits = normalizedRawPhone?.filter { it.isDigit() } ?: ""
    val phoneCandidates = buildSet {
      if (normalizedRawPhone != null) add(normalizedRawPhone)
      if (cleanDigits.length == 10) add("+91$cleanDigits")
      if (cleanDigits.length > 10 && !normalizedRawPhone.orEmpty().startsWith("+")) {
        add("+$cleanDigits")
        add("+91${cleanDigits.takeLast(10)}")
      }
    }
    for (candidate in phoneCandidates) {
      val phoneDocRef = db.collection("pastorInvites").document("phone_$candidate")
      try {
        val snap = phoneDocRef.get().await()
        if (snap.exists()) return snap.data
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // Ignore if not matching or missing
      }
    }
    return null
  }

  suspend fun createUserProfile(profile: UserProfile): Result<Unit> = runCatching {
    val currentUid = auth.currentUser?.uid ?: return@runCatching
    val uid = profile.uid.ifBlank { currentUid }
    val docRef = db.collection("users").document(uid)
    val resolvedFamilyId = profile.familyId.ifBlank {
      if (profile.userRole == com.example.core.model.UserRole.BELIEVER) "fam_$uid" else ""
    }

    val payload = hashMapOf<String, Any?>(
      "uid" to uid,
      "phoneNumber" to profile.phoneNumber,
      "email" to profile.email,
      "displayName" to profile.displayName,
      "role" to profile.role,
      "churchId" to profile.churchId,
      "accountStatus" to profile.accountStatus,
      "membershipId" to profile.membershipId,
      "familyId" to resolvedFamilyId,
      "photoUrl" to profile.photoUrl,
      "profileCompleted" to profile.profileCompleted,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfile(profile: UserProfile): Result<Unit> = runCatching {
    val currentUid = auth.currentUser?.uid ?: return@runCatching
    val uid = profile.uid.ifBlank { currentUid }
    val docRef = db.collection("users").document(uid)

    val payload = hashMapOf<String, Any?>(
      "displayName" to profile.displayName,
      "phoneNumber" to profile.phoneNumber,
      "email" to profile.email,
      "role" to profile.role,
      "churchId" to profile.churchId,
      "accountStatus" to profile.accountStatus,
      "membershipId" to profile.membershipId,
      "profileCompleted" to profile.profileCompleted,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (profile.familyId.isNotBlank()) {
      payload["familyId"] = profile.familyId
    }
    if (profile.photoUrl.isNotBlank()) {
      payload["photoUrl"] = profile.photoUrl
    }

    try {
      docRef.set(payload, SetOptions.merge()).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun submitMembershipApplication(
    fullName: String,
    phoneNumber: String,
    email: String,
    address: String,
    dateOfBirth: String = "",
    gender: String = "",
    baptismStatus: String = "NOT_BAPTIZED",
    churchId: String = "church_main",
    familyId: String = "",
    familyName: String = "",
    familyMembers: List<com.example.core.model.FamilyMemberEntry> = emptyList(),
    profilePhotoUrl: String = ""
  ): Result<String> = runCatching {
    val uid = auth.currentUser?.uid ?: return@runCatching ""
    val docRef = db.collection("membershipApplications").document(uid)
    val resolvedFamilyId = familyId.ifBlank { "fam_$uid" }
    val resolvedFamilyName = familyName.trim().ifBlank { "${fullName.trim()} Family" }
    val serializedMembers = familyMembers.map { entry ->
      mapOf(
        "id" to entry.id,
        "name" to entry.name,
        "relationship" to entry.relationship,
        "dateOfBirth" to entry.dateOfBirth,
        "gender" to entry.gender,
        "baptismStatus" to entry.baptismStatus,
        "phoneNumber" to entry.phoneNumber
      )
    }
    val payload = hashMapOf<String, Any?>(
      "applicationId" to uid,
      "uid" to uid,
      "churchId" to churchId,
      "fullName" to fullName.trim(),
      "phoneNumber" to phoneNumber.trim(),
      "email" to email.trim(),
      "dateOfBirth" to dateOfBirth.trim(),
      "gender" to gender.trim(),
      "address" to address.trim(),
      "baptismStatus" to baptismStatus.ifBlank { "NOT_BAPTIZED" },
      "familyId" to resolvedFamilyId,
      "familyName" to resolvedFamilyName,
      "familyMembersCount" to (1 + familyMembers.size),
      "familyMembers" to serializedMembers,
      "status" to AccountStatus.PENDING.name,
      "submittedAt" to FieldValue.serverTimestamp()
    )
    if (profilePhotoUrl.isNotBlank()) {
      payload["profilePhotoUrl"] = profilePhotoUrl
    }

    try {
      docRef.set(payload, SetOptions.merge()).await()
      uid
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun seedDefaultChurchInfo(
    adminUid: String = auth.currentUser?.uid ?: "",
    adminEmail: String = auth.currentUser?.email ?: ""
  ): Result<Unit> = runCatching {
    if (auth.currentUser == null) return Result.success(Unit)
    val docRef = db.collection("churches").document("church_main")
    val payload = hashMapOf<String, Any?>(
      "churchId" to "church_main",
      "name" to "GRACE AG",
      "address" to "123 Faith Avenue, Chennai, Tamil Nadu",
      "phone" to "+91 44 2456 7890",
      "email" to adminEmail.ifBlank { "office@gracechurch.org" },
      "defaultLanguage" to "en",
      "serviceTimings" to "Sunday 09:00 AM (Tamil), 11:00 AM (English)",
      "monthlyDuesAmount" to 500.0,
      "adminUid" to adminUid,
      "adminEmail" to adminEmail,
      "createdAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload, SetOptions.merge()).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }
}
