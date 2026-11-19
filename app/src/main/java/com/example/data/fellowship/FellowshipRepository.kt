package com.example.data.fellowship

import android.content.Context
import com.example.core.model.FellowshipCategory
import com.example.core.model.FellowshipGroup
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

class FellowshipRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultGroups = mutableListOf(
    FellowshipGroup(
      groupId = "fg_anna_nagar",
      nameEnglish = "Anna Nagar Grace Fellowship",
      nameTamil = "அண்ணா நகர் கிருபை ஐக்கியம்",
      areaZone = "Central Chennai (Zone 1)",
      leaderName = "Bro. Paulraj Joseph",
      leaderPhone = "+91 98401 23456",
      meetingSchedule = "Every Friday • 7:00 PM - 8:30 PM",
      meetingLocation = "Flat 4B, Shanthi Colony, Anna Nagar, Chennai",
      memberCount = 18,
      memberIds = listOf("mem_1"),
      currentStudyTopicEnglish = "Walking in Faith: Study on Book of James",
      currentStudyTopicTamil = "விசுவாசத்தில் நடத்தல்: யாக்கோபு நிருப தியானம்",
      category = FellowshipCategory.AREA_CARE_CELL
    ),
    FellowshipGroup(
      groupId = "fg_youth_cya",
      nameEnglish = "Ignite Christian Youth Fellowship (CYA)",
      nameTamil = "இக்னைட் வாலிபர் ஐக்கியம்",
      areaZone = "Church Main Hall & Outreach",
      leaderName = "Bro. Jason Durairaj (Youth Pastor)",
      leaderPhone = "+91 91760 99887",
      meetingSchedule = "2nd & 4th Saturday • 5:00 PM - 7:00 PM",
      meetingLocation = "Grace Baptist Youth Chapel (2nd Floor)",
      memberCount = 45,
      memberIds = listOf("mem_1"),
      currentStudyTopicEnglish = "Unashamed: Standing Firm on Campus & Career",
      currentStudyTopicTamil = "வெட்கப்படாமல் நிற்போம்: கல்லூரியிலும் பணியிலும் சாட்சி",
      category = FellowshipCategory.YOUTH_MINISTRY
    )
  )

  private val _fellowshipGroups = MutableStateFlow<List<FellowshipGroup>>(defaultGroups.toList())

  fun observeFellowshipGroups(churchId: String = "church_main"): Flow<List<FellowshipGroup>> {
    if (auth.currentUser == null) {
      return _fellowshipGroups.asStateFlow()
    }

    return db.collection("fellowshipGroups")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(FellowshipGroup::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "fellowshipGroups")
        emit(emptyList())
      }
  }

  fun observeGroupsByCategory(category: FellowshipCategory): Flow<List<FellowshipGroup>> {
    return observeFellowshipGroups().map { list ->
      list.filter { it.category == category }
    }
  }

  fun addFellowshipGroup(newGroup: FellowshipGroup) {
    val withId = if (newGroup.groupId.isBlank()) {
      newGroup.copy(groupId = "fg_${UUID.randomUUID().toString().take(8)}")
    } else {
      newGroup
    }
    defaultGroups.add(0, withId)
    _fellowshipGroups.value = defaultGroups.toList()
  }

  suspend fun createGroup(group: FellowshipGroup): Result<FellowshipGroup> = runCatching {
    val groupId = group.groupId.ifBlank { "fg_${UUID.randomUUID().toString().take(8)}" }
    val resolvedGroup = group.copy(
      groupId = groupId,
      nameTamil = group.nameTamil.ifBlank { group.nameEnglish },
      currentStudyTopicTamil = group.currentStudyTopicTamil.ifBlank { group.currentStudyTopicEnglish },
      memberCount = maxOf(group.memberCount, group.memberIds.size)
    )

    defaultGroups.removeAll { it.groupId == groupId }
    defaultGroups.add(0, resolvedGroup)
    _fellowshipGroups.value = defaultGroups.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("fellowshipGroups").document(groupId)
      val payload = buildFirestorePayload(resolvedGroup, isCreate = true)
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
    resolvedGroup
  }

  suspend fun updateGroup(group: FellowshipGroup): Result<Unit> = runCatching {
    val resolvedGroup = group.copy(
      nameTamil = group.nameTamil.ifBlank { group.nameEnglish },
      currentStudyTopicTamil = group.currentStudyTopicTamil.ifBlank { group.currentStudyTopicEnglish }
    )
    val idx = defaultGroups.indexOfFirst { it.groupId == resolvedGroup.groupId }
    if (idx >= 0) {
      defaultGroups[idx] = resolvedGroup
    } else {
      defaultGroups.add(0, resolvedGroup)
    }
    _fellowshipGroups.value = defaultGroups.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("fellowshipGroups").document(resolvedGroup.groupId)
      val payload = buildFirestorePayload(resolvedGroup, isCreate = false)
      try {
        docRef.set(payload, SetOptions.merge()).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  suspend fun updateGroupMembers(groupId: String, newMemberIds: List<String>): Result<Unit> = runCatching {
    val distinctIds = newMemberIds.distinct()

    if (auth.currentUser != null) {
      val docRef = db.collection("fellowshipGroups").document(groupId)
      try {
        docRef.update(
          mapOf(
            "memberIds" to distinctIds,
            "memberCount" to distinctIds.size,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    } else {
      val idx = defaultGroups.indexOfFirst { it.groupId == groupId }
      val existing = if (idx >= 0) defaultGroups[idx] else null
      val updatedCount = if (existing != null) {
        val delta = distinctIds.size - existing.memberIds.size
        (existing.memberCount + delta).coerceAtLeast(distinctIds.size)
      } else {
        distinctIds.size
      }
      if (idx >= 0 && existing != null) {
        defaultGroups[idx] = existing.copy(memberIds = distinctIds, memberCount = updatedCount)
        _fellowshipGroups.value = defaultGroups.toList()
      }
    }
  }

  suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
    defaultGroups.removeAll { it.groupId == groupId }
    _fellowshipGroups.value = defaultGroups.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("fellowshipGroups").document(groupId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  private fun buildFirestorePayload(group: FellowshipGroup, isCreate: Boolean): HashMap<String, Any?> {
    return hashMapOf<String, Any?>(
      "groupId" to group.groupId,
      "churchId" to group.churchId,
      "nameEnglish" to group.nameEnglish,
      "nameTamil" to group.nameTamil,
      "areaZone" to group.areaZone,
      "leaderName" to group.leaderName,
      "leaderPhone" to group.leaderPhone,
      "meetingSchedule" to group.meetingSchedule,
      "meetingLocation" to group.meetingLocation,
      "memberCount" to group.memberCount,
      "memberIds" to group.memberIds,
      "currentStudyTopicEnglish" to group.currentStudyTopicEnglish,
      "currentStudyTopicTamil" to group.currentStudyTopicTamil,
      "category" to group.category.name,
      "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
      if (isCreate) {
        put("createdAt", FieldValue.serverTimestamp())
      }
    }
  }
}
