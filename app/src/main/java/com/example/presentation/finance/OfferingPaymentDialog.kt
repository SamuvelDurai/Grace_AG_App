package com.example.presentation.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.model.ChurchConfig
import com.example.core.model.OfferingCategory
import com.example.core.model.OfferingTransaction
import com.example.core.model.PaymentMethod
import com.example.core.util.launchUpiPayment
import com.example.data.finance.FinanceRepository
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftAmberContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingPaymentDialog(
  uid: String,
  donorName: String,
  membershipId: String,
  financeRepository: FinanceRepository,
  onDismiss: () -> Unit,
  churchConfig: ChurchConfig = ChurchConfig()
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var selectedCategory by remember { mutableStateOf(OfferingCategory.TITHE) }
  var amountText by remember { mutableStateOf("1000") }
  var noteText by remember { mutableStateOf("") }
  var referenceText by remember { mutableStateOf("") }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI_ONLINE) }
  var isProcessing by remember { mutableStateOf(false) }
  var paymentError by remember { mutableStateOf<String?>(null) }
  var completedTransaction by remember { mutableStateOf<OfferingTransaction?>(null) }

  if (completedTransaction != null) {
    val tx = completedTransaction!!
    AlertDialog(
      onDismissRequest = {
        completedTransaction = null
        onDismiss()
      },
      containerColor = SurfaceWhite,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Offering Submitted",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftAmberContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Pending Pastor / Admin Verification — Official receipt is issued once payment is verified.",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentAmber
              ),
              modifier = Modifier.padding(10.dp),
              textAlign = TextAlign.Center
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Tracking ID: ${tx.receiptNumber}", fontWeight = FontWeight.Bold, color = ClassicChurchBlue)
          Text("Donor: ${tx.donorName} (${tx.membershipId})", color = TextDeepNavy)
          Text("Category: ${tx.category.name.replace('_', ' ')}", color = TextSlate)
          Text("Amount: ₹${tx.amount.toInt()}", fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 18.sp)
          Text("Date: ${tx.transactionDate}", color = TextSlate)
          Text("Method: ${tx.paymentMethod.name.replace('_', ' ')}", color = TextSlate)
          if (tx.transactionReference.isNotBlank()) {
            Text("Reference: ${tx.transactionReference}", fontWeight = FontWeight.SemiBold, color = TextDeepNavy)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            completedTransaction = null
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Done", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      }
    )
    return
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
        .testTag("offering_payment_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.VolunteerActivism,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Give Tithes & Offerings",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = churchConfig.name.ifBlank { "GRACE AG" },
                style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSlate)
          }
        }

        HorizontalDivider(color = SoftBlueBorder)

        Text("Select Offering Purpose", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OfferingCategory.values().forEach { cat ->
            val isSelected = selectedCategory == cat
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) SoftBlueContainer else SurfaceWhite,
              border = BorderStroke(1.dp, if (isSelected) ClassicChurchBlue else SoftBlueBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedCategory = cat }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = cat.name.replace('_', ' '),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ClassicChurchBlue else TextDeepNavy
                  )
                )
                if (isSelected) {
                  Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ClassicChurchBlue, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        Text("Offering Amount (₹)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { char -> char.isDigit() }; paymentError = null },
          modifier = Modifier.fillMaxWidth().testTag("offering_amount_input"),
          shape = RoundedCornerShape(12.dp),
          prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = ClassicChurchBlue) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClassicChurchBlue,
            unfocusedBorderColor = SoftBlueBorder,
            focusedTextColor = TextDeepNavy,
            unfocusedTextColor = TextDeepNavy,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor = ClassicChurchBlue
          )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("500", "1000", "2500", "5000").forEach { preset ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (amountText == preset) ClassicChurchBlue else SoftBlueContainer,
              modifier = Modifier.clickable { amountText = preset; paymentError = null }
            ) {
              Text(
                text = "₹$preset",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = if (amountText == preset) TextWhite else ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Text("Payment Method", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            PaymentMethod.UPI_ONLINE to "UPI",
            PaymentMethod.CARD_NETBANKING to "Bank / NEFT",
            PaymentMethod.CASH_OFFLINE to "Cash"
          ).forEach { (method, label) ->
            val isSelected = selectedMethod == method
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) ClassicChurchBlue else SoftBlueContainer,
              border = BorderStroke(1.dp, if (isSelected) ClassicChurchBlue else SoftBlueBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedMethod = method; paymentError = null }
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                  color = if (isSelected) TextWhite else ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              )
            }
          }
        }

        // Real payment instructions & verification notice
        val amountNumeric = amountText.toDoubleOrNull() ?: 0.0
        when (selectedMethod) {
          PaymentMethod.UPI_ONLINE -> {
            if (churchConfig.upiId.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftBlueContainer,
                border = BorderStroke(1.dp, SoftBlueBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "Church UPI ID: ${churchConfig.upiId}",
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy,
                    style = MaterialTheme.typography.bodySmall
                  )
                  OutlinedButton(
                    onClick = {
                      context.launchUpiPayment(
                        upiId = churchConfig.upiId,
                        payeeName = churchConfig.name.ifBlank { "GRACE AG" },
                        amountInr = amountNumeric.coerceAtLeast(1.0),
                        transactionNote = "${selectedCategory.name.replace('_', ' ')} - $membershipId"
                      )
                    },
                    enabled = amountNumeric > 0.0,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClassicChurchBlue),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ClassicChurchBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open UPI App to Pay ₹${amountNumeric.toInt()}", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                  Text(
                    text = "After completing the UPI transfer, enter your 12-digit UTR / Transaction ID below for Pastor verification.",
                    color = TextSlate,
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftAmberContainer,
                border = BorderStroke(1.dp, AccentAmber),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Church UPI ID is not configured yet by Admin. Pay Cash at Church Office, or enter a valid UPI transaction reference if you transferred directly.",
                  color = AccentAmber,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }
          }
          PaymentMethod.CARD_NETBANKING -> {
            if (churchConfig.bankAccountDetails.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftBlueContainer,
                border = BorderStroke(1.dp, SoftBlueBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Bank Details:\n${churchConfig.bankAccountDetails}\n\nEnter your NEFT/IMPS Reference ID below after transferring.",
                  color = TextDeepNavy,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            } else {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftAmberContainer,
                border = BorderStroke(1.dp, AccentAmber),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Online Bank / Card gateway is not added yet. Select Cash to request verification at church office, or enter a Bank Reference ID if transferred.",
                  color = AccentAmber,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }
          }
          PaymentMethod.CASH_OFFLINE -> {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CleanBackground,
              border = BorderStroke(1.dp, SoftBlueBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Submit this cash offering request and hand over ₹${amountNumeric.toInt()} at the church office / offering counter. Status stays Pending until verified by Pastor.",
                color = TextSlate,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }

        if (selectedMethod != PaymentMethod.CASH_OFFLINE) {
          OutlinedTextField(
            value = referenceText,
            onValueChange = { referenceText = it; paymentError = null },
            label = { Text("UPI / Bank Transaction Reference ID *", color = TextSlate) },
            placeholder = { Text("e.g. 432198765432", color = TextSlate.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth().testTag("offering_reference_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ClassicChurchBlue,
              unfocusedBorderColor = SoftBlueBorder,
              focusedTextColor = TextDeepNavy,
              unfocusedTextColor = TextDeepNavy,
              focusedLabelColor = ClassicChurchBlue,
              unfocusedLabelColor = TextSlate,
              focusedContainerColor = SurfaceWhite,
              unfocusedContainerColor = SurfaceWhite,
              cursorColor = ClassicChurchBlue
            )
          )
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Optional Note / Prayer Dedication", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClassicChurchBlue,
            unfocusedBorderColor = SoftBlueBorder,
            focusedTextColor = TextDeepNavy,
            unfocusedTextColor = TextDeepNavy,
            focusedLabelColor = ClassicChurchBlue,
            unfocusedLabelColor = TextSlate,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor = ClassicChurchBlue
          )
        )

        if (!paymentError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ErrorRedContainer,
            border = BorderStroke(1.dp, ErrorRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = paymentError!!,
              color = ErrorRed,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        val buttonText = when {
          isProcessing -> "Submitting..."
          selectedMethod == PaymentMethod.CASH_OFFLINE -> "Submit Cash Offering (₹${amountText.ifBlank { "0" }})"
          else -> "Submit Payment Proof (₹${amountText.ifBlank { "0" }})"
        }

        Button(
          onClick = {
            paymentError = null
            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            if (amountVal <= 0.0) {
              paymentError = "Enter a valid offering amount."
              return@Button
            }
            if (selectedMethod != PaymentMethod.CASH_OFFLINE && referenceText.trim().length < 6) {
              paymentError = "Please transfer the amount first and enter your Transaction / UTR Reference ID (min 6 characters)."
              return@Button
            }
            isProcessing = true
            coroutineScope.launch {
              val res = financeRepository.submitOffering(
                uid = uid,
                donorName = donorName,
                membershipId = membershipId,
                category = selectedCategory,
                amount = amountVal,
                paymentMethod = selectedMethod,
                note = noteText,
                transactionReference = referenceText,
                churchId = churchConfig.churchId.ifBlank { "church_main" }
              )
              isProcessing = false
              res.onSuccess { tx ->
                completedTransaction = tx
              }.onFailure { err ->
                paymentError = err.localizedMessage ?: "Failed to submit offering. Please try again."
              }
            }
          },
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_offering_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
          enabled = !isProcessing && (amountText.toDoubleOrNull() ?: 0.0) > 0.0
        ) {
          Text(
            text = buttonText,
            fontWeight = FontWeight.Bold,
            color = TextWhite
          )
        }
      }
    }
  }
}
