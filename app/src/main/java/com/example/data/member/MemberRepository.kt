package com.example.data.member

import android.content.Context
import com.example.core.model.AccountStatus
import com.example.core.model.BelieverMember
import com.example.core.model.MembershipApplication
import com.example.core.util.ChurchIdGenerator
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.example.data.family.FamilyRepository
import com.example.data.pastoralcare.CelebrationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MemberRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
  val familyRepository: FamilyRepository = FamilyRepository(db)
  val celebrationRepository: CelebrationRepository = CelebrationRepository(db)

  // Local fallback used strictly when FirebaseAuth has no signed-in user (unit tests)
  private val inMemoryMembers = mutableListOf(
    BelieverMember(
      memberId = "mem_1",
      uid = "local_member_1",
      membershipId = "GAG-2026-0001",
      fullName = "Samuel Durai",
      phoneNumber = "9876543210",
      email = "samuel@example.com",
      dateOfBirth = "15/05/1990",
      gender = "Male",
      address = "42, Cross Street, Chennai",
      churchJoiningDate = "10/01/2023",
      membershipStatus = "ACTIVE",
      baptismStatus = "BAPTIZED",
      baptismDate = "12/04/2015",
      baptismPlace = "GRACE AG, Chennai",
      baptizedBy = "Rev. Dr. Thomas Paul",
      ministryGroup = "Worship & Choir",
      verificationToken = "GAG-VERIFY-GAG-2026-0001"
    ),
    BelieverMember(
      memberId = "mem_2",
      uid = "local_member_2",
      membershipId = "GAG-2026-0002",
      fullName = "Grace Priya",
      phoneNumber = "9876543211",
      email = "grace@example.com",
      dateOfBirth = "22/08/1994",
      gender = "Female",
      address = "18, Church Road, Chennai",
      churchJoiningDate = "05/03/2023",
      membershipStatus = "ACTIVE",
      baptismStatus = "BAPTIZED",
      baptismDate = "14/06/2018",
      baptismPlace = "Bethel Chapel",
      baptizedBy = "Pastor Stephen",
      ministryGroup = "Sunday School",
      verificationToken = "GAG-VERIFY-GAG-2026-0002"
    )
  )

  private val inMemoryApplications = mutableListOf<MembershipApplication>()
  private val _membersFlow = MutableStateFlow<List<BelieverMember>>(inMemoryMembers.toList())
  private val _pendingAppsFlow = MutableStateFlow<List<MembershipApplication>>(emptyList())

  fun observeMemberByUid(uid: String): Flow<BelieverMember?> {
    if (auth.currentUser == null) {
      return _membersFlow.map { list ->
        list.find { it.uid == uid } ?: list.firstOrNull()
      }
    }

    val query = db.collection("believers")
      .whereEqualTo("uid", uid)
      .limit(1)

    return query.snapshots()
      .map { snapshot ->
        snapshot.documents.firstOrNull()?.let { doc ->
          doc.toObject(BelieverMember::class.java)?.let {
            if (it.memberId.isBlank()) it.copy(memberId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.GET, "believers?uid=$uid")
        emit(null)
      }
  }

  suspend fun getMemberByUid(uid: String, churchId: String? = null): BelieverMember? {
    if (auth.currentUser == null) {
      return inMemoryMembers.find { it.uid == uid } ?: inMemoryMembers.firstOrNull()
    }
    return try {
      val directDoc = db.collection("believers").document("mem_$uid").get().await()
      if (directDoc.exists()) {
        return directDoc.toObject(BelieverMember::class.java)?.let {
          if (it.memberId.isBlank()) it.copy(memberId = directDoc.id) else it
        }
      }
      var query = db.collection("believers").whereEqualTo("uid", uid)
      if (!churchId.isNullOrBlank()) {
        query = query.whereEqualTo("churchId", churchId)
      }
      val snapshot = query.limit(1).get().await()
      snapshot.documents.firstOrNull()?.let { doc ->
        doc.toObject(BelieverMember::class.java)?.let {
          if (it.memberId.isBlank()) it.copy(memberId = doc.id) else it
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "believers?uid=$uid")
      null
    }
  }

  suspend fun saveMemberProfile(member: BelieverMember): Result<Unit> = runCatching {
    val resolvedFamilyId = member.effectiveFamilyId()
    val resolvedFamilyName = member.effectiveFamilyName()
    val normalizedMember = member.copy(
      familyId = resolvedFamilyId,
      familyName = resolvedFamilyName
    )

    val index = inMemoryMembers.indexOfFirst { it.uid == normalizedMember.uid || it.memberId == normalizedMember.memberId }
    if (index >= 0) {
      inMemoryMembers[index] = normalizedMember
    } else {
      inMemoryMembers.add(normalizedMember)
    }
    _membersFlow.value = inMemoryMembers.toList()

    val currentUser = auth.currentUser
    if (currentUser == null) {
      familyRepository.syncFromBelieverMember(normalizedMember)
      return@runCatching
    }

    val docId = normalizedMember.memberId.ifBlank { "mem_${normalizedMember.uid}" }
    val docRef = db.collection("believers").document(docId)
    val serializedFamilyMembers = normalizedMember.familyMembers.map { entry ->
      mapOf(
        "id" to entry.id.ifBlank { UUID.randomUUID().toString().take(8) },
        "name" to entry.name.trim(),
        "relationship" to entry.relationship.trim(),
        "dateOfBirth" to entry.dateOfBirth.trim(),
        "gender" to entry.gender.trim(),
        "baptismStatus" to entry.baptismStatus.ifBlank { "NOT_BAPTIZED" },
        "phoneNumber" to entry.phoneNumber.trim(),
        "linkedUid" to entry.linkedUid.trim(),
        "membershipId" to entry.membershipId.trim(),
        "profilePhotoUrl" to entry.profilePhotoUrl
      )
    }

    val targetUid = normalizedMember.uid.ifBlank { currentUser.uid }
    val payload = hashMapOf<String, Any?>(
      "memberId" to docId,
      "uid" to targetUid,
      "churchId" to normalizedMember.churchId.ifBlank { "church_main" },
      "membershipId" to normalizedMember.membershipId,
      "fullName" to normalizedMember.fullName,
      "profilePhotoUrl" to normalizedMember.profilePhotoUrl,
      "phoneNumber" to normalizedMember.phoneNumber,
      "email" to normalizedMember.email,
      "dateOfBirth" to normalizedMember.dateOfBirth,
      "gender" to normalizedMember.gender,
      "address" to normalizedMember.address,
      "fatherName" to normalizedMember.fatherName,
      "motherName" to normalizedMember.motherName,
      "spouseName" to normalizedMember.spouseName,
      "familyId" to resolvedFamilyId,
      "familyName" to resolvedFamilyName,
      "familyRole" to normalizedMember.familyRole.ifBlank { "FAMILY_HEAD" },
      "familyMembers" to serializedFamilyMembers,
      "churchJoiningDate" to normalizedMember.churchJoiningDate,
      "membershipStatus" to normalizedMember.membershipStatus,
      "baptismStatus" to normalizedMember.baptismStatus,
      "baptismDate" to normalizedMember.baptismDate,
      "baptismPlace" to normalizedMember.baptismPlace,
      "baptizedBy" to normalizedMember.baptizedBy,
      "ministryGroup" to normalizedMember.ministryGroup,
      "verificationToken" to normalizedMember.verificationToken.ifBlank {
        ChurchIdGenerator.verificationTokenFor(normalizedMember.membershipId)
      },
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      docRef.set(payload, SetOptions.merge()).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }

    // Sync profile photo & basic details to /users/{uid} so Pastor & Super Admin see it across devices
    if (targetUid.isNotBlank()) {
      runCatching {
        val userUpdates = hashMapOf<String, Any?>(
          "updatedAt" to FieldValue.serverTimestamp()
        )
        if (normalizedMember.profilePhotoUrl.isNotBlank()) {
          userUpdates["photoUrl"] = normalizedMember.profilePhotoUrl
        }
        if (normalizedMember.fullName.isNotBlank()) {
          userUpdates["displayName"] = normalizedMember.fullName.trim()
        }
        if (normalizedMember.phoneNumber.isNotBlank()) {
          userUpdates["phoneNumber"] = normalizedMember.phoneNumber.trim()
        }
        if (userUpdates.size > 1) {
          db.collection("users").document(targetUid).update(userUpdates).await()
        }
      }
    }

    // Sync exact lookup keys (Membership ID & 10-digit Phone) so Family Heads can connect relatives cleanly
    runCatching {
      syncMemberLookupKeys(normalizedMember.copy(memberId = docId, uid = targetUid))
    }

    runCatching {
      familyRepository.syncFromBelieverMember(normalizedMember.copy(memberId = docId, uid = targetUid)).getOrThrow()
    }

    runCatching {
      celebrationRepository.syncBirthdayFromBeliever(normalizedMember.copy(memberId = docId, uid = targetUid))
    }
  }

  suspend fun ensureMemberLookup(member: BelieverMember) {
    if (auth.currentUser == null || member.uid.isBlank() || member.membershipId.isBlank()) return
    runCatching {
      syncMemberLookupKeys(member)
    }
  }

  private suspend fun syncMemberLookupKeys(member: BelieverMember) {
    val uid = member.uid.ifBlank { return }
    val churchId = member.churchId.ifBlank { "church_main" }
    val basePayload = hashMapOf<String, Any?>(
      "uid" to uid,
      "memberId" to member.memberId.ifBlank { "mem_$uid" },
      "membershipId" to member.membershipId.trim(),
      "churchId" to churchId,
      "fullName" to member.fullName.trim(),
      "phoneNumber" to member.phoneNumber.trim(),
      "baptismStatus" to member.baptismStatus.ifBlank { "NOT_BAPTIZED" },
      "gender" to member.gender.trim(),
      "dateOfBirth" to member.dateOfBirth.trim(),
      "profilePhotoUrl" to member.profilePhotoUrl,
      "familyId" to member.effectiveFamilyId(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    val cleanId = member.membershipId.trim().uppercase()
    if (cleanId.isNotBlank() && cleanId != "PENDING ID") {
      db.collection("memberLookup").document("id_$cleanId")
        .set(basePayload, SetOptions.merge())
        .await()
    }

    val digits = member.phoneNumber.filter { it.isDigit() }
    if (digits.length >= 10) {
      db.collection("memberLookup").document("phone_${digits.takeLast(10)}")
        .set(basePayload, SetOptions.merge())
        .await()
    }
  }

  suspend fun lookupBelieverByIdOrPhone(
    query: String,
    churchId: String = "church_main"
  ): Result<BelieverMember?> = runCatching {
    val clean = query.trim()
    require(clean.isNotBlank()) { "Enter a Membership ID or 10-digit Mobile Number." }
    val cleanDigits = clean.filter { it.isDigit() }

    if (auth.currentUser == null) {
      val localMatch = inMemoryMembers.firstOrNull { m ->
        val mDigits = m.phoneNumber.filter { it.isDigit() }
        m.membershipId.equals(clean, ignoreCase = true) ||
          (cleanDigits.length >= 10 && mDigits.endsWith(cleanDigits.takeLast(10))) ||
          m.fullName.equals(clean, ignoreCase = true)
      }
      return@runCatching localMatch
    }

    val candidateKeys = buildList {
      add("id_${clean.uppercase()}")
      if (cleanDigits.length >= 10) {
        add("phone_${cleanDigits.takeLast(10)}")
      }
    }

    for (key in candidateKeys) {
      val snap = try {
        db.collection("memberLookup").document(key).get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        null
      }
      if (snap != null && snap.exists()) {
        val docChurchId = snap.getString("churchId") ?: "church_main"
        if (docChurchId == churchId) {
          return@runCatching BelieverMember(
            memberId = snap.getString("memberId") ?: "mem_${snap.getString("uid").orEmpty()}",
            uid = snap.getString("uid") ?: "",
            churchId = docChurchId,
            membershipId = snap.getString("membershipId") ?: "",
            fullName = snap.getString("fullName") ?: "Church Member",
            profilePhotoUrl = snap.getString("profilePhotoUrl") ?: "",
            phoneNumber = snap.getString("phoneNumber") ?: "",
            dateOfBirth = snap.getString("dateOfBirth") ?: "",
            gender = snap.getString("gender") ?: "",
            familyId = snap.getString("familyId") ?: "",
            baptismStatus = snap.getString("baptismStatus") ?: "NOT_BAPTIZED"
          )
        }
      }
    }

    // Fallback for Pastors/Admins querying believers collection directly
    val directMatches = runCatching {
      db.collection("believers")
        .whereEqualTo("churchId", churchId)
        .limit(250)
        .get()
        .await()
        .documents
        .mapNotNull { doc ->
          doc.toObject(BelieverMember::class.java)?.let {
            if (it.memberId.isBlank()) it.copy(memberId = doc.id) else it
          }
        }
    }.getOrDefault(emptyList())

    directMatches.firstOrNull { m ->
      val mDigits = m.phoneNumber.filter { it.isDigit() }
      m.membershipId.equals(clean, ignoreCase = true) ||
        (cleanDigits.length >= 10 && mDigits.endsWith(cleanDigits.takeLast(10)))
    }
  }

  suspend fun adoptLinkedFamilyForMember(
    member: BelieverMember,
    targetFamilyId: String,
    targetFamilyName: String,
    isHead: Boolean
  ): Result<Unit> = runCatching {
    val cleanFamilyId = targetFamilyId.trim()
    if (cleanFamilyId.isBlank()) return@runCatching
    val targetRole = if (isHead) "FAMILY_HEAD" else "FAMILY_MEMBER"
    val updatedLocal = member.copy(
      familyId = cleanFamilyId,
      familyName = targetFamilyName.ifBlank { member.familyName },
      familyRole = targetRole
    )
    val idx = inMemoryMembers.indexOfFirst { it.uid == member.uid || it.memberId == member.memberId }
    if (idx >= 0) {
      inMemoryMembers[idx] = updatedLocal
      _membersFlow.value = inMemoryMembers.toList()
    }

    val currentUser = auth.currentUser ?: return@runCatching
    val uid = member.uid.ifBlank { currentUser.uid }
    val docId = member.memberId.ifBlank { "mem_$uid" }
    db.collection("users").document(uid).update(
      mapOf(
        "familyId" to cleanFamilyId,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    db.collection("believers").document(docId).set(
      mapOf(
        "familyId" to cleanFamilyId,
        "familyName" to targetFamilyName.ifBlank { member.effectiveFamilyName() },
        "familyRole" to targetRole,
        "updatedAt" to FieldValue.serverTimestamp()
      ),
      SetOptions.merge()
    ).await()
  }

  suspend fun updateMemberFamilyAssignment(
    member: BelieverMember,
    newFamilyId: String,
    newFamilyName: String,
    familyRole: String = member.familyRole.ifBlank { "FAMILY_HEAD" }
  ): Result<BelieverMember> = runCatching {
    val cleanFamilyId = newFamilyId.trim().ifBlank { "fam_${member.uid}" }
    val cleanFamilyName = newFamilyName.trim().ifBlank { "${member.fullName} Family" }
    val updated = member.copy(
      familyId = cleanFamilyId,
      familyName = cleanFamilyName,
      familyRole = familyRole
    )
    saveMemberProfile(updated).getOrThrow()

    if (auth.currentUser != null && member.uid.isNotBlank()) {
      db.collection("users").document(member.uid).update(
        mapOf(
          "familyId" to cleanFamilyId,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      runCatching {
        if (familyRole == "FAMILY_HEAD") {
          familyRepository.assignHouseholdHead(
            familyId = cleanFamilyId,
            headUid = updated.uid,
            headMemberId = updated.memberId,
            headName = updated.fullName,
            headPhone = updated.phoneNumber
          )
        } else {
          familyRepository.linkAccountUidToFamily(cleanFamilyId, updated.uid)
        }
      }
    }
    updated
  }

  fun observeAllMembers(churchId: String = "church_main"): Flow<List<BelieverMember>> {
    if (auth.currentUser == null) {
      return _membersFlow
    }

    return db.collection("believers")
      .whereEqualTo("churchId", churchId)
      .limit(250)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(BelieverMember::class.java)?.let {
            if (it.memberId.isBlank()) it.copy(memberId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "believers")
        emit(emptyList())
      }
  }

  fun observePendingApplications(churchId: String = "church_main"): Flow<List<MembershipApplication>> {
    if (auth.currentUser == null) {
      return _pendingAppsFlow
    }

    return db.collection("membershipApplications")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(MembershipApplication::class.java)?.let { app ->
            if (app.applicationId.isBlank()) app.copy(applicationId = doc.id) else app
          }
        }.filter { it.status == "PENDING" }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "membershipApplications")
        emit(emptyList())
      }
  }

  suspend fun allocateMembershipId(churchId: String = "church_main"): String =
    ChurchIdGenerator.nextMembershipId(db, churchId)

  suspend fun approveApplication(
    application: MembershipApplication,
    reviewerUid: String
  ): Result<BelieverMember> = runCatching {
    val resolvedChurchId = application.churchId.ifBlank { "church_main" }
    val existing = if (auth.currentUser != null) {
      getMemberByUid(application.uid, resolvedChurchId)
    } else {
      inMemoryMembers.find { it.uid == application.uid }
    }
    val newMembershipId = existing?.membershipId?.ifBlank { null }
      ?: ChurchIdGenerator.nextMembershipId(db, resolvedChurchId)
    val token = existing?.verificationToken?.ifBlank { null }
      ?: ChurchIdGenerator.verificationTokenFor(newMembershipId)
    val resolvedFamilyId = existing?.familyId?.ifBlank { null }
      ?: application.familyId.ifBlank { "fam_${application.uid}" }
    val resolvedFamilyName = existing?.familyName?.ifBlank { null }
      ?: application.familyName.ifBlank { "${application.fullName.ifBlank { existing?.fullName ?: "Member" }} Family" }
    val resolvedFamilyMembers = if (existing?.familyMembers?.isNotEmpty() == true) {
      existing.familyMembers
    } else {
      application.familyMembers
    }

    val newMember = BelieverMember(
      memberId = existing?.memberId?.ifBlank { null } ?: "mem_${application.uid}",
      uid = application.uid,
      churchId = application.churchId.ifBlank { existing?.churchId ?: "church_main" },
      membershipId = newMembershipId,
      fullName = application.fullName.ifBlank { existing?.fullName ?: "" },
      profilePhotoUrl = existing?.profilePhotoUrl ?: "",
      phoneNumber = application.phoneNumber.ifBlank { existing?.phoneNumber ?: "" },
      email = application.email.ifBlank { existing?.email ?: "" },
      dateOfBirth = application.dateOfBirth.ifBlank { existing?.dateOfBirth ?: "" },
      gender = application.gender.ifBlank { existing?.gender ?: "" },
      address = application.address.ifBlank { existing?.address ?: "" },
      fatherName = existing?.fatherName ?: "",
      motherName = existing?.motherName ?: "",
      spouseName = existing?.spouseName ?: "",
      familyId = resolvedFamilyId,
      familyName = resolvedFamilyName,
      familyRole = existing?.familyRole?.ifBlank { null } ?: "FAMILY_HEAD",
      familyMembers = resolvedFamilyMembers,
      churchJoiningDate = existing?.churchJoiningDate?.ifBlank { null } ?: "Today",
      membershipStatus = "ACTIVE",
      baptismStatus = existing?.baptismStatus?.ifBlank { null } ?: application.baptismStatus,
      baptismDate = existing?.baptismDate ?: "",
      baptismPlace = existing?.baptismPlace ?: "",
      baptizedBy = existing?.baptizedBy ?: "",
      ministryGroup = existing?.ministryGroup ?: "",
      verificationToken = token
    )

    val appIndex = inMemoryApplications.indexOfFirst { it.applicationId == application.applicationId }
    if (appIndex >= 0) {
      inMemoryApplications[appIndex] = application.copy(
        status = "APPROVED",
        reviewedBy = reviewerUid
      )
    }
    inMemoryMembers.removeAll { it.uid == newMember.uid }
    inMemoryMembers.add(newMember)
    _membersFlow.value = inMemoryMembers.toList()
    _pendingAppsFlow.value = inMemoryApplications.filter { it.status == "PENDING" }

    auth.currentUser ?: return@runCatching newMember

    val appRef = db.collection("membershipApplications").document(application.applicationId)
    appRef.update(
      mapOf(
        "status" to "APPROVED",
        "reviewedBy" to reviewerUid,
        "reviewedAt" to FieldValue.serverTimestamp()
      )
    ).await()

    saveMemberProfile(newMember).getOrThrow()

    db.collection("users").document(application.uid).update(
      mapOf(
        "accountStatus" to AccountStatus.APPROVED.name,
        "membershipId" to newMembershipId,
        "familyId" to resolvedFamilyId,
        "profileCompleted" to true,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()

    newMember
  }

  suspend fun rejectApplication(
    applicationId: String,
    reviewerUid: String,
    reason: String
  ): Result<Unit> = runCatching {
    val appIndex = inMemoryApplications.indexOfFirst { it.applicationId == applicationId }
    if (appIndex >= 0) {
      inMemoryApplications[appIndex] = inMemoryApplications[appIndex].copy(
        status = "REJECTED",
        reviewNote = reason,
        reviewedBy = reviewerUid
      )
    }
    _pendingAppsFlow.value = inMemoryApplications.filter { it.status == "PENDING" }

    auth.currentUser ?: return@runCatching
    db.collection("membershipApplications").document(applicationId).update(
      mapOf(
        "status" to "REJECTED",
        "reviewNote" to reason,
        "reviewedBy" to reviewerUid,
        "reviewedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }
}
