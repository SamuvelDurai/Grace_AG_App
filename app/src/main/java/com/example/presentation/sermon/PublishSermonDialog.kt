package com.example.presentation.sermon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.sermon.SermonRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PublishSermonDialog(
  sermonRepository: SermonRepository,
  defaultSpeakerName: String,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var titleEn by remember { mutableStateOf("") }
  var titleTa by remember { mutableStateOf("") }
  var speakerName by remember { mutableStateOf(defaultSpeakerName) }
  var scriptureRef by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var videoUrl by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
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

  val canSave = (titleEn.isNotBlank() || titleTa.isNotBlank()) && speakerName.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = "Publish Sermon Message",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = titleEn,
          onValueChange = { titleEn = it },
          label = { Text("Sermon Title (English) *", color = TextSlate) },
          placeholder = { Text("e.g. Walking in Divine Faith", color = TextMuted) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sermon_title_en_input"),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = titleTa,
          onValueChange = { titleTa = it },
          label = { Text("Sermon Title (தமிழ்)", color = TextSlate) },
          placeholder = { Text("உ.ம். விசுவாசத்தில் நடப்போம்", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = speakerName,
          onValueChange = { speakerName = it },
          label = { Text("Preacher / Speaker *", color = TextSlate) },
          placeholder = { Text("e.g. Rev. Dr. Stephen Paul", color = TextMuted) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sermon_speaker_input"),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = scriptureRef,
          onValueChange = { scriptureRef = it },
          label = { Text("Scripture Reference", color = TextSlate) },
          placeholder = { Text("e.g. Hebrews 11:1-6", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Key Points & Sermon Notes", color = TextSlate) },
          placeholder = { Text("1. Main theme\n2. Practical application", color = TextMuted) },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = videoUrl,
          onValueChange = { videoUrl = it },
          label = { Text("Video / Audio Link (Optional)", color = TextSlate) },
          placeholder = { Text("https://...", color = TextMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (canSave && !isSubmitting) {
            isSubmitting = true
            coroutineScope.launch {
              sermonRepository.publishSermon(
                titleEn = titleEn.trim().ifBlank { titleTa.trim() },
                titleTa = titleTa.trim().ifBlank { titleEn.trim() },
                speaker = speakerName.trim(),
                ref = scriptureRef.trim().ifBlank { "Holy Bible" },
                notes = notes.trim(),
                videoUrl = videoUrl.trim()
              )
              isSubmitting = false
              onDismiss()
            }
          }
        },
        enabled = canSave && !isSubmitting,
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("publish_sermon_submit_button")
      ) {
        Text("Publish", color = TextWhite, fontWeight = FontWeight.Bold)
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
        ),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
