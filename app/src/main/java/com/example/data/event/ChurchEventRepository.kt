package com.example.data.event

import android.content.Context
import com.example.core.model.ChurchEvent
import com.example.core.model.EventCategory
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

class ChurchEventRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultEvents = mutableListOf(
    ChurchEvent(
      eventId = "ev_1",
      churchId = "church_main",
      titleEnglish = "Annual Harvest & Thanksgiving Festival",
      titleTamil = "வருடாந்திர அறுவடை மற்றும் நன்றிப் பெருவிழா",
      descriptionEnglish = "Special praise celebration, thanksgiving auctions, musical cantata, and communal fellowship feast.",
      descriptionTamil = "சிறப்பு ஸ்தோத்திர ஆராதனை, நன்றி பலி காணிக்கை, நற்செய்தி இசை நிகழ்ச்சி மற்றும் சபை ஐக்கிய பந்தி.",
      eventDate = "Sunday, 22 Nov 2026",
      startTime = "09:30 AM",
      endTime = "02:00 PM",
      venue = "Main Sanctuary & Fellowship Grounds",
      capacity = 300,
      registeredCount = 184,
      attendeeIds = listOf("mem_1", "mem_2"),
      isRegisteredByCurrentUser = true,
      organizer = "Church Executive & Choir Committee",
      category = EventCategory.SPECIAL_FESTIVAL
    )
  )

  private val _eventsFlow = MutableStateFlow<List<ChurchEvent>>(defaultEvents.toList())

  fun observeEvents(churchId: String = "church_main"): Flow<List<ChurchEvent>> {
    val currentUid = auth.currentUser?.uid ?: return _eventsFlow.asStateFlow()

    return db.collection("events")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChurchEvent::class.java).map { ev ->
          ev.copy(isRegisteredByCurrentUser = ev.attendeeIds.contains(currentUid))
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "events")
        emit(emptyList())
      }
  }

  suspend fun toggleRsvp(eventId: String): Result<Unit> = runCatching {
    val currentUid = auth.currentUser?.uid
    if (currentUid != null) {
      val docRef = db.collection("events").document(eventId)
      val snap = try {
        docRef.get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }
      val event = snap.toObject(ChurchEvent::class.java)
        ?: throw IllegalStateException("Event not found")
      val nextAttendees = if (event.attendeeIds.contains(currentUid)) {
        event.attendeeIds.filterNot { it == currentUid }
      } else {
        (event.attendeeIds + currentUid).distinct()
      }
      try {
        docRef.update(
          mapOf(
            "attendeeIds" to nextAttendees,
            "registeredCount" to nextAttendees.size,
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
      val idx = defaultEvents.indexOfFirst { it.eventId == eventId }
      if (idx != -1) {
        val event = defaultEvents[idx]
        val nextStatus = !event.isRegisteredByCurrentUser
        val nextCount = if (nextStatus) event.registeredCount + 1 else (event.registeredCount - 1).coerceAtLeast(0)
        defaultEvents[idx] = event.copy(isRegisteredByCurrentUser = nextStatus, registeredCount = nextCount)
        _eventsFlow.value = defaultEvents.toList()
      }
    }
  }

  suspend fun createEvent(event: ChurchEvent): Result<ChurchEvent> = runCatching {
    val eventId = event.eventId.ifBlank { "ev_${UUID.randomUUID().toString().take(8)}" }
    val resolvedEvent = event.copy(
      eventId = eventId,
      titleTamil = event.titleTamil.ifBlank { event.titleEnglish },
      descriptionTamil = event.descriptionTamil.ifBlank { event.descriptionEnglish },
      registeredCount = maxOf(event.registeredCount, event.attendeeIds.size),
      capacity = event.capacity.coerceAtLeast(1)
    )

    defaultEvents.removeAll { it.eventId == eventId }
    defaultEvents.add(0, resolvedEvent)
    _eventsFlow.value = defaultEvents.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("events").document(eventId)
      val payload = buildFirestorePayload(resolvedEvent, isCreate = true)
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
    resolvedEvent
  }

  suspend fun createEvent(
    titleEn: String,
    titleTa: String,
    descEn: String,
    descTa: String,
    date: String,
    time: String,
    venue: String,
    churchId: String = "church_main"
  ): Result<ChurchEvent> = createEvent(
    ChurchEvent(
      churchId = churchId,
      titleEnglish = titleEn,
      titleTamil = titleTa,
      descriptionEnglish = descEn,
      descriptionTamil = descTa,
      eventDate = date,
      startTime = time,
      venue = venue
    )
  )

  suspend fun updateEvent(event: ChurchEvent): Result<Unit> = runCatching {
    val resolvedEvent = event.copy(
      titleTamil = event.titleTamil.ifBlank { event.titleEnglish },
      descriptionTamil = event.descriptionTamil.ifBlank { event.descriptionEnglish },
      capacity = event.capacity.coerceAtLeast(1)
    )
    val idx = defaultEvents.indexOfFirst { it.eventId == resolvedEvent.eventId }
    if (idx >= 0) {
      defaultEvents[idx] = resolvedEvent
    } else {
      defaultEvents.add(0, resolvedEvent)
    }
    _eventsFlow.value = defaultEvents.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("events").document(resolvedEvent.eventId)
      val payload = buildFirestorePayload(resolvedEvent, isCreate = false)
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

  suspend fun updateEventAttendees(eventId: String, newAttendeeIds: List<String>): Result<Unit> = runCatching {
    val distinctIds = newAttendeeIds.distinct()

    if (auth.currentUser != null) {
      val docRef = db.collection("events").document(eventId)
      try {
        docRef.update(
          mapOf(
            "attendeeIds" to distinctIds,
            "registeredCount" to distinctIds.size,
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
      val idx = defaultEvents.indexOfFirst { it.eventId == eventId }
      val existing = if (idx >= 0) defaultEvents[idx] else null
      val updatedCount = if (existing != null) {
        val delta = distinctIds.size - existing.attendeeIds.size
        (existing.registeredCount + delta).coerceAtLeast(distinctIds.size)
      } else {
        distinctIds.size
      }
      if (idx >= 0 && existing != null) {
        defaultEvents[idx] = existing.copy(attendeeIds = distinctIds, registeredCount = updatedCount)
        _eventsFlow.value = defaultEvents.toList()
      }
    }
  }

  suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
    defaultEvents.removeAll { it.eventId == eventId }
    _eventsFlow.value = defaultEvents.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("events").document(eventId)
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

  private fun buildFirestorePayload(event: ChurchEvent, isCreate: Boolean): HashMap<String, Any?> {
    return hashMapOf<String, Any?>(
      "eventId" to event.eventId,
      "churchId" to event.churchId,
      "titleEnglish" to event.titleEnglish,
      "titleTamil" to event.titleTamil,
      "descriptionEnglish" to event.descriptionEnglish,
      "descriptionTamil" to event.descriptionTamil,
      "eventDate" to event.eventDate,
      "startTime" to event.startTime,
      "endTime" to event.endTime,
      "venue" to event.venue,
      "isRsvpRequired" to event.isRsvpRequired,
      "capacity" to event.capacity,
      "registeredCount" to event.registeredCount,
      "attendeeIds" to event.attendeeIds,
      "organizer" to event.organizer,
      "category" to event.category.name,
      "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
      if (isCreate) {
        put("createdAt", FieldValue.serverTimestamp())
      }
    }
  }
}
