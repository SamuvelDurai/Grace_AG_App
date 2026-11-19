package com.example.core.util

import com.example.core.model.OfferingCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generates clean, sequential, human-readable GRACE AG church identifiers
 * (no random digits or UUID slices) backed by atomic Firestore counters
 * with a sequential local fallback for offline unit tests.
 */
object ChurchIdGenerator {

  private val localCounters = ConcurrentHashMap<String, AtomicInteger>()

  fun currentYear(date: Date = Date()): String =
    SimpleDateFormat("yyyy", Locale.US).format(date)

  fun currentMonthYm(date: Date = Date()): String =
    SimpleDateFormat("yyyyMM", Locale.US).format(date)

  fun adminId(year: String = currentYear()): String = "GAG-A-$year-001"

  fun verificationTokenFor(membershipId: String): String {
    val clean = membershipId.trim().ifBlank { "PENDING" }
    return "GAG-VERIFY-$clean"
  }

  private fun categoryCode(category: OfferingCategory): String = when (category) {
    OfferingCategory.TITHE -> "TITH"
    OfferingCategory.GENERAL_OFFERING -> "GEN"
    OfferingCategory.MISSIONARY_SUPPORT -> "MISS"
    OfferingCategory.BUILDING_FUND -> "BLDG"
    OfferingCategory.CHARITY_WELFARE -> "CHAR"
    OfferingCategory.SPECIAL_THANKSGIVING -> "THNK"
  }

  suspend fun nextMembershipId(
    db: FirebaseFirestore,
    churchId: String = "church_main"
  ): String {
    val year = currentYear()
    val seq = nextSequence(db, churchId, "member_$year")
    return "GAG-$year-${seq.toString().padStart(4, '0')}"
  }

  suspend fun nextPastorId(
    db: FirebaseFirestore,
    churchId: String = "church_main"
  ): String {
    val year = currentYear()
    val seq = nextSequence(db, churchId, "pastor_$year")
    return "GAG-P-$year-${seq.toString().padStart(3, '0')}"
  }

  suspend fun nextSubscriptionRequestId(
    db: FirebaseFirestore,
    churchId: String = "church_main",
    monthYm: String = currentMonthYm()
  ): String {
    val seq = nextSequence(db, churchId, "req_sub_$monthYm")
    return "REQ-SUB-$monthYm-${seq.toString().padStart(4, '0')}"
  }

  suspend fun nextSubscriptionReceiptId(
    db: FirebaseFirestore,
    churchId: String = "church_main",
    monthYm: String = currentMonthYm()
  ): String {
    val seq = nextSequence(db, churchId, "receipt_sub_$monthYm")
    return "REC-SUB-$monthYm-${seq.toString().padStart(4, '0')}"
  }

  suspend fun nextOfferingRequestId(
    db: FirebaseFirestore,
    churchId: String = "church_main",
    category: OfferingCategory,
    monthYm: String = currentMonthYm()
  ): String {
    val seq = nextSequence(db, churchId, "req_off_$monthYm")
    return "REQ-${categoryCode(category)}-$monthYm-${seq.toString().padStart(4, '0')}"
  }

  suspend fun nextOfferingReceiptId(
    db: FirebaseFirestore,
    churchId: String = "church_main",
    category: OfferingCategory,
    monthYm: String = currentMonthYm()
  ): String {
    val seq = nextSequence(db, churchId, "receipt_off_$monthYm")
    return "REC-${categoryCode(category)}-$monthYm-${seq.toString().padStart(4, '0')}"
  }

  private suspend fun nextSequence(
    db: FirebaseFirestore,
    churchId: String,
    counterKey: String
  ): Int {
    val safeChurchId = churchId.ifBlank { "church_main" }
    val localKey = "${safeChurchId}_$counterKey"
    if (FirebaseAuth.getInstance().currentUser == null) {
      return localCounters.getOrPut(localKey) { AtomicInteger(0) }.incrementAndGet()
    }

    val counterRef = db.collection("churches")
      .document(safeChurchId)
      .collection("counters")
      .document(counterKey)

    return try {
      db.runTransaction { tx ->
        val snap = tx.get(counterRef)
        val current = snap.getLong("lastNumber")?.toInt() ?: 0
        val next = current + 1
        tx.set(counterRef, mapOf("lastNumber" to next))
        next
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      localCounters.getOrPut(localKey) { AtomicInteger(0) }.incrementAndGet()
    }
  }

  fun resetLocalCountersForTest() {
    localCounters.clear()
  }
}
