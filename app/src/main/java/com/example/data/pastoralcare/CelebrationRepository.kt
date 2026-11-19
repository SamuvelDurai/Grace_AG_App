package com.example.data.pastoralcare

import android.content.Context
import com.example.core.model.BelieverMember
import com.example.core.model.CelebrationType
import com.example.core.model.CelebrationWish
import com.example.core.model.ChurchCelebration
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class CelebrationRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val localCelebrations = mutableListOf<ChurchCelebration>()
  private val _celebrationsFlow = MutableStateFlow<List<ChurchCelebration>>(emptyList())

  fun observeChurchCelebrations(churchId: String = "church_main"): Flow<List<ChurchCelebration>> {
    val cleanChurchId = churchId.ifBlank { "church_main" }
    if (auth.currentUser == null) {
      return _celebrationsFlow.asStateFlow().map { list ->
        val today = Calendar.getInstance()
        list.filter { it.churchId == cleanChurchId }.sortedBy { it.daysUntil(today) }
      }
    }

    return db.collection("churchCelebrations")
      .whereEqualTo("churchId", cleanChurchId)
      .limit(250)
      .snapshots()
      .map { snapshot ->
        val today = Calendar.getInstance()
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(ChurchCelebration::class.java)?.let {
            if (it.celebrationId.isBlank()) it.copy(celebrationId = doc.id) else it
          }
        }.sortedBy { it.daysUntil(today) }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "churchCelebrations")
        emit(emptyList())
      }
  }

  suspend fun syncBirthdayFromBeliever(member: BelieverMember): Result<Unit> = runCatching {
    val uid = member.uid.trim()
    val name = member.fullName.trim()
    if (uid.isBlank() || name.isBlank()) return@runCatching
    val parsed = ChurchCelebration.parseFlexibleDate(member.dateOfBirth) ?: return@runCatching
    val (monthDay, displayDate) = parsed
    val churchId = member.churchId.ifBlank { "church_main" }
    val celebrationId = "bday_$uid"

    upsertCelebrationInternal(
      celebrationId = celebrationId,
      churchId = churchId,
      memberUid = uid,
      memberName = name,
      profilePhotoUrl = member.profilePhotoUrl,
      type = CelebrationType.BIRTHDAY,
      monthDay = monthDay,
      displayDate = displayDate
    )
  }

  suspend fun saveCelebration(
    memberName: String,
    rawDate: String,
    type: CelebrationType = CelebrationType.BIRTHDAY,
    memberUid: String = "",
    profilePhotoUrl: String = "",
    churchId: String = "church_main"
  ): Result<ChurchCelebration> = runCatching {
    val cleanName = memberName.trim()
    require(cleanName.isNotBlank()) { "Member name is required." }
    val parsed = ChurchCelebration.parseFlexibleDate(rawDate)
      ?: throw IllegalArgumentException("Enter a valid date (e.g. 19/11/1995 or 19 Nov).")
    val (monthDay, displayDate) = parsed
    val cleanChurchId = churchId.ifBlank { "church_main" }
    val prefix = if (type == CelebrationType.BIRTHDAY) "bday" else "anniv"
    val cleanMemberUid = memberUid.trim().ifBlank { "custom_${UUID.randomUUID().toString().take(8)}" }
    val celebrationId = "${prefix}_$cleanMemberUid"

    upsertCelebrationInternal(
      celebrationId = celebrationId,
      churchId = cleanChurchId,
      memberUid = cleanMemberUid,
      memberName = cleanName,
      profilePhotoUrl = profilePhotoUrl,
      type = type,
      monthDay = monthDay,
      displayDate = displayDate
    )
  }

  private suspend fun upsertCelebrationInternal(
    celebrationId: String,
    churchId: String,
    memberUid: String,
    memberName: String,
    profilePhotoUrl: String,
    type: CelebrationType,
    monthDay: String,
    displayDate: String
  ): ChurchCelebration {
    val existing = localCelebrations.find { it.celebrationId == celebrationId }
    val resolved = (existing ?: ChurchCelebration()).copy(
      celebrationId = celebrationId,
      churchId = churchId,
      memberUid = memberUid,
      memberName = memberName,
      profilePhotoUrl = profilePhotoUrl.ifBlank { existing?.profilePhotoUrl.orEmpty() },
      type = type.name,
      monthDay = monthDay,
      displayDate = displayDate
    )
    localCelebrations.removeAll { it.celebrationId == celebrationId }
    localCelebrations.add(0, resolved)
    _celebrationsFlow.value = localCelebrations.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("churchCelebrations").document(celebrationId)
      val remoteSnap = runCatching { docRef.get().await() }.getOrNull()
      val basePayload = hashMapOf<String, Any?>(
        "celebrationId" to celebrationId,
        "churchId" to churchId,
        "memberUid" to memberUid,
        "memberName" to memberName,
        "profilePhotoUrl" to resolved.profilePhotoUrl,
        "type" to type.name,
        "monthDay" to monthDay,
        "displayDate" to displayDate,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (remoteSnap == null || !remoteSnap.exists()) {
        basePayload["wishCount"] = 0
        basePayload["wishedByUids"] = emptyList<String>()
        basePayload["recentWishes"] = emptyList<Map<String, Any?>>()
      }
      try {
        docRef.set(basePayload, SetOptions.merge()).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        throw e
      }
    }
    return resolved
  }

  suspend fun sendChurchFamilyWish(
    celebrationId: String,
    senderUid: String,
    senderName: String,
    message: String
  ): Result<Unit> = runCatching {
    val cleanId = celebrationId.trim()
    val cleanMessage = message.trim()
    require(cleanId.isNotBlank()) { "Celebration ID is required." }
    require(cleanMessage.isNotBlank()) { "Please enter a birthday wish message." }

    val effectiveSenderUid = auth.currentUser?.uid ?: senderUid.ifBlank { "local_member" }
    val effectiveSenderName = senderName.trim().ifBlank { "Church Family Member" }
    val timeText = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
    val newWish = CelebrationWish(
      senderUid = effectiveSenderUid,
      senderName = effectiveSenderName,
      message = cleanMessage.take(280),
      createdAtText = timeText
    )

    if (auth.currentUser == null) {
      val idx = localCelebrations.indexOfFirst { it.celebrationId == cleanId }
      if (idx >= 0) {
        val current = localCelebrations[idx]
        val nextUids = (current.wishedByUids + effectiveSenderUid).distinct()
        val nextWishes = (listOf(newWish) + current.recentWishes.filterNot { it.senderUid == effectiveSenderUid }).take(25)
        localCelebrations[idx] = current.copy(
          wishCount = nextUids.size,
          wishedByUids = nextUids,
          recentWishes = nextWishes
        )
        _celebrationsFlow.value = localCelebrations.toList()
      }
      return@runCatching
    }

    val docRef = db.collection("churchCelebrations").document(cleanId)
    val snap = try {
      docRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
    val current = snap.toObject(ChurchCelebration::class.java)
      ?: throw IllegalStateException("Celebration not found")

    val nextUids = (current.wishedByUids + effectiveSenderUid).distinct()
    val nextWishes = (listOf(newWish) + current.recentWishes.filterNot { it.senderUid == effectiveSenderUid })
      .take(25)
      .map { w ->
        mapOf(
          "senderUid" to w.senderUid,
          "senderName" to w.senderName,
          "message" to w.message,
          "createdAtText" to w.createdAtText
        )
      }

    try {
      docRef.update(
        mapOf(
          "wishCount" to nextUids.size,
          "wishedByUids" to nextUids,
          "recentWishes" to nextWishes,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteCelebration(celebrationId: String): Result<Unit> = runCatching {
    localCelebrations.removeAll { it.celebrationId == celebrationId }
    _celebrationsFlow.value = localCelebrations.toList()
    if (auth.currentUser != null && celebrationId.isNotBlank()) {
      val docRef = db.collection("churchCelebrations").document(celebrationId)
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
}
