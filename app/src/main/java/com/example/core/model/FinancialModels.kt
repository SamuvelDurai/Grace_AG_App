package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

enum class SubscriptionStatus {
  PAID,
  PENDING,
  PENDING_VERIFICATION,
  OVERDUE,
  WAIVED
}

enum class OfferingCategory {
  TITHE,
  GENERAL_OFFERING,
  MISSIONARY_SUPPORT,
  BUILDING_FUND,
  CHARITY_WELFARE,
  SPECIAL_THANKSGIVING
}

enum class PaymentMethod {
  UPI_ONLINE,
  CARD_NETBANKING,
  CASH_OFFLINE
}

enum class PaymentTransactionStatus {
  SUCCESS,
  PENDING,
  FAILED
}

@Keep
@IgnoreExtraProperties
data class MonthlySubscription(
  val subscriptionId: String = "",
  val churchId: String = "church_main",
  val uid: String = "",
  val familyId: String = "",
  val familyName: String = "",
  val familyMemberCount: Int = 1,
  val memberName: String = "",
  val membershipId: String = "",
  val monthYear: String = "", // e.g. "November 2026"
  val amountDue: Double = 500.0,
  val amountPaid: Double = 0.0,
  val status: SubscriptionStatus = SubscriptionStatus.PENDING,
  val dueDate: String = "",
  val paidDate: String = "",
  val paidByUid: String = "",
  val paidByName: String = "",
  val paymentMethod: String = "",
  val transactionReference: String = "",
  val receiptNumber: String = ""
)

@Keep
@IgnoreExtraProperties
data class OfferingTransaction(
  val transactionId: String = "",
  val churchId: String = "church_main",
  val uid: String = "",
  val donorName: String = "",
  val membershipId: String = "",
  val category: OfferingCategory = OfferingCategory.TITHE,
  val amount: Double = 0.0,
  val paymentMethod: PaymentMethod = PaymentMethod.UPI_ONLINE,
  val status: PaymentTransactionStatus = PaymentTransactionStatus.PENDING,
  val transactionDate: String = "",
  val note: String = "",
  val transactionReference: String = "",
  val receiptNumber: String = ""
)
