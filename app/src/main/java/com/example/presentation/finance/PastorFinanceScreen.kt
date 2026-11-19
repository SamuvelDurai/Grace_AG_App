package com.example.presentation.finance

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ChurchConfig
import com.example.core.model.PaymentTransactionStatus
import com.example.core.model.SubscriptionStatus
import com.example.core.util.CsvExportHelper
import com.example.data.admin.ChurchAdminRepository
import com.example.data.finance.FinanceRepository
import com.example.data.member.MemberRepository
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftAmberContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PastorFinanceScreen(
  financeRepository: FinanceRepository,
  memberRepository: MemberRepository,
  churchAdminRepository: ChurchAdminRepository,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val subscriptions by financeRepository.observeSubscriptions().collectAsState(initial = emptyList())
  val offerings by financeRepository.observeOfferings().collectAsState(initial = emptyList())
  val members by memberRepository.observeAllMembers().collectAsState(initial = emptyList())
  val churchConfig by churchAdminRepository.observeChurchConfig().collectAsState(initial = ChurchConfig())
  var selectedTab by remember { mutableIntStateOf(0) }
  var isGeneratingDues by remember { mutableStateOf(false) }

  val verifiedOfferings = offerings.filter { it.status == PaymentTransactionStatus.SUCCESS }
  val totalOfferings = verifiedOfferings.sumOf { it.amount }
  val totalSubscriptionsCollected = subscriptions
    .filter { it.status == SubscriptionStatus.PAID }
    .sumOf { it.amountPaid }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Financial Collections & Dues",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          ),
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
          onClick = {
            val csv = CsvExportHelper.buildFinanceCsv(offerings, subscriptions)
            CsvExportHelper.shareCsv(context, "Church Finance Report (CSV)", csv)
          },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ClassicChurchBlue
          ),
          modifier = Modifier
            .height(48.dp)
            .testTag("export_finance_csv_button")
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export CSV", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    // Metric Summary Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Verified Offerings", style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹${totalOfferings.toInt()}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
            val pendingOffersCount = offerings.count { it.status == PaymentTransactionStatus.PENDING }
            Text("${verifiedOfferings.size} verified • $pendingOffersCount pending", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
        }

        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Family Subscriptions", style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹${totalSubscriptionsCollected.toInt()}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
            val paidCount = subscriptions.count { it.status == SubscriptionStatus.PAID }
            Text("$paidCount/${subscriptions.size} Families Paid", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
        }
      }
    }

    item {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = SurfaceWhite,
        contentColor = ClassicChurchBlue,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = ClassicChurchBlue,
            height = 3.dp
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              "Voluntary Offerings",
              fontWeight = FontWeight.Bold,
              color = if (selectedTab == 0) ClassicChurchBlue else TextSlate
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              "Monthly Dues",
              fontWeight = FontWeight.Bold,
              color = if (selectedTab == 1) ClassicChurchBlue else TextSlate
            )
          }
        )
      }
    }

    if (selectedTab == 0) {
      if (offerings.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, SoftBlueBorder)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("No voluntary offerings recorded yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
              Spacer(modifier = Modifier.height(4.dp))
              Text("Member tithes and offerings submitted in the app will appear here.", color = TextSlate, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
      items(offerings, key = { it.transactionId }) { tx ->
        val (badgeBg, badgeFg, badgeLabel) = when (tx.status) {
          PaymentTransactionStatus.SUCCESS -> Triple(SuccessGreenContainer, SuccessGreen, "Verified")
          PaymentTransactionStatus.PENDING -> Triple(SoftAmberContainer, AccentAmber, "Pending Verification")
          PaymentTransactionStatus.FAILED -> Triple(ErrorRedContainer, ErrorRed, "Rejected")
        }
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(tx.donorName, fontWeight = FontWeight.Bold, color = TextDeepNavy)
                Text("Category: ${tx.category.name.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall.copy(color = ClassicChurchBlue))
                val refPart = if (tx.transactionReference.isNotBlank()) " • Ref: ${tx.transactionReference}" else ""
                Text("${tx.paymentMethod.name.replace('_', ' ')}$refPart", style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
                Text("ID: ${tx.receiptNumber} • ${tx.transactionDate}", style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("₹${tx.amount.toInt()}", fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 16.sp)
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = badgeBg
                ) {
                  Text(badgeLabel, style = MaterialTheme.typography.labelSmall.copy(color = badgeFg, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }
            }
            if (tx.status == PaymentTransactionStatus.PENDING) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      financeRepository.verifyOfferingTransaction(tx.transactionId, approve = true)
                        .onSuccess { Toast.makeText(context, "Offering verified (${it.receiptNumber})", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.localizedMessage ?: "Verification failed", Toast.LENGTH_LONG).show() }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Verify Paid", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                  onClick = {
                    coroutineScope.launch {
                      financeRepository.verifyOfferingTransaction(tx.transactionId, approve = false)
                        .onSuccess { Toast.makeText(context, "Offering marked rejected", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.localizedMessage ?: "Action failed", Toast.LENGTH_LONG).show() }
                    }
                  },
                  border = BorderStroke(1.dp, ErrorRed),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    } else {
      item {
        Button(
          onClick = {
            isGeneratingDues = true
            coroutineScope.launch {
              val res = financeRepository.generateMonthlyFamilySubscriptions(
                members = members,
                amountDue = churchConfig.monthlyDuesAmount.coerceAtLeast(100.0)
              )
              isGeneratingDues = false
              res.onSuccess { createdCount ->
                val msg = if (createdCount > 0) {
                  "Generated $createdCount family subscription bill(s) for ${financeRepository.currentMonthYearLabel()}"
                } else {
                  "All active families already have a bill for ${financeRepository.currentMonthYearLabel()}"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
              }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to generate family dues", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isGeneratingDues && members.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("generate_family_dues_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = TextWhite,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isGeneratingDues) {
              "Generating Family Bills..."
            } else {
              "Generate Family Dues (₹${churchConfig.monthlyDuesAmount.toInt()} / Family)"
            },
            color = TextWhite,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (subscriptions.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, SoftBlueBorder)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("No family subscription dues yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap 'Generate Family Dues' above to issue 1 monthly bill per family household.",
                color = TextSlate,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }
      items(subscriptions, key = { it.subscriptionId }) { sub ->
        val isPaid = sub.status == SubscriptionStatus.PAID
        val isPendingVerification = sub.status == SubscriptionStatus.PENDING_VERIFICATION
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, if (isPendingVerification) AccentAmber else SoftBlueBorder)
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = sub.familyName.ifBlank { sub.memberName },
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                  Surface(shape = RoundedCornerShape(6.dp), color = SoftBlueContainer) {
                    Text(
                      text = "${sub.familyMemberCount.coerceAtLeast(1)} member(s)",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = ClassicChurchBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text("Head: ${sub.memberName} (${sub.membershipId}) • ${sub.monthYear}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
                if (sub.familyId.isNotBlank()) {
                  Text("Family ID: ${sub.familyId}", style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue))
                }
                when {
                  isPaid -> {
                    val payerSuffix = if (sub.paidByName.isNotBlank()) " by ${sub.paidByName}" else ""
                    Text("Verified Paid on ${sub.paidDate}$payerSuffix", style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen))
                  }
                  isPendingVerification -> {
                    val payerLabel = sub.paidByName.ifBlank { sub.memberName }
                    val methodLabel = sub.paymentMethod.ifBlank { "UPI" }.replace('_', ' ')
                    val refLabel = sub.transactionReference.ifBlank { sub.receiptNumber }
                    Text(
                      text = "Submitted by $payerLabel ($methodLabel) • Ref: $refLabel",
                      style = MaterialTheme.typography.labelSmall.copy(color = AccentAmber, fontWeight = FontWeight.SemiBold)
                    )
                  }
                  else -> {
                    Text("Due: ${sub.dueDate}", style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed))
                  }
                }
              }
              Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("₹${sub.amountDue.toInt()}", fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 16.sp)
                if (isPaid) {
                  Surface(shape = RoundedCornerShape(4.dp), color = SuccessGreenContainer) {
                    Text(
                      text = "PAID",
                      style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                } else if (isPendingVerification) {
                  Surface(shape = RoundedCornerShape(4.dp), color = SoftAmberContainer) {
                    Text(
                      text = "VERIFY",
                      style = MaterialTheme.typography.labelSmall.copy(color = AccentAmber, fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                } else {
                  Button(
                    onClick = {
                      coroutineScope.launch {
                        val res = financeRepository.paySubscription(
                          subscriptionId = sub.subscriptionId,
                          paidByName = "Pastor Office Cash Collection",
                          paymentMethod = "CASH_OFFLINE"
                        )
                        res.onSuccess {
                          Toast.makeText(context, "Marked family bill paid (${it.receiptNumber})", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                          Toast.makeText(context, err.localizedMessage ?: "Failed to update payment", Toast.LENGTH_LONG).show()
                        }
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text("Mark Paid (Cash)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  }
                }
              }
            }
            if (isPendingVerification) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      financeRepository.paySubscription(subscriptionId = sub.subscriptionId)
                        .onSuccess { Toast.makeText(context, "Verified & marked PAID (${it.receiptNumber})", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.localizedMessage ?: "Verification failed", Toast.LENGTH_LONG).show() }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Verify Paid", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                  onClick = {
                    coroutineScope.launch {
                      financeRepository.rejectSubscriptionPayment(sub.subscriptionId)
                        .onSuccess { Toast.makeText(context, "Returned bill to PENDING", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.localizedMessage ?: "Action failed", Toast.LENGTH_LONG).show() }
                    }
                  },
                  border = BorderStroke(1.dp, ErrorRed),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
