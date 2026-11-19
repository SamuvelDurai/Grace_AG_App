package com.example.core.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.core.model.BelieverMember
import com.example.core.model.MonthlySubscription
import com.example.core.model.OfferingTransaction

object CsvExportHelper {

  fun buildMembersCsv(members: List<BelieverMember>): String {
    val sb = StringBuilder()
    sb.appendLine("Membership ID,Full Name,Phone,Email,Gender,DOB,Baptism Status,Ministry Group,Family Name,Family ID,Family Size,Status,Address")
    members.forEach { m ->
      sb.appendLine(
        listOf(
          m.membershipId,
          m.fullName,
          m.phoneNumber,
          m.email,
          m.gender,
          m.dateOfBirth,
          m.baptismStatus,
          m.ministryGroup,
          m.effectiveFamilyName(),
          m.effectiveFamilyId(),
          m.totalFamilyMembersCount().toString(),
          m.membershipStatus,
          m.address
        ).joinToString(",") { escapeCsv(it) }
      )
    }
    return sb.toString()
  }

  fun buildFinanceCsv(
    offerings: List<OfferingTransaction>,
    subscriptions: List<MonthlySubscription>
  ): String {
    val sb = StringBuilder()
    sb.appendLine("=== VOLUNTARY OFFERINGS ===")
    sb.appendLine("Receipt No,Donor Name,Membership ID,Category,Amount (INR),Method,Date,Note")
    offerings.forEach { o ->
      sb.appendLine(
        listOf(
          o.receiptNumber,
          o.donorName,
          o.membershipId,
          o.category.name,
          o.amount.toInt().toString(),
          o.paymentMethod.name,
          o.transactionDate,
          o.note
        ).joinToString(",") { escapeCsv(it) }
      )
    }
    sb.appendLine()
    sb.appendLine("=== FAMILY MONTHLY SUBSCRIPTION DUES ===")
    sb.appendLine("Family Name,Family ID,Members,Head Member,Membership ID,Month,Amount Due,Amount Paid,Status,Due Date,Paid Date,Paid By,Receipt")
    subscriptions.forEach { s ->
      sb.appendLine(
        listOf(
          s.familyName.ifBlank { s.memberName },
          s.familyId,
          s.familyMemberCount.coerceAtLeast(1).toString(),
          s.memberName,
          s.membershipId,
          s.monthYear,
          s.amountDue.toInt().toString(),
          s.amountPaid.toInt().toString(),
          s.status.name,
          s.dueDate,
          s.paidDate,
          s.paidByName.ifBlank { s.memberName },
          s.receiptNumber
        ).joinToString(",") { escapeCsv(it) }
      )
    }
    return sb.toString()
  }

  fun shareCsv(context: Context, subject: String, csvContent: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, csvContent)
    }
    context.launchActivitySafely(
      Intent.createChooser(intent, subject),
      "Unable to open share sheet"
    )
  }

  private fun escapeCsv(value: String): String {
    val sanitized = value.replace("\r", " ").replace("\n", " ")
    return if (sanitized.contains(",") || sanitized.contains("\"")) {
      "\"${sanitized.replace("\"", "\"\"")}\""
    } else {
      sanitized
    }
  }
}
