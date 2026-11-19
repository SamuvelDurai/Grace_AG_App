package com.example.presentation.announcement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.core.model.AnnouncementCategory
import com.example.data.announcement.AnnouncementRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishAnnouncementDialog(
  announcementRepository: AnnouncementRepository,
  pastorName: String,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var titleEn by remember { mutableStateOf("") }
  var titleTa by remember { mutableStateOf("") }
  var bodyEn by remember { mutableStateOf("") }
  var bodyTa by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(AnnouncementCategory.SUNDAY_SERVICE) }
  var isExpanded by remember { mutableStateOf(false) }
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
    cursorColor = ClassicChurchBlue,
    focusedTrailingIconColor = ClassicChurchBlue,
    unfocusedTrailingIconColor = TextSlate
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = "Publish Church Notice",
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
        ExposedDropdownMenuBox(
          expanded = isExpanded,
          onExpandedChange = { isExpanded = it }
        ) {
          OutlinedTextField(
            value = selectedCategory.name.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category", color = TextSlate) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )
          ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(SurfaceWhite)
          ) {
            AnnouncementCategory.values().forEach { cat ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = cat.name.replace('_', ' '),
                    color = TextDeepNavy,
                    fontWeight = FontWeight.Medium
                  )
                },
                onClick = {
                  selectedCategory = cat
                  isExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = titleEn,
          onValueChange = { titleEn = it },
          label = { Text("Title (English)", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = titleTa,
          onValueChange = { titleTa = it },
          label = { Text("Title (தமிழ்)", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = bodyEn,
          onValueChange = { bodyEn = it },
          label = { Text("Details (English)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = bodyTa,
          onValueChange = { bodyTa = it },
          label = { Text("Details (தமிழ்)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (titleEn.isNotBlank() || titleTa.isNotBlank()) {
            isSubmitting = true
            coroutineScope.launch {
              announcementRepository.publishAnnouncement(
                titleEn = titleEn.ifBlank { titleTa },
                titleTa = titleTa.ifBlank { titleEn },
                bodyEn = bodyEn.ifBlank { bodyTa },
                bodyTa = bodyTa.ifBlank { bodyEn },
                category = selectedCategory,
                author = pastorName
              )
              isSubmitting = false
              onDismiss()
            }
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        enabled = !isSubmitting && (titleEn.isNotBlank() || titleTa.isNotBlank())
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
        )
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
