package com.example.presentation.prayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.PrayerRequest
import com.example.core.model.PrayerStatus
import com.example.data.prayer.PrayerRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PastoralPrayerResponseDialog(
  prayerRequest: PrayerRequest,
  prayerRepository: PrayerRepository,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var selectedStatus by remember { mutableStateOf(prayerRequest.status) }
  var pastoralNote by remember { mutableStateOf(prayerRequest.pastoralNote) }
  var isSaving by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = "Pastoral Prayer Response",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("From: ${prayerRequest.requesterName}", fontWeight = FontWeight.Bold, color = TextDeepNavy)
        Text("Title: ${prayerRequest.title}", fontWeight = FontWeight.SemiBold, color = ClassicChurchBlue)
        Text(prayerRequest.description, style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate))
        Spacer(modifier = Modifier.height(4.dp))

        Text("Update Status", fontWeight = FontWeight.Bold, color = TextDeepNavy)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(PrayerStatus.IN_REVIEW, PrayerStatus.PRAYED_FOR, PrayerStatus.CLOSED).forEach { st ->
            val selected = selectedStatus == st
            FilterChip(
              selected = selected,
              onClick = { selectedStatus = st },
              label = {
                Text(
                  text = st.name.replace('_', ' '),
                  fontWeight = FontWeight.Bold,
                  color = if (selected) TextWhite else ClassicChurchBlue
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ClassicChurchBlue,
                selectedLabelColor = TextWhite,
                containerColor = SoftBlueContainer,
                labelColor = ClassicChurchBlue
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = SoftBlueBorder,
                selectedBorderColor = ClassicChurchBlue
              )
            )
          }
        }

        OutlinedTextField(
          value = pastoralNote,
          onValueChange = { pastoralNote = it },
          label = { Text("Private Encouragement / Pastoral Note", color = TextSlate) },
          placeholder = { Text("e.g. Prayed with the intercessory team; holding you in prayer.", color = TextMuted) },
          minLines = 3,
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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          isSaving = true
          coroutineScope.launch {
            prayerRepository.updatePrayerStatus(prayerRequest.requestId, selectedStatus, pastoralNote)
            isSaving = false
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        enabled = !isSaving
      ) {
        Text("Save Response", color = TextWhite, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SoftBlueBorder),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = TextDeepNavy
        )
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
