package com.example.data.sermon

import android.content.Context
import com.example.core.model.Sermon
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

class SermonRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultSermons = mutableListOf(
    Sermon(
      sermonId = "serm_1",
      titleEnglish = "Walking in Divine Faith and Victory",
      titleTamil = "விசுவாசத்தில் நடப்போம் - வெற்றி பெறுவோம்",
      speakerName = "Rev. Dr. Stephen Paul",
      scriptureReference = "Hebrews 11:1-6",
      dateDelivered = "Sunday, 08 Nov 2026",
      notesSummary = "1. The essence of unseen faith.\n2. Overcoming spiritual discouragement.\n3. The promise of God's abiding presence.",
      durationMinutes = 42
    )
  )

  private val _sermonsFlow = MutableStateFlow<List<Sermon>>(defaultSermons.toList())

  fun observeSermons(churchId: String = "church_main"): Flow<List<Sermon>> {
    if (auth.currentUser == null) {
      return _sermonsFlow.asStateFlow()
    }

    return db.collection("sermons")
      .whereEqualTo("churchId", churchId)
      .limit(25)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(Sermon::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "sermons")
        emit(emptyList())
      }
  }

  suspend fun publishSermon(
    titleEn: String,
    titleTa: String,
    speaker: String,
    ref: String,
    notes: String,
    videoUrl: String,
    churchId: String = "church_main"
  ): Result<Sermon> = runCatching {
    val s = Sermon(
      sermonId = "serm_${UUID.randomUUID().toString().take(8)}",
      churchId = churchId,
      titleEnglish = titleEn,
      titleTamil = titleTa,
      speakerName = speaker,
      scriptureReference = ref,
      dateDelivered = "Today",
      notesSummary = notes,
      videoUrl = videoUrl
    )
    defaultSermons.add(0, s)
    _sermonsFlow.value = defaultSermons.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("sermons").document(s.sermonId)
      val payload = hashMapOf<String, Any?>(
        "sermonId" to s.sermonId,
        "churchId" to churchId,
        "titleEnglish" to titleEn,
        "titleTamil" to titleTa,
        "speakerName" to speaker,
        "scriptureReference" to ref,
        "dateDelivered" to s.dateDelivered,
        "notesSummary" to notes,
        "videoUrl" to videoUrl,
        "durationMinutes" to s.durationMinutes,
        "category" to s.category,
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
    s
  }

  suspend fun deleteSermon(sermonId: String): Result<Unit> = runCatching {
    defaultSermons.removeAll { it.sermonId == sermonId }
    _sermonsFlow.value = defaultSermons.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("sermons").document(sermonId)
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
