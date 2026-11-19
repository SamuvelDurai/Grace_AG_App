package com.example.data.prayer

import android.content.Context
import com.example.core.model.PrayerPrivacy
import com.example.core.model.PrayerRequest
import com.example.core.model.PrayerStatus
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PrayerRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultPrayers = mutableListOf(
    PrayerRequest(
      requestId = "pr_1",
      uid = "local_member_1",
      requesterName = "Samuel Durai",
      title = "Healing for Family Elder",
      description = "Please uphold my grandmother who is currently undergoing treatment for heart ailment.",
      privacy = PrayerPrivacy.PASTOR_ONLY,
      status = PrayerStatus.PRAYED_FOR,
      dateSubmitted = "11 Nov 2026",
      pastoralNote = "Pastor Stephen prayed with the elder over phone. Praying for swift recovery.",
      pastoralResponseDate = "12 Nov 2026"
    )
  )

  private val _prayersFlow = MutableStateFlow<List<PrayerRequest>>(defaultPrayers.toList())

  fun observeAllPrayerRequests(churchId: String = "church_main"): Flow<List<PrayerRequest>> {
    if (auth.currentUser == null) {
      return _prayersFlow.asStateFlow()
    }

    return db.collection("prayerRequests")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(PrayerRequest::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "prayerRequests")
        emit(emptyList())
      }
  }

  fun observeUserPrayerRequests(uid: String): Flow<List<PrayerRequest>> {
    if (auth.currentUser == null) {
      return _prayersFlow.map { list -> list.filter { it.uid == uid } }
    }

    return db.collection("prayerRequests")
      .whereEqualTo("uid", uid)
      .limit(30)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(PrayerRequest::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "prayerRequests?uid=$uid")
        emit(emptyList())
      }
  }

  suspend fun submitPrayerRequest(
    uid: String,
    name: String,
    title: String,
    description: String,
    privacy: PrayerPrivacy,
    isAnonymous: Boolean,
    churchId: String = "church_main"
  ): Result<PrayerRequest> = runCatching {
    val effectiveUid = auth.currentUser?.uid ?: uid
    val req = PrayerRequest(
      requestId = UUID.randomUUID().toString(),
      churchId = churchId,
      uid = effectiveUid,
      requesterName = if (isAnonymous) "Anonymous Believer" else name,
      isAnonymous = isAnonymous,
      title = title,
      description = description,
      privacy = privacy,
      status = PrayerStatus.NEW,
      dateSubmitted = "Today"
    )
    defaultPrayers.add(0, req)
    _prayersFlow.value = defaultPrayers.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("prayerRequests").document(req.requestId)
      val payload = hashMapOf<String, Any?>(
        "requestId" to req.requestId,
        "churchId" to churchId,
        "uid" to effectiveUid,
        "requesterName" to req.requesterName,
        "isAnonymous" to isAnonymous,
        "title" to title,
        "description" to description,
        "privacy" to privacy.name,
        "status" to PrayerStatus.NEW.name,
        "dateSubmitted" to req.dateSubmitted,
        "pastoralNote" to "",
        "pastoralResponseDate" to "",
        "createdAt" to FieldValue.serverTimestamp()
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
    req
  }

  suspend fun updatePrayerStatus(
    requestId: String,
    status: PrayerStatus,
    pastoralNote: String
  ): Result<Unit> = runCatching {
    val idx = defaultPrayers.indexOfFirst { it.requestId == requestId }
    if (idx != -1) {
      val existing = defaultPrayers[idx]
      defaultPrayers[idx] = existing.copy(
        status = status,
        pastoralNote = pastoralNote,
        pastoralResponseDate = "Today"
      )
      _prayersFlow.value = defaultPrayers.toList()
    }

    if (auth.currentUser != null) {
      val docRef = db.collection("prayerRequests").document(requestId)
      try {
        docRef.update(
          mapOf(
            "status" to status.name,
            "pastoralNote" to pastoralNote,
            "pastoralResponseDate" to "Today",
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
  }
}
