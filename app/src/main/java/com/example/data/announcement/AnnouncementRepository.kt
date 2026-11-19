package com.example.data.announcement

import android.content.Context
import com.example.core.model.Announcement
import com.example.core.model.AnnouncementCategory
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

class AnnouncementRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultAnnouncements = mutableListOf(
    Announcement(
      announcementId = "ann_1",
      titleEnglish = "Sunday Morning Worship & Lord's Table",
      titleTamil = "ஞாயிறு காலை ஆராதனை மற்றும் திருவிருந்து",
      bodyEnglish = "Divine service commences at 09:00 AM in the sanctuary. Sunday school begins concurrently.",
      bodyTamil = "காலை 09:00 மணிக்கு பிரதான ஆராதனை மற்றும் திருவிருந்து நடைபெறும். அனைவரும் குடும்பமாய் வாருங்கள்.",
      category = AnnouncementCategory.SUNDAY_SERVICE,
      publishDate = "Today",
      authorName = "Senior Pastor",
      targetGroup = "ALL"
    )
  )

  private val _announcementsFlow = MutableStateFlow<List<Announcement>>(defaultAnnouncements.toList())

  fun observeAnnouncements(churchId: String = "church_main"): Flow<List<Announcement>> {
    if (auth.currentUser == null) {
      return _announcementsFlow.asStateFlow()
    }

    return db.collection("announcements")
      .whereEqualTo("churchId", churchId)
      .limit(25)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(Announcement::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "announcements")
        emit(emptyList())
      }
  }

  suspend fun publishAnnouncement(
    titleEn: String,
    titleTa: String,
    bodyEn: String,
    bodyTa: String,
    category: AnnouncementCategory,
    author: String,
    churchId: String = "church_main"
  ): Result<Announcement> = runCatching {
    val newAnn = Announcement(
      announcementId = "ann_${UUID.randomUUID().toString().take(8)}",
      churchId = churchId,
      titleEnglish = titleEn,
      titleTamil = titleTa,
      bodyEnglish = bodyEn,
      bodyTamil = bodyTa,
      category = category,
      publishDate = "Just now",
      authorName = author
    )
    defaultAnnouncements.add(0, newAnn)
    _announcementsFlow.value = defaultAnnouncements.toList()

    val currentUser = auth.currentUser
    if (currentUser != null) {
      val docRef = db.collection("announcements").document(newAnn.announcementId)
      val payload = hashMapOf<String, Any?>(
        "announcementId" to newAnn.announcementId,
        "churchId" to churchId,
        "titleEnglish" to titleEn,
        "titleTamil" to titleTa,
        "bodyEnglish" to bodyEn,
        "bodyTamil" to bodyTa,
        "category" to category.name,
        "publishDate" to newAnn.publishDate,
        "expiryDate" to newAnn.expiryDate,
        "isEmergency" to newAnn.isEmergency,
        "authorName" to author,
        "targetGroup" to newAnn.targetGroup,
        "createdBy" to currentUser.uid,
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
    newAnn
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> = runCatching {
    defaultAnnouncements.removeAll { it.announcementId == announcementId }
    _announcementsFlow.value = defaultAnnouncements.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("announcements").document(announcementId)
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
