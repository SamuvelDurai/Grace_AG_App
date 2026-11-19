package com.example.data.pastoralcare

import android.content.Context
import com.example.core.model.PastoralCareAlert
import com.example.core.model.PastoralCareStatus
import com.example.core.model.PastoralCareType
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

class PastoralCareRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val localAlerts = mutableListOf(
    PastoralCareAlert(
      alertId = "pca_001",
      churchId = "church_main",
      memberUid = "u_samuel",
      memberName = "Bro. Samuel Durai",
      memberPhone = "+91 98765 43210",
      membershipId = "GAG-2026-0001",
      careType = PastoralCareType.BIRTHDAY,
      eventDate = "Today",
      milestoneYear = 28,
      notes = "Active youth worship team member & prayer cell participant",
      locationDetails = "Anna Nagar, Chennai",
      status = PastoralCareStatus.PENDING,
      isToday = true
    ),
    PastoralCareAlert(
      alertId = "pca_002",
      churchId = "church_main",
      memberUid = "u_jebasingh",
      memberName = "Mr. & Mrs. Jebasingh",
      memberPhone = "+91 94440 22119",
      membershipId = "GAG-2026-0002",
      careType = PastoralCareType.WEDDING_ANNIVERSARY,
      eventDate = "Today",
      milestoneYear = 25,
      notes = "Silver Wedding Jubilee celebration - Family prayer in evening",
      locationDetails = "Velachery, Chennai",
      status = PastoralCareStatus.PENDING,
      isToday = true
    ),
    PastoralCareAlert(
      alertId = "pca_003",
      churchId = "church_main",
      memberUid = "u_mary_paul",
      memberName = "Sis. Mary Paul",
      memberPhone = "+91 98410 77889",
      membershipId = "GAG-2026-0003",
      careType = PastoralCareType.HOSPITAL_VISIT,
      eventDate = "Today",
      notes = "Underwent knee replacement surgery yesterday; requested pastor's communion & anointing prayer",
      locationDetails = "Room 304, Apollo Hospitals, Greams Road, Thousand Lights",
      status = PastoralCareStatus.PENDING,
      isToday = true
    )
  )

  private val _alerts = MutableStateFlow<List<PastoralCareAlert>>(localAlerts.toList())

  fun observeAlerts(churchId: String = "church_main"): Flow<List<PastoralCareAlert>> {
    if (auth.currentUser == null) {
      return _alerts.asStateFlow()
    }

    return db.collection("pastoralCareAlerts")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(PastoralCareAlert::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "pastoralCareAlerts")
        emit(emptyList())
      }
  }

  fun observeTodayAlerts(churchId: String = "church_main"): Flow<List<PastoralCareAlert>> {
    return observeAlerts(churchId).map { list -> list.filter { it.isToday } }
  }

  fun observeTodayCelebrations(memberUid: String? = null): Flow<List<PastoralCareAlert>> {
    val currentUser = auth.currentUser
    if (currentUser == null) {
      return _alerts.map { list ->
        list.filter {
          it.careType == PastoralCareType.BIRTHDAY || it.careType == PastoralCareType.WEDDING_ANNIVERSARY
        }
      }
    }

    val targetUid = memberUid?.takeIf { it.isNotBlank() } ?: currentUser.uid
    return db.collection("pastoralCareAlerts")
      .whereEqualTo("memberUid", targetUid)
      .limit(20)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(PastoralCareAlert::class.java).filter {
          it.careType == PastoralCareType.BIRTHDAY || it.careType == PastoralCareType.WEDDING_ANNIVERSARY
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "pastoralCareAlerts?memberUid=$targetUid")
        emit(emptyList())
      }
  }

  suspend fun updateAlertStatus(alertId: String, newStatus: PastoralCareStatus): Result<Unit> = runCatching {
    val idx = localAlerts.indexOfFirst { it.alertId == alertId }
    if (idx >= 0) {
      localAlerts[idx] = localAlerts[idx].copy(status = newStatus)
      _alerts.value = localAlerts.toList()
    }

    if (auth.currentUser != null) {
      val docRef = db.collection("pastoralCareAlerts").document(alertId)
      try {
        docRef.update(
          mapOf(
            "status" to newStatus.name,
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

  suspend fun requestPastoralVisit(
    memberUid: String,
    memberName: String,
    memberPhone: String,
    membershipId: String,
    careType: PastoralCareType,
    notes: String,
    locationDetails: String,
    churchId: String = "church_main"
  ): Result<PastoralCareAlert> = runCatching {
    val effectiveUid = auth.currentUser?.uid ?: memberUid
    val alertId = "pca_${UUID.randomUUID().toString().take(8)}"
    val newAlert = PastoralCareAlert(
      alertId = alertId,
      churchId = churchId,
      memberUid = effectiveUid,
      memberName = memberName.ifBlank { "Church Member" },
      memberPhone = memberPhone,
      membershipId = membershipId,
      careType = careType,
      eventDate = "Today",
      notes = notes.trim(),
      locationDetails = locationDetails.trim(),
      status = PastoralCareStatus.PENDING,
      isToday = true
    )

    localAlerts.add(0, newAlert)
    _alerts.value = localAlerts.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("pastoralCareAlerts").document(alertId)
      val payload = hashMapOf<String, Any?>(
        "alertId" to newAlert.alertId,
        "churchId" to newAlert.churchId,
        "memberUid" to newAlert.memberUid,
        "memberName" to newAlert.memberName,
        "memberPhone" to newAlert.memberPhone,
        "membershipId" to newAlert.membershipId,
        "careType" to newAlert.careType.name,
        "eventDate" to newAlert.eventDate,
        "milestoneYear" to newAlert.milestoneYear,
        "notes" to newAlert.notes,
        "locationDetails" to newAlert.locationDetails,
        "status" to newAlert.status.name,
        "isToday" to newAlert.isToday,
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

    newAlert
  }
}
