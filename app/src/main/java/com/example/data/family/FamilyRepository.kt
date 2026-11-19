package com.example.data.family

import android.content.Context
import com.example.core.model.BelieverMember
import com.example.core.model.Family
import com.example.core.model.FamilyMemberEntry
import com.example.core.model.MonthlySubscription
import com.example.core.model.SubscriptionStatus
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val inMemoryFamilies = mutableListOf(
    Family(
      familyId = "fam_local_member_1",
      churchId = "church_main",
      familyName = "Samuel Durai Family",
      headUid = "local_member_1",
      headMemberId = "mem_1",
      headName = "Samuel Durai",
      headPhone = "9876543210",
      address = "42, Cross Street, Chennai",
      dependentMembers = listOf(
        FamilyMemberEntry(
          id = "dep_1",
          name = "Esther Samuel",
          relationship = "Spouse",
          gender = "Female",
          baptismStatus = "BAPTIZED"
        ),
        FamilyMemberEntry(
          id = "dep_2",
          name = "Caleb Samuel",
          relationship = "Son",
          gender = "Male",
          baptismStatus = "NOT_BAPTIZED"
        )
      ),
      linkedMemberUids = listOf("local_member_1"),
      sharedSubscriptionStatus = SubscriptionStatus.PAID,
      lastSubscriptionId = "sub_fam_local_member_1_2026_11",
      lastPaidByUid = "local_member_1",
      lastPaidByName = "Samuel Durai",
      monthlyDuesAmount = 500.0
    )
  )

  private val _familiesFlow = MutableStateFlow<List<Family>>(inMemoryFamilies.toList())

  fun observeAllFamilies(churchId: String = "church_main"): Flow<List<Family>> {
    if (auth.currentUser == null) {
      return _familiesFlow.asStateFlow()
    }
    return db.collection("families")
      .whereEqualTo("churchId", churchId)
      .limit(250)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(Family::class.java)?.let {
            if (it.familyId.isBlank()) it.copy(familyId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "families")
        emit(emptyList())
      }
  }

  fun observeFamilyById(familyId: String): Flow<Family?> {
    val cleanId = familyId.trim()
    if (cleanId.isBlank()) {
      return MutableStateFlow(null)
    }
    if (auth.currentUser == null) {
      return _familiesFlow.map { list -> list.find { it.familyId == cleanId } }
    }

    val docRef = db.collection("families").document(cleanId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(Family::class.java)?.let {
            if (it.familyId.isBlank()) it.copy(familyId = snapshot.id) else it
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

  fun observeFamilyForMember(uid: String, fallbackFamilyId: String = ""): Flow<Family?> {
    val cleanUid = uid.trim()
    if (cleanUid.isBlank()) {
      return observeFamilyById(fallbackFamilyId)
    }
    if (auth.currentUser == null) {
      return _familiesFlow.map { list ->
        pickBestFamilyForMember(list, cleanUid, fallbackFamilyId)
      }
    }

    return db.collection("families")
      .whereArrayContains("linkedMemberUids", cleanUid)
      .limit(10)
      .snapshots()
      .map { snapshot ->
        val matches = snapshot.documents.mapNotNull { doc ->
          doc.toObject(Family::class.java)?.let {
            if (it.familyId.isBlank()) it.copy(familyId = doc.id) else it
          }
        }
        pickBestFamilyForMember(matches, cleanUid, fallbackFamilyId)
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "families?linkedMemberUids=$cleanUid")
        emit(null)
      }
  }

  private fun pickBestFamilyForMember(
    candidates: List<Family>,
    uid: String,
    fallbackFamilyId: String
  ): Family? {
    val matching = candidates.filter { fam ->
      fam.headUid == uid || fam.linkedMemberUids.contains(uid) || fam.familyId == fallbackFamilyId
    }
    if (matching.isEmpty()) return null
    return matching.firstOrNull { fam -> fam.headUid.isNotBlank() && fam.headUid != uid }
      ?: matching.firstOrNull { fam -> fam.totalHouseholdCount() > 1 || fam.dependentMembers.isNotEmpty() }
      ?: matching.firstOrNull { fam -> fam.familyId == fallbackFamilyId }
      ?: matching.first()
  }

  suspend fun getFamilyById(familyId: String): Family? {
    val cleanId = familyId.trim()
    if (cleanId.isBlank()) return null
    if (auth.currentUser == null) {
      return inMemoryFamilies.find { it.familyId == cleanId }
    }
    val docRef = db.collection("families").document(cleanId)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(Family::class.java)?.let {
          if (it.familyId.isBlank()) it.copy(familyId = snapshot.id) else it
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

  suspend fun upsertFamily(family: Family): Result<Family> = runCatching {
    val callerUid = auth.currentUser?.uid ?: ""
    val effectiveHeadUid = family.headUid.ifBlank { callerUid }
    val resolvedId = family.familyId.ifBlank {
      if (effectiveHeadUid.isNotBlank()) "fam_$effectiveHeadUid" else "fam_${UUID.randomUUID().toString().take(8)}"
    }
    val normalizedDependents = family.dependentMembers.map { entry ->
      entry.copy(
        id = entry.id.ifBlank { UUID.randomUUID().toString().take(8) },
        name = entry.name.trim(),
        relationship = entry.relationship.trim().ifBlank { "Family Member" },
        dateOfBirth = entry.dateOfBirth.trim(),
        gender = entry.gender.trim(),
        baptismStatus = entry.baptismStatus.ifBlank { "NOT_BAPTIZED" },
        phoneNumber = entry.phoneNumber.trim(),
        linkedUid = entry.linkedUid.trim(),
        membershipId = entry.membershipId.trim(),
        profilePhotoUrl = entry.profilePhotoUrl
      )
    }
    val linkedFromEntries = normalizedDependents.mapNotNull { it.linkedUid.takeIf { uid -> uid.isNotBlank() } }
    val normalizedLinked = (family.linkedMemberUids + effectiveHeadUid + linkedFromEntries)
      .filter { it.isNotBlank() }
      .distinct()

    val normalized = family.copy(
      familyId = resolvedId,
      churchId = family.churchId.ifBlank { "church_main" },
      familyName = family.familyName.trim().ifBlank {
        if (family.headName.isNotBlank()) "${family.headName.trim()} Family" else "Church Family"
      },
      headUid = effectiveHeadUid,
      dependentMembers = normalizedDependents,
      linkedMemberUids = normalizedLinked
    )

    val existingIdx = inMemoryFamilies.indexOfFirst { it.familyId == resolvedId }
    if (existingIdx >= 0) {
      inMemoryFamilies[existingIdx] = normalized
    } else {
      inMemoryFamilies.add(0, normalized)
    }
    _familiesFlow.value = inMemoryFamilies.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("families").document(resolvedId)
      val serializedDependents = normalized.dependentMembers.map { entry ->
        mapOf(
          "id" to entry.id,
          "name" to entry.name,
          "relationship" to entry.relationship,
          "dateOfBirth" to entry.dateOfBirth,
          "gender" to entry.gender,
          "baptismStatus" to entry.baptismStatus,
          "phoneNumber" to entry.phoneNumber,
          "linkedUid" to entry.linkedUid,
          "membershipId" to entry.membershipId,
          "profilePhotoUrl" to entry.profilePhotoUrl
        )
      }
      val payload = hashMapOf<String, Any?>(
        "familyId" to normalized.familyId,
        "churchId" to normalized.churchId,
        "familyName" to normalized.familyName,
        "headUid" to normalized.headUid,
        "headMemberId" to normalized.headMemberId,
        "headName" to normalized.headName,
        "headPhone" to normalized.headPhone,
        "address" to normalized.address,
        "dependentMembers" to serializedDependents,
        "linkedMemberUids" to normalized.linkedMemberUids,
        "sharedSubscriptionStatus" to normalized.sharedSubscriptionStatus.name,
        "lastSubscriptionId" to normalized.lastSubscriptionId,
        "lastPaidMonthYear" to normalized.lastPaidMonthYear,
        "lastPaidDate" to normalized.lastPaidDate,
        "lastPaidByUid" to normalized.lastPaidByUid,
        "lastPaidByName" to normalized.lastPaidByName,
        "monthlyDuesAmount" to normalized.monthlyDuesAmount,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (existingIdx < 0) {
        payload["createdAt"] = FieldValue.serverTimestamp()
      }
      try {
        docRef.set(payload, SetOptions.merge()).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        throw e
      }
    }

    normalized
  }

  suspend fun addDependentMember(
    familyId: String,
    member: FamilyMemberEntry
  ): Result<Family> = runCatching {
    require(member.name.isNotBlank()) { "Dependent member name is required" }
    val existing = getFamilyById(familyId) ?: Family(familyId = familyId)
    val entry = member.copy(
      id = member.id.ifBlank { UUID.randomUUID().toString().take(8) },
      name = member.name.trim(),
      relationship = member.relationship.trim().ifBlank { "Family Member" }
    )
    val updatedList = existing.dependentMembers.filterNot {
      it.id == entry.id || (entry.linkedUid.isNotBlank() && it.linkedUid == entry.linkedUid)
    } + entry
    upsertFamily(existing.copy(dependentMembers = updatedList)).getOrThrow()
  }

  suspend fun connectBelieverToFamily(
    familyId: String,
    targetMember: BelieverMember,
    relationshipRole: String,
    makeHead: Boolean = false,
    defaultHeadMember: BelieverMember? = null
  ): Result<Family> = runCatching {
    require(targetMember.uid.isNotBlank()) { "Selected believer account has no user ID." }
    val cleanRole = relationshipRole.trim().ifBlank { "Family Member" }
    val existing = getFamilyById(familyId) ?: Family(
      familyId = familyId,
      churchId = targetMember.churchId.ifBlank { defaultHeadMember?.churchId ?: "church_main" },
      familyName = defaultHeadMember?.effectiveFamilyName() ?: targetMember.effectiveFamilyName(),
      headUid = defaultHeadMember?.uid ?: "",
      headMemberId = defaultHeadMember?.memberId ?: "",
      headName = defaultHeadMember?.fullName ?: "",
      headPhone = defaultHeadMember?.phoneNumber ?: "",
      address = defaultHeadMember?.address ?: ""
    )

    val promoteToHead = makeHead || cleanRole.equals("Head", ignoreCase = true)
    val previousHeadUid = existing.headUid
    val targetEntry = FamilyMemberEntry(
      id = "linked_${targetMember.uid}",
      name = targetMember.fullName.ifBlank { "Church Member" },
      relationship = if (promoteToHead) "Head" else cleanRole,
      dateOfBirth = targetMember.dateOfBirth,
      gender = targetMember.gender,
      baptismStatus = targetMember.baptismStatus.ifBlank { "NOT_BAPTIZED" },
      phoneNumber = targetMember.phoneNumber,
      linkedUid = targetMember.uid,
      membershipId = targetMember.membershipId,
      profilePhotoUrl = targetMember.profilePhotoUrl
    )

    val baseList = existing.dependentMembers
      .filterNot {
        it.linkedUid == targetMember.uid ||
          (targetMember.membershipId.isNotBlank() && it.membershipId == targetMember.membershipId) ||
          (promoteToHead && it.relationship.equals("Head", ignoreCase = true) && it.linkedUid != targetMember.uid)
      }
      .toMutableList()

    // When transferring Head to another connected member, preserve the former Head in the roster
    if (promoteToHead &&
      previousHeadUid.isNotBlank() &&
      previousHeadUid != targetMember.uid &&
      baseList.none { it.linkedUid == previousHeadUid }
    ) {
      baseList.add(
        FamilyMemberEntry(
          id = "linked_$previousHeadUid",
          name = existing.headName.ifBlank { defaultHeadMember?.fullName ?: "Former Head" },
          relationship = "Family Member",
          phoneNumber = existing.headPhone.ifBlank { defaultHeadMember?.phoneNumber ?: "" },
          baptismStatus = defaultHeadMember?.baptismStatus ?: "BAPTIZED",
          linkedUid = previousHeadUid,
          membershipId = defaultHeadMember?.membershipId ?: "",
          profilePhotoUrl = defaultHeadMember?.profilePhotoUrl ?: ""
        )
      )
    }

    if (!promoteToHead || targetMember.uid != previousHeadUid) {
      // Keep non-head connected accounts in dependentMembers; if promoted head replaces old head,
      // head metadata sits in headUid/headName and doesn't duplicate if head isn't in dependentMembers.
      if (!promoteToHead) {
        baseList.add(targetEntry)
      }
    }

    val updatedLinked = (existing.linkedMemberUids + targetMember.uid + existing.headUid)
      .filter { it.isNotBlank() }
      .distinct()

    val updatedFamily = existing.copy(
      headUid = if (promoteToHead || existing.headUid.isBlank()) targetMember.uid else existing.headUid,
      headMemberId = if (promoteToHead || existing.headMemberId.isBlank()) targetMember.memberId else existing.headMemberId,
      headName = if (promoteToHead || existing.headName.isBlank()) targetMember.fullName else existing.headName,
      headPhone = if (promoteToHead || existing.headPhone.isBlank()) targetMember.phoneNumber else existing.headPhone,
      dependentMembers = baseList,
      linkedMemberUids = updatedLinked
    )

    val saved = upsertFamily(updatedFamily).getOrThrow()

    // Best-effort Pastor/Admin cross-user sync (silently ignored when executed by a regular Family Head,
    // whose relative adopts the familyId automatically on dashboard/profile load).
    if (auth.currentUser != null) {
      runCatching {
        db.collection("users").document(targetMember.uid).update(
          mapOf(
            "familyId" to saved.familyId,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        val memberDocId = targetMember.memberId.ifBlank { "mem_${targetMember.uid}" }
        db.collection("believers").document(memberDocId).set(
          mapOf(
            "familyId" to saved.familyId,
            "familyName" to saved.resolvedFamilyName(),
            "familyRole" to if (saved.headUid == targetMember.uid) "FAMILY_HEAD" else "FAMILY_MEMBER",
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          SetOptions.merge()
        ).await()
      }
    }

    saved
  }

  suspend fun updateMemberRoleInFamily(
    familyId: String,
    entryIdOrUid: String,
    newRole: String,
    promoteToHead: Boolean = false
  ): Result<Family> = runCatching {
    val cleanRole = newRole.trim().ifBlank { "Family Member" }
    val existing = getFamilyById(familyId) ?: throw IllegalStateException("Family not found")
    val targetEntry = existing.dependentMembers.firstOrNull {
      it.id == entryIdOrUid || (entryIdOrUid.isNotBlank() && it.linkedUid == entryIdOrUid)
    } ?: throw IllegalStateException("Family member entry not found")

    val shouldPromote = (promoteToHead || cleanRole.equals("Head", ignoreCase = true)) &&
      targetEntry.linkedUid.isNotBlank()

    val updated = if (shouldPromote) {
      val prevHeadUid = existing.headUid
      val remaining = existing.dependentMembers
        .filterNot { it.id == targetEntry.id || it.linkedUid == targetEntry.linkedUid }
        .toMutableList()
      if (prevHeadUid.isNotBlank() && prevHeadUid != targetEntry.linkedUid && remaining.none { it.linkedUid == prevHeadUid }) {
        remaining.add(
          FamilyMemberEntry(
            id = "linked_$prevHeadUid",
            name = existing.headName.ifBlank { "Former Head" },
            relationship = "Family Member",
            phoneNumber = existing.headPhone,
            linkedUid = prevHeadUid
          )
        )
      }
      existing.copy(
        headUid = targetEntry.linkedUid,
        headMemberId = "mem_${targetEntry.linkedUid}",
        headName = targetEntry.name,
        headPhone = targetEntry.phoneNumber,
        dependentMembers = remaining,
        linkedMemberUids = (existing.linkedMemberUids + targetEntry.linkedUid + prevHeadUid)
          .filter { it.isNotBlank() }
          .distinct()
      )
    } else {
      val updatedDependents = existing.dependentMembers.map { entry ->
        if (entry.id == targetEntry.id) entry.copy(relationship = cleanRole) else entry
      }
      existing.copy(dependentMembers = updatedDependents)
    }

    val saved = upsertFamily(updated).getOrThrow()

    if (auth.currentUser != null && shouldPromote && targetEntry.linkedUid.isNotBlank()) {
      runCatching {
        db.collection("believers").document("mem_${targetEntry.linkedUid}").set(
          mapOf("familyRole" to "FAMILY_HEAD", "updatedAt" to FieldValue.serverTimestamp()),
          SetOptions.merge()
        ).await()
        if (existing.headUid.isNotBlank() && existing.headUid != targetEntry.linkedUid) {
          db.collection("believers").document("mem_${existing.headUid}").set(
            mapOf("familyRole" to "FAMILY_MEMBER", "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
          ).await()
        }
      }
    }

    saved
  }

  suspend fun removeDependentMember(
    familyId: String,
    dependentId: String
  ): Result<Family> = runCatching {
    val existing = getFamilyById(familyId)
      ?: throw IllegalStateException("Family not found")
    val targetEntry = existing.dependentMembers.firstOrNull {
      it.id == dependentId || (dependentId.isNotBlank() && it.linkedUid == dependentId)
    }
    val updatedList = existing.dependentMembers.filterNot {
      it.id == dependentId || (dependentId.isNotBlank() && it.linkedUid == dependentId)
    }
    val removedUid = targetEntry?.linkedUid?.takeIf { it.isNotBlank() && it != existing.headUid }
    val updatedLinked = if (removedUid != null) {
      existing.linkedMemberUids.filterNot { it == removedUid }
    } else {
      existing.linkedMemberUids
    }
    val saved = upsertFamily(
      existing.copy(
        dependentMembers = updatedList,
        linkedMemberUids = updatedLinked
      )
    ).getOrThrow()

    if (auth.currentUser != null && removedUid != null) {
      runCatching {
        val standaloneFamId = "fam_$removedUid"
        db.collection("users").document(removedUid).update(
          mapOf("familyId" to standaloneFamId, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        db.collection("believers").document("mem_$removedUid").set(
          mapOf(
            "familyId" to standaloneFamId,
            "familyRole" to "FAMILY_HEAD",
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          SetOptions.merge()
        ).await()
      }
    }

    saved
  }

  suspend fun assignHouseholdHead(
    familyId: String,
    headUid: String,
    headMemberId: String,
    headName: String,
    headPhone: String = ""
  ): Result<Family> = runCatching {
    require(headUid.isNotBlank()) { "Household head UID is required" }
    val existing = getFamilyById(familyId) ?: Family(familyId = familyId)
    val prevHeadUid = existing.headUid
    val cleanedDependents = existing.dependentMembers
      .filterNot { it.linkedUid == headUid }
      .toMutableList()
    if (prevHeadUid.isNotBlank() && prevHeadUid != headUid && cleanedDependents.none { it.linkedUid == prevHeadUid }) {
      cleanedDependents.add(
        FamilyMemberEntry(
          id = "linked_$prevHeadUid",
          name = existing.headName.ifBlank { "Former Head" },
          relationship = "Family Member",
          phoneNumber = existing.headPhone,
          linkedUid = prevHeadUid
        )
      )
    }
    val updated = existing.copy(
      headUid = headUid,
      headMemberId = headMemberId,
      headName = headName.trim(),
      headPhone = headPhone.trim(),
      dependentMembers = cleanedDependents,
      linkedMemberUids = (existing.linkedMemberUids + headUid).filter { it.isNotBlank() }.distinct()
    )
    upsertFamily(updated).getOrThrow()
  }

  suspend fun linkAccountUidToFamily(
    familyId: String,
    uid: String
  ): Result<Family> = runCatching {
    require(uid.isNotBlank()) { "Member UID is required" }
    val existing = getFamilyById(familyId) ?: Family(familyId = familyId)
    val updatedLinked = (existing.linkedMemberUids + uid).filter { it.isNotBlank() }.distinct()
    upsertFamily(existing.copy(linkedMemberUids = updatedLinked)).getOrThrow()
  }

  suspend fun updateSharedSubscriptionStatus(
    familyId: String,
    status: SubscriptionStatus,
    subscriptionId: String = "",
    monthYear: String = "",
    paidDate: String = "",
    paidByUid: String = "",
    paidByName: String = "",
    monthlyDuesAmount: Double? = null
  ): Result<Family> = runCatching {
    val existing = getFamilyById(familyId) ?: Family(familyId = familyId)
    val updated = existing.copy(
      sharedSubscriptionStatus = status,
      lastSubscriptionId = subscriptionId.ifBlank { existing.lastSubscriptionId },
      lastPaidMonthYear = monthYear.ifBlank { existing.lastPaidMonthYear },
      lastPaidDate = paidDate.ifBlank { existing.lastPaidDate },
      lastPaidByUid = paidByUid.ifBlank { existing.lastPaidByUid },
      lastPaidByName = paidByName.ifBlank { existing.lastPaidByName },
      monthlyDuesAmount = monthlyDuesAmount ?: existing.monthlyDuesAmount
    )
    upsertFamily(updated).getOrThrow()
  }

  suspend fun syncFromBelieverMember(
    member: BelieverMember,
    subscription: MonthlySubscription? = null
  ): Result<Family> = runCatching {
    val famId = member.effectiveFamilyId()
    val existing = getFamilyById(famId)
    val isHead = existing == null || existing.headUid.isBlank() || existing.headUid == member.uid

    val mergedDependents = if (!isHead) {
      // Preserve household roster; refresh only this linked member's synced name/phone/photo/baptism
      val currentExisting = existing!!
      val hasEntry = currentExisting.dependentMembers.any { it.linkedUid == member.uid }
      if (hasEntry) {
        currentExisting.dependentMembers.map { entry ->
          if (entry.linkedUid == member.uid) {
            entry.copy(
              name = member.fullName.ifBlank { entry.name },
              phoneNumber = member.phoneNumber.ifBlank { entry.phoneNumber },
              baptismStatus = member.baptismStatus.ifBlank { entry.baptismStatus },
              gender = member.gender.ifBlank { entry.gender },
              dateOfBirth = member.dateOfBirth.ifBlank { entry.dateOfBirth },
              membershipId = member.membershipId.ifBlank { entry.membershipId },
              profilePhotoUrl = member.profilePhotoUrl.ifBlank { entry.profilePhotoUrl }
            )
          } else {
            entry
          }
        }
      } else {
        existing.dependentMembers
      }
    } else if (member.familyMembers.isNotEmpty()) {
      val connectedInExisting = existing?.dependentMembers
        ?.filter { it.linkedUid.isNotBlank() && it.linkedUid != member.uid }
        .orEmpty()
      val merged = member.familyMembers.toMutableList()
      connectedInExisting.forEach { connected ->
        if (merged.none { it.linkedUid == connected.linkedUid || it.id == connected.id }) {
          merged.add(connected)
        }
      }
      merged
    } else {
      existing?.dependentMembers ?: emptyList()
    }

    val mergedLinked = ((existing?.linkedMemberUids ?: emptyList()) + member.uid)
      .filter { it.isNotBlank() }
      .distinct()

    val family = Family(
      familyId = famId,
      churchId = member.churchId.ifBlank { existing?.churchId ?: "church_main" },
      familyName = if (isHead) member.effectiveFamilyName() else (existing?.familyName?.ifBlank { member.effectiveFamilyName() } ?: member.effectiveFamilyName()),
      headUid = if (isHead) member.uid.ifBlank { existing?.headUid ?: "" } else existing?.headUid ?: member.uid,
      headMemberId = if (isHead) member.memberId.ifBlank { existing?.headMemberId ?: "" } else existing?.headMemberId ?: member.memberId,
      headName = if (isHead) member.fullName.ifBlank { existing?.headName ?: "" } else existing?.headName ?: member.fullName,
      headPhone = if (isHead) member.phoneNumber.ifBlank { existing?.headPhone ?: "" } else existing?.headPhone ?: member.phoneNumber,
      address = if (isHead) member.address.ifBlank { existing?.address ?: "" } else (existing?.address?.ifBlank { member.address } ?: member.address),
      dependentMembers = mergedDependents,
      linkedMemberUids = mergedLinked,
      sharedSubscriptionStatus = subscription?.status ?: existing?.sharedSubscriptionStatus ?: SubscriptionStatus.PENDING,
      lastSubscriptionId = subscription?.subscriptionId?.ifBlank { existing?.lastSubscriptionId ?: "" } ?: existing?.lastSubscriptionId ?: "",
      lastPaidMonthYear = subscription?.monthYear?.ifBlank { existing?.lastPaidMonthYear ?: "" } ?: existing?.lastPaidMonthYear ?: "",
      lastPaidDate = subscription?.paidDate?.ifBlank { existing?.lastPaidDate ?: "" } ?: existing?.lastPaidDate ?: "",
      lastPaidByUid = subscription?.paidByUid?.ifBlank { existing?.lastPaidByUid ?: "" } ?: existing?.lastPaidByUid ?: "",
      lastPaidByName = subscription?.paidByName?.ifBlank { existing?.lastPaidByName ?: "" } ?: existing?.lastPaidByName ?: "",
      monthlyDuesAmount = subscription?.amountDue ?: existing?.monthlyDuesAmount ?: 500.0
    )

    upsertFamily(family).getOrThrow()
  }
}
