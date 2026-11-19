package com.example.data.finance

import android.content.Context
import com.example.core.model.BelieverMember
import com.example.core.model.MonthlySubscription
import com.example.core.model.OfferingCategory
import com.example.core.model.OfferingTransaction
import com.example.core.model.PaymentMethod
import com.example.core.model.PaymentTransactionStatus
import com.example.core.model.SubscriptionStatus
import com.example.core.util.ChurchIdGenerator
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.example.data.family.FamilyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val defaultSubscriptions = mutableListOf(
    MonthlySubscription(
      subscriptionId = "sub_fam_local_member_1_2026_11",
      uid = "local_member_1",
      familyId = "fam_local_member_1",
      familyName = "Samuel Durai Family",
      familyMemberCount = 3,
      memberName = "Samuel Durai",
      membershipId = "GAG-2026-0001",
      monthYear = currentMonthYearLabel(),
      amountDue = 500.0,
      amountPaid = 500.0,
      status = SubscriptionStatus.PAID,
      dueDate = "10 ${SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())}",
      paidDate = "05 ${SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())}",
      paidByUid = "local_member_1",
      paidByName = "Samuel Durai",
      paymentMethod = PaymentMethod.UPI_ONLINE.name,
      receiptNumber = "REC-SUB-202611-0001"
    )
  )

  private val defaultOfferings = mutableListOf(
    OfferingTransaction(
      transactionId = "tx_1",
      uid = "local_member_1",
      donorName = "Samuel Durai",
      membershipId = "GAG-2026-0001",
      category = OfferingCategory.TITHE,
      amount = 3500.0,
      paymentMethod = PaymentMethod.UPI_ONLINE,
      status = PaymentTransactionStatus.SUCCESS,
      transactionDate = "05 Nov 2026",
      note = "Monthly Tithe",
      receiptNumber = "REC-TITH-202611-0001"
    )
  )

  private val _subscriptionsFlow = MutableStateFlow<List<MonthlySubscription>>(defaultSubscriptions.toList())
  private val _offeringsFlow = MutableStateFlow<List<OfferingTransaction>>(defaultOfferings.toList())

  fun currentMonthKey(date: Date = Date()): String =
    SimpleDateFormat("yyyy_MM", Locale.US).format(date)

  fun currentMonthYearLabel(date: Date = Date()): String =
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(date)

  private fun todayLabel(date: Date = Date()): String =
    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(date)

  private fun sanitizeIdSegment(raw: String): String =
    raw.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")

  fun subscriptionDocId(familyId: String, monthKey: String = currentMonthKey()): String =
    "sub_${sanitizeIdSegment(familyId)}_$monthKey"

  fun observeSubscriptions(churchId: String = "church_main"): Flow<List<MonthlySubscription>> {
    if (auth.currentUser == null) {
      return _subscriptionsFlow.asStateFlow()
    }

    return db.collection("subscriptions")
      .whereEqualTo("churchId", churchId)
      .limit(250)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(MonthlySubscription::class.java)?.let {
            if (it.subscriptionId.isBlank()) it.copy(subscriptionId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "subscriptions")
        emit(emptyList())
      }
  }

  fun observeUserSubscriptions(uid: String, familyId: String = ""): Flow<List<MonthlySubscription>> {
    val effectiveFamily = familyId.ifBlank { if (uid.isNotBlank()) "fam_$uid" else "" }

    if (auth.currentUser == null) {
      return _subscriptionsFlow.map { list ->
        list.filter { sub ->
          sub.uid == uid || (effectiveFamily.isNotBlank() && sub.familyId == effectiveFamily)
        }
      }
    }

    val byUidFlow = db.collection("subscriptions")
      .whereEqualTo("uid", uid)
      .limit(50)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(MonthlySubscription::class.java)?.let {
            if (it.subscriptionId.isBlank()) it.copy(subscriptionId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "subscriptions?uid=$uid")
        emit(emptyList())
      }

    if (effectiveFamily.isBlank()) {
      return byUidFlow
    }

    val byFamilyFlow = db.collection("subscriptions")
      .whereEqualTo("familyId", effectiveFamily)
      .limit(50)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(MonthlySubscription::class.java)?.let {
            if (it.subscriptionId.isBlank()) it.copy(subscriptionId = doc.id) else it
          }
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "subscriptions?familyId=$effectiveFamily")
        emit(emptyList())
      }

    return combine(byFamilyFlow, byUidFlow) { familyList, uidList ->
      (familyList + uidList).distinctBy { it.subscriptionId.ifBlank { "${it.familyId}_${it.monthYear}" } }
    }
  }

  fun observeOfferings(churchId: String = "church_main"): Flow<List<OfferingTransaction>> {
    if (auth.currentUser == null) {
      return _offeringsFlow.asStateFlow()
    }

    return db.collection("offerings")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(OfferingTransaction::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "offerings")
        emit(emptyList())
      }
  }

  fun observeUserOfferings(uid: String): Flow<List<OfferingTransaction>> {
    if (auth.currentUser == null) {
      return _offeringsFlow.map { list -> list.filter { it.uid == uid } }
    }

    return db.collection("offerings")
      .whereEqualTo("uid", uid)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(OfferingTransaction::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "offerings?uid=$uid")
        emit(emptyList())
      }
  }

  /**
   * Generates at most ONE monthly subscription bill per family (grouped by effectiveFamilyId),
   * never per individual family member.
   * Returns the number of newly created family bills for the current month.
   */
  suspend fun generateMonthlyFamilySubscriptions(
    members: List<BelieverMember>,
    amountDue: Double,
    churchId: String = "church_main",
    monthKey: String = currentMonthKey(),
    monthYear: String = currentMonthYearLabel()
  ): Result<Int> = runCatching {
    val activeMembers = members.filter {
      it.membershipStatus != "REJECTED" && it.membershipStatus != "SUSPENDED"
    }
    if (activeMembers.isEmpty()) return@runCatching 0

    val families = activeMembers.groupBy { it.effectiveFamilyId() }
    val dueText = "10 ${SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())}"
    var createdCount = 0

    for ((familyId, familyAccounts) in families) {
      if (familyId.isBlank()) continue
      val head = familyAccounts.firstOrNull { it.familyRole == "FAMILY_HEAD" } ?: familyAccounts.first()
      val totalPersons = maxOf(familyAccounts.size, head.totalFamilyMembersCount())
      val docId = subscriptionDocId(familyId, monthKey)
      val famName = head.effectiveFamilyName()

      if (auth.currentUser == null) {
        val existsInMem = defaultSubscriptions.any {
          it.subscriptionId == docId || (it.familyId == familyId && it.monthYear == monthYear)
        }
        if (!existsInMem) {
          defaultSubscriptions.add(
            0,
            MonthlySubscription(
              subscriptionId = docId,
              churchId = churchId,
              uid = head.uid.ifBlank { head.memberId },
              familyId = familyId,
              familyName = famName,
              familyMemberCount = totalPersons,
              memberName = head.fullName,
              membershipId = head.membershipId,
              monthYear = monthYear,
              amountDue = amountDue,
              amountPaid = 0.0,
              status = SubscriptionStatus.PENDING,
              dueDate = dueText
            )
          )
          createdCount++
        }
        continue
      }

      val docRef = db.collection("subscriptions").document(docId)
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val payload = hashMapOf<String, Any?>(
          "subscriptionId" to docId,
          "churchId" to churchId,
          "uid" to head.uid.ifBlank { head.memberId },
          "familyId" to familyId,
          "familyName" to famName,
          "familyMemberCount" to totalPersons,
          "memberName" to head.fullName,
          "membershipId" to head.membershipId,
          "monthYear" to monthYear,
          "amountDue" to amountDue,
          "amountPaid" to 0.0,
          "status" to SubscriptionStatus.PENDING.name,
          "dueDate" to dueText,
          "paidDate" to "",
          "paidByUid" to "",
          "paidByName" to "",
          "paymentMethod" to "",
          "transactionReference" to "",
          "receiptNumber" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
          docRef.set(payload).await()
          createdCount++
          runCatching {
            FamilyRepository(db).updateSharedSubscriptionStatus(
              familyId = familyId,
              status = SubscriptionStatus.PENDING,
              subscriptionId = docId,
              monthYear = monthYear,
              monthlyDuesAmount = amountDue
            )
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, docRef.path)
          throw e
        }
      }
    }

    if (auth.currentUser == null && createdCount > 0) {
      _subscriptionsFlow.value = defaultSubscriptions.toList()
    }
    createdCount
  }

  /**
   * Believer submits payment proof (UTR/Transaction Reference or Cash handover request)
   * for their household's single monthly subscription.
   * Status becomes PENDING_VERIFICATION until a Pastor or Super Admin verifies the payment.
   */
  suspend fun submitFamilySubscriptionPayment(
    existingSubscriptionId: String?,
    familyId: String,
    familyName: String,
    familyMemberCount: Int,
    payerUid: String,
    payerName: String,
    membershipId: String,
    amountDue: Double,
    paymentMethod: PaymentMethod = PaymentMethod.UPI_ONLINE,
    transactionReference: String = "",
    churchId: String = "church_main"
  ): Result<MonthlySubscription> = runCatching {
    val cleanRef = transactionReference.trim()
    if (paymentMethod != PaymentMethod.CASH_OFFLINE) {
      require(cleanRef.length >= 6) {
        "Enter your UPI / Bank Transaction Reference ID (minimum 6 characters)."
      }
    }

    val effectiveUid = auth.currentUser?.uid ?: payerUid
    val effectiveFamilyId = familyId.ifBlank { "fam_$effectiveUid" }
    val monthKey = currentMonthKey()
    val monthYear = currentMonthYearLabel()
    val docId = existingSubscriptionId?.takeIf { it.isNotBlank() }
      ?: subscriptionDocId(effectiveFamilyId, monthKey)
    val submissionRef = ChurchIdGenerator.nextSubscriptionRequestId(db, churchId)
    val today = todayLabel()

    if (auth.currentUser != null) {
      val docRef = db.collection("subscriptions").document(docId)
      val snap = try {
        docRef.get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }

      if (snap.exists()) {
        val existing = snap.toObject(MonthlySubscription::class.java)
          ?: throw IllegalStateException("Subscription not found")
        if (existing.status == SubscriptionStatus.PAID) {
          return@runCatching existing
        }

        val updated = existing.copy(
          amountPaid = 0.0,
          status = SubscriptionStatus.PENDING_VERIFICATION,
          paidDate = today,
          paidByUid = effectiveUid,
          paidByName = payerName.ifBlank { existing.memberName },
          paymentMethod = paymentMethod.name,
          transactionReference = cleanRef.ifBlank { "CASH_AT_OFFICE" },
          receiptNumber = existing.receiptNumber.ifBlank { submissionRef }
        )

        try {
          docRef.update(
            mapOf(
              "amountPaid" to 0.0,
              "status" to SubscriptionStatus.PENDING_VERIFICATION.name,
              "paidDate" to today,
              "paidByUid" to updated.paidByUid,
              "paidByName" to updated.paidByName,
              "paymentMethod" to updated.paymentMethod,
              "transactionReference" to updated.transactionReference,
              "receiptNumber" to updated.receiptNumber,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
          runCatching {
            FamilyRepository(db).updateSharedSubscriptionStatus(
              familyId = updated.familyId.ifBlank { effectiveFamilyId },
              status = SubscriptionStatus.PENDING_VERIFICATION,
              subscriptionId = updated.subscriptionId,
              monthYear = updated.monthYear,
              paidDate = today,
              paidByUid = updated.paidByUid,
              paidByName = updated.paidByName,
              monthlyDuesAmount = updated.amountDue
            )
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.UPDATE, docRef.path)
          throw e
        }
        return@runCatching updated
      }

      val sub = MonthlySubscription(
        subscriptionId = docId,
        churchId = churchId,
        uid = effectiveUid,
        familyId = effectiveFamilyId,
        familyName = familyName.ifBlank { "${payerName.ifBlank { "Member" }} Family" },
        familyMemberCount = familyMemberCount.coerceAtLeast(1),
        memberName = payerName,
        membershipId = membershipId,
        monthYear = monthYear,
        amountDue = amountDue,
        amountPaid = 0.0,
        status = SubscriptionStatus.PENDING_VERIFICATION,
        dueDate = "10 ${SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())}",
        paidDate = today,
        paidByUid = effectiveUid,
        paidByName = payerName,
        paymentMethod = paymentMethod.name,
        transactionReference = cleanRef.ifBlank { "CASH_AT_OFFICE" },
        receiptNumber = submissionRef
      )
      val payload = hashMapOf<String, Any?>(
        "subscriptionId" to sub.subscriptionId,
        "churchId" to sub.churchId,
        "uid" to sub.uid,
        "familyId" to sub.familyId,
        "familyName" to sub.familyName,
        "familyMemberCount" to sub.familyMemberCount,
        "memberName" to sub.memberName,
        "membershipId" to sub.membershipId,
        "monthYear" to sub.monthYear,
        "amountDue" to sub.amountDue,
        "amountPaid" to 0.0,
        "status" to SubscriptionStatus.PENDING_VERIFICATION.name,
        "dueDate" to sub.dueDate,
        "paidDate" to sub.paidDate,
        "paidByUid" to sub.paidByUid,
        "paidByName" to sub.paidByName,
        "paymentMethod" to sub.paymentMethod,
        "transactionReference" to sub.transactionReference,
        "receiptNumber" to sub.receiptNumber,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload).await()
        runCatching {
          FamilyRepository(db).updateSharedSubscriptionStatus(
            familyId = effectiveFamilyId,
            status = SubscriptionStatus.PENDING_VERIFICATION,
            subscriptionId = docId,
            monthYear = monthYear,
            paidDate = today,
            paidByUid = effectiveUid,
            paidByName = payerName,
            monthlyDuesAmount = amountDue
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
      return@runCatching sub
    }

    val existingIdx = defaultSubscriptions.indexOfFirst {
      it.subscriptionId == docId || (it.familyId == effectiveFamilyId && it.monthYear == monthYear)
    }
    if (existingIdx >= 0) {
      val existing = defaultSubscriptions[existingIdx]
      if (existing.status == SubscriptionStatus.PAID) return@runCatching existing
      val updated = existing.copy(
        amountPaid = 0.0,
        status = SubscriptionStatus.PENDING_VERIFICATION,
        paidDate = today,
        paidByUid = effectiveUid,
        paidByName = payerName,
        paymentMethod = paymentMethod.name,
        transactionReference = cleanRef.ifBlank { "CASH_AT_OFFICE" },
        receiptNumber = submissionRef
      )
      defaultSubscriptions[existingIdx] = updated
      _subscriptionsFlow.value = defaultSubscriptions.toList()
      return@runCatching updated
    }

    val created = MonthlySubscription(
      subscriptionId = docId,
      churchId = churchId,
      uid = effectiveUid,
      familyId = effectiveFamilyId,
      familyName = familyName.ifBlank { "${payerName.ifBlank { "Member" }} Family" },
      familyMemberCount = familyMemberCount.coerceAtLeast(1),
      memberName = payerName,
      membershipId = membershipId,
      monthYear = monthYear,
      amountDue = amountDue,
      amountPaid = 0.0,
      status = SubscriptionStatus.PENDING_VERIFICATION,
      dueDate = "10 ${SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date())}",
      paidDate = today,
      paidByUid = effectiveUid,
      paidByName = payerName,
      paymentMethod = paymentMethod.name,
      transactionReference = cleanRef.ifBlank { "CASH_AT_OFFICE" },
      receiptNumber = submissionRef
    )
    defaultSubscriptions.add(0, created)
    _subscriptionsFlow.value = defaultSubscriptions.toList()
    created
  }

  /**
   * Pastor or Super Admin verifies a submitted family payment (or collects cash in person)
   * and officially marks the household subscription as PAID.
   */
  suspend fun paySubscription(
    subscriptionId: String,
    paidByUid: String = auth.currentUser?.uid ?: "",
    paidByName: String = "",
    paymentMethod: String = PaymentMethod.CASH_OFFLINE.name
  ): Result<MonthlySubscription> = runCatching {
    val today = todayLabel()

    if (auth.currentUser != null) {
      val docRef = db.collection("subscriptions").document(subscriptionId)
      val snap = try {
        docRef.get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }

      val existing = snap.toObject(MonthlySubscription::class.java)
        ?: throw IllegalStateException("Subscription not found")

      if (existing.status == SubscriptionStatus.PAID) {
        return@runCatching existing
      }

      val receipt = ChurchIdGenerator.nextSubscriptionReceiptId(db, existing.churchId.ifBlank { "church_main" })
      val payerDisplay = existing.paidByName.ifBlank { paidByName.ifBlank { existing.memberName } }
      val resolvedMethod = existing.paymentMethod.ifBlank { paymentMethod }
      val resolvedPaidByUid = existing.paidByUid.ifBlank { paidByUid.ifBlank { auth.currentUser?.uid ?: existing.uid } }
      val updated = existing.copy(
        amountPaid = existing.amountDue,
        status = SubscriptionStatus.PAID,
        paidDate = today,
        paidByUid = resolvedPaidByUid,
        paidByName = payerDisplay,
        paymentMethod = resolvedMethod,
        receiptNumber = receipt
      )

      try {
        docRef.update(
          mapOf(
            "amountPaid" to updated.amountPaid,
            "status" to SubscriptionStatus.PAID.name,
            "paidDate" to today,
            "paidByUid" to updated.paidByUid,
            "paidByName" to payerDisplay,
            "paymentMethod" to resolvedMethod,
            "receiptNumber" to receipt,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        if (updated.familyId.isNotBlank()) {
          runCatching {
            FamilyRepository(db).updateSharedSubscriptionStatus(
              familyId = updated.familyId,
              status = SubscriptionStatus.PAID,
              subscriptionId = updated.subscriptionId,
              monthYear = updated.monthYear,
              paidDate = today,
              paidByUid = updated.paidByUid,
              paidByName = payerDisplay,
              monthlyDuesAmount = updated.amountDue
            )
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }

      return@runCatching updated
    }

    val idx = defaultSubscriptions.indexOfFirst { it.subscriptionId == subscriptionId }
    if (idx != -1) {
      val existing = defaultSubscriptions[idx]
      if (existing.status == SubscriptionStatus.PAID) {
        return@runCatching existing
      }
      val receipt = ChurchIdGenerator.nextSubscriptionReceiptId(db, existing.churchId.ifBlank { "church_main" })
      val payerDisplay = existing.paidByName.ifBlank { paidByName.ifBlank { existing.memberName } }
      val updated = existing.copy(
        amountPaid = existing.amountDue,
        status = SubscriptionStatus.PAID,
        paidDate = today,
        paidByUid = existing.paidByUid.ifBlank { paidByUid.ifBlank { existing.uid } },
        paidByName = payerDisplay,
        paymentMethod = existing.paymentMethod.ifBlank { paymentMethod },
        receiptNumber = receipt
      )
      defaultSubscriptions[idx] = updated
      _subscriptionsFlow.value = defaultSubscriptions.toList()
      updated
    } else {
      throw IllegalStateException("Subscription not found")
    }
  }

  /**
   * Pastor or Super Admin rejects an unverified payment claim and returns the bill to PENDING.
   */
  suspend fun rejectSubscriptionPayment(subscriptionId: String): Result<MonthlySubscription> = runCatching {
    if (auth.currentUser != null) {
      val docRef = db.collection("subscriptions").document(subscriptionId)
      val snap = docRef.get().await()
      val existing = snap.toObject(MonthlySubscription::class.java)
        ?: throw IllegalStateException("Subscription not found")
      val updated = existing.copy(
        amountPaid = 0.0,
        status = SubscriptionStatus.PENDING,
        paidDate = "",
        paidByUid = "",
        paidByName = "",
        paymentMethod = "",
        transactionReference = "",
        receiptNumber = ""
      )
      docRef.update(
        mapOf(
          "amountPaid" to 0.0,
          "status" to SubscriptionStatus.PENDING.name,
          "paidDate" to "",
          "paidByUid" to "",
          "paidByName" to "",
          "paymentMethod" to "",
          "transactionReference" to "",
          "receiptNumber" to "",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      if (updated.familyId.isNotBlank()) {
        runCatching {
          FamilyRepository(db).updateSharedSubscriptionStatus(
            familyId = updated.familyId,
            status = SubscriptionStatus.PENDING,
            subscriptionId = updated.subscriptionId,
            monthYear = updated.monthYear,
            monthlyDuesAmount = updated.amountDue
          )
        }
      }
      return@runCatching updated
    }

    val idx = defaultSubscriptions.indexOfFirst { it.subscriptionId == subscriptionId }
    if (idx != -1) {
      val updated = defaultSubscriptions[idx].copy(
        amountPaid = 0.0,
        status = SubscriptionStatus.PENDING,
        paidDate = "",
        paidByUid = "",
        paidByName = "",
        paymentMethod = "",
        transactionReference = "",
        receiptNumber = ""
      )
      defaultSubscriptions[idx] = updated
      _subscriptionsFlow.value = defaultSubscriptions.toList()
      updated
    } else {
      throw IllegalStateException("Subscription not found")
    }
  }

  /**
   * Believer submits an offering transfer/handover request.
   * Created as PENDING until verified by a Pastor or Super Admin.
   */
  suspend fun submitOffering(
    uid: String,
    donorName: String,
    membershipId: String,
    category: OfferingCategory,
    amount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    transactionReference: String = "",
    churchId: String = "church_main"
  ): Result<OfferingTransaction> = runCatching {
    require(amount > 0.0) { "Offering amount must be greater than zero." }
    val cleanRef = transactionReference.trim()
    if (paymentMethod != PaymentMethod.CASH_OFFLINE) {
      require(cleanRef.length >= 6) {
        "Enter your UPI / Bank Transaction Reference ID (minimum 6 characters)."
      }
    }

    val effectiveUid = auth.currentUser?.uid ?: uid
    val trackingId = ChurchIdGenerator.nextOfferingRequestId(db, churchId, category)
    val tx = OfferingTransaction(
      transactionId = UUID.randomUUID().toString(),
      churchId = churchId,
      uid = effectiveUid,
      donorName = donorName,
      membershipId = membershipId,
      category = category,
      amount = amount,
      paymentMethod = paymentMethod,
      status = PaymentTransactionStatus.PENDING,
      transactionDate = todayLabel(),
      note = note,
      transactionReference = cleanRef.ifBlank { "CASH_AT_OFFICE" },
      receiptNumber = trackingId
    )
    defaultOfferings.add(0, tx)
    _offeringsFlow.value = defaultOfferings.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("offerings").document(tx.transactionId)
      val payload = hashMapOf<String, Any?>(
        "transactionId" to tx.transactionId,
        "churchId" to churchId,
        "uid" to effectiveUid,
        "donorName" to donorName,
        "membershipId" to membershipId,
        "category" to category.name,
        "amount" to amount,
        "paymentMethod" to paymentMethod.name,
        "status" to PaymentTransactionStatus.PENDING.name,
        "transactionDate" to tx.transactionDate,
        "note" to note,
        "transactionReference" to tx.transactionReference,
        "receiptNumber" to trackingId,
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
    tx
  }

  /**
   * Pastor or Super Admin verifies (SUCCESS) or rejects (FAILED) a believer's offering submission.
   */
  suspend fun verifyOfferingTransaction(
    transactionId: String,
    approve: Boolean
  ): Result<OfferingTransaction> = runCatching {
    val nextStatus = if (approve) PaymentTransactionStatus.SUCCESS else PaymentTransactionStatus.FAILED
    if (auth.currentUser != null) {
      val docRef = db.collection("offerings").document(transactionId)
      val snap = docRef.get().await()
      val existing = snap.toObject(OfferingTransaction::class.java)
        ?: throw IllegalStateException("Offering transaction not found")
      val verifiedReceipt = if (approve) {
        ChurchIdGenerator.nextOfferingReceiptId(db, existing.churchId.ifBlank { "church_main" }, existing.category)
      } else {
        existing.receiptNumber
      }
      docRef.update(
        mapOf(
          "status" to nextStatus.name,
          "receiptNumber" to verifiedReceipt,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      return@runCatching existing.copy(status = nextStatus, receiptNumber = verifiedReceipt)
    }

    val idx = defaultOfferings.indexOfFirst { it.transactionId == transactionId }
    if (idx != -1) {
      val existing = defaultOfferings[idx]
      val verifiedReceipt = if (approve) {
        ChurchIdGenerator.nextOfferingReceiptId(db, existing.churchId.ifBlank { "church_main" }, existing.category)
      } else {
        existing.receiptNumber
      }
      val updated = existing.copy(status = nextStatus, receiptNumber = verifiedReceipt)
      defaultOfferings[idx] = updated
      _offeringsFlow.value = defaultOfferings.toList()
      updated
    } else {
      throw IllegalStateException("Offering transaction not found")
    }
  }
}
