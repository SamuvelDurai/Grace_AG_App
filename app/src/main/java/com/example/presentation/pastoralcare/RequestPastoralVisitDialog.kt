package com.example.presentation.pastoralcare

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.core.model.PastoralCareType
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun RequestPastoralVisitDialog(
  uid: String,
  userName: String,
  userPhone: String,
  membershipId: String,
  pastoralCareRepository: PastoralCareRepository,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var selectedType by remember { mutableStateOf(PastoralCareType.HOSPITAL_VISIT) }
  var locationDetails by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("request_pastoral_visit_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      border = BorderStroke(1.5.dp, SoftBlueBorder)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(id = R.string.request_pastoral_visit),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = stringResource(id = R.string.request_visit_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSlate
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(id = R.string.visit_type_prompt),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Care type selection
        val types = listOf(
          Triple(PastoralCareType.HOSPITAL_VISIT, stringResource(id = R.string.hospital_visit_label), Icons.Default.LocalHospital),
          Triple(PastoralCareType.HOME_BLESSING, stringResource(id = R.string.home_blessing_label), Icons.Default.Home),
          Triple(PastoralCareType.BEREAVEMENT_CARE, stringResource(id = R.string.bereavement_label), Icons.Default.VolunteerActivism)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          types.forEach { (type, label, icon) ->
            val isSelected = selectedType == type
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) ClassicChurchBlue else SoftBlueContainer,
              border = BorderStroke(1.dp, if (isSelected) ClassicChurchBlue else SoftBlueBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedType = type }
                .testTag("visit_type_${type.name.lowercase()}")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = if (isSelected) TextWhite else ClassicChurchBlue,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TextWhite else TextDeepNavy
                  ),
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = locationDetails,
          onValueChange = { locationDetails = it },
          label = { Text(stringResource(id = R.string.visit_location_prompt), color = TextSlate) },
          placeholder = {
            Text(
              "e.g. Room 304, Apollo Hospital Greams Road or Street Address",
              color = com.example.ui.theme.TextMuted
            )
          },
          modifier = Modifier.fillMaxWidth().testTag("visit_location_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDeepNavy,
            unfocusedTextColor = TextDeepNavy,
            focusedBorderColor = ClassicChurchBlue,
            unfocusedBorderColor = SoftBlueBorder,
            focusedLabelColor = ClassicChurchBlue,
            unfocusedLabelColor = TextSlate,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor = ClassicChurchBlue
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(stringResource(id = R.string.visit_notes_prompt), color = TextSlate) },
          placeholder = {
            Text(
              "Describe the prayer need, convenient time, or person to visit...",
              color = com.example.ui.theme.TextMuted
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("visit_notes_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDeepNavy,
            unfocusedTextColor = TextDeepNavy,
            focusedBorderColor = ClassicChurchBlue,
            unfocusedBorderColor = SoftBlueBorder,
            focusedLabelColor = ClassicChurchBlue,
            unfocusedLabelColor = TextSlate,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor = ClassicChurchBlue
          ),
          maxLines = 4
        )

        Spacer(modifier = Modifier.height(20.dp))

        val toastMessage = stringResource(id = R.string.request_submitted_toast)

        Button(
          onClick = {
            if (locationDetails.isNotBlank()) {
              isSubmitting = true
              coroutineScope.launch {
                val result = pastoralCareRepository.requestPastoralVisit(
                  memberUid = uid,
                  memberName = userName,
                  memberPhone = userPhone,
                  membershipId = membershipId,
                  careType = selectedType,
                  notes = notes,
                  locationDetails = locationDetails
                )
                isSubmitting = false
                result.fold(
                  onSuccess = {
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    onDismiss()
                  },
                  onFailure = { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Failed to submit visit request.", Toast.LENGTH_LONG).show()
                  }
                )
              }
            }
          },
          enabled = locationDetails.isNotBlank() && !isSubmitting,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_pastoral_visit_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue)
        ) {
          Text(
            text = stringResource(id = R.string.submit_visit_request),
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextWhite
            )
          )
        }
      }
    }
  }
}
