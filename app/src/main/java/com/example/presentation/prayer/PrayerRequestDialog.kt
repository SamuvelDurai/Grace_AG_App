package com.example.presentation.prayer

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.model.PrayerPrivacy
import com.example.data.prayer.PrayerRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerRequestDialog(
  uid: String,
  userName: String,
  prayerRepository: PrayerRepository,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var privacy by remember { mutableStateOf(PrayerPrivacy.PASTOR_ONLY) }
  var isAnonymous by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }
  var submitError by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
        .testTag("prayer_request_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
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
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Submit Prayer Burden",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text("Shared in absolute confidence", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSlate)
          }
        }

        HorizontalDivider(color = SoftBlueBorder)

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Prayer Request Title", color = TextSlate) },
          placeholder = { Text("e.g. Healing, Family Peace, Guidance", color = com.example.ui.theme.TextMuted) },
          modifier = Modifier.fillMaxWidth().testTag("prayer_title_input"),
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

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Describe Your Need / Request", color = TextSlate) },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("prayer_desc_input"),
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

        Text("Privacy Level", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            Pair(PrayerPrivacy.PASTOR_ONLY, "Pastor Only (Highest Confidentiality)"),
            Pair(PrayerPrivacy.PRAYER_INTERCESSORS, "Pastor & Church Prayer Intercessors"),
            Pair(PrayerPrivacy.CHURCH_FAMILY, "Church Congregation Prayer Bulletin")
          ).forEach { (p, label) ->
            val isSelected = privacy == p
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) SoftBlueContainer else SurfaceWhite,
              border = BorderStroke(1.dp, if (isSelected) ClassicChurchBlue else SoftBlueBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { privacy = p }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = if (isSelected) ClassicChurchBlue else TextSlate,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) ClassicChurchBlue else TextDeepNavy,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = isAnonymous,
            onCheckedChange = { isAnonymous = it },
            colors = CheckboxDefaults.colors(checkedColor = ClassicChurchBlue)
          )
          Text("Keep my name anonymous", style = MaterialTheme.typography.bodySmall.copy(color = TextDeepNavy))
        }

        if (!submitError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ErrorRed.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = submitError.orEmpty(),
              style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed, fontWeight = FontWeight.Medium),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Button(
          onClick = {
            if (title.isNotBlank() && !isSubmitting) {
              isSubmitting = true
              submitError = null
              coroutineScope.launch {
                val result = prayerRepository.submitPrayerRequest(
                  uid = uid,
                  name = userName,
                  title = title,
                  description = description,
                  privacy = privacy,
                  isAnonymous = isAnonymous
                )
                isSubmitting = false
                result.onSuccess {
                  onDismiss()
                }.onFailure { err ->
                  submitError = err.localizedMessage ?: "Failed to submit prayer request. Please try again."
                }
              }
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_prayer_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
          enabled = title.isNotBlank() && !isSubmitting
        ) {
          Text(if (isSubmitting) "Sending..." else "Send Prayer Request", fontWeight = FontWeight.Bold, color = TextWhite)
        }
      }
    }
  }
}
