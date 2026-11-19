package com.example.presentation.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.ChurchEvent
import com.example.core.model.EventCategory
import com.example.data.event.ChurchEventRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun CreateOrEditEventDialog(
  existingEvent: ChurchEvent?,
  isTamil: Boolean,
  churchEventRepository: ChurchEventRepository,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val isEdit = existingEvent != null

  var category by remember {
    mutableStateOf(existingEvent?.category ?: EventCategory.SUNDAY_SERVICE)
  }
  var titleEn by remember { mutableStateOf(existingEvent?.titleEnglish ?: "") }
  var titleTa by remember { mutableStateOf(existingEvent?.titleTamil ?: "") }
  var eventDate by remember { mutableStateOf(existingEvent?.eventDate ?: "") }
  var startTime by remember { mutableStateOf(existingEvent?.startTime ?: "") }
  var endTime by remember { mutableStateOf(existingEvent?.endTime ?: "") }
  var venue by remember { mutableStateOf(existingEvent?.venue ?: "") }
  var organizer by remember { mutableStateOf(existingEvent?.organizer ?: "Pastoral & Ministry Team") }
  var capacityText by remember { mutableStateOf((existingEvent?.capacity ?: 200).toString()) }
  var descEn by remember { mutableStateOf(existingEvent?.descriptionEnglish ?: "") }
  var descTa by remember { mutableStateOf(existingEvent?.descriptionTamil ?: "") }
  var isRsvpRequired by remember { mutableStateOf(existingEvent?.isRsvpRequired ?: true) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextDeepNavy,
    unfocusedTextColor = TextDeepNavy,
    focusedBorderColor = ClassicChurchBlue,
    unfocusedBorderColor = SoftBlueBorder,
    focusedLabelColor = ClassicChurchBlue,
    unfocusedLabelColor = TextSlate,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    cursorColor = ClassicChurchBlue
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = when {
          isEdit && isTamil -> "நிகழ்வு விவரங்களைத் திருத்து"
          isEdit -> "Edit Church Event"
          isTamil -> "புதிய சபை நிகழ்வை உருவாக்கு"
          else -> "Create Church Event"
        },
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
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = if (isTamil) "நிகழ்வு வகை" else "Event Category",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ClassicChurchBlue
          )
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          EventCategory.entries.forEach { cat ->
            val selected = category == cat
            FilterChip(
              selected = selected,
              onClick = { category = cat },
              label = {
                Text(
                  text = cat.displayLabel(isTamil),
                  color = if (selected) ClassicChurchBlue else TextDeepNavy,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = SurfaceWhite,
                labelColor = TextDeepNavy,
                selectedContainerColor = SoftBlueContainer,
                selectedLabelColor = ClassicChurchBlue
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = SoftBlueBorder,
                selectedBorderColor = ClassicChurchBlue
              ),
              modifier = Modifier.testTag("event_category_chip_${cat.name}")
            )
          }
        }

        OutlinedTextField(
          value = titleEn,
          onValueChange = {
            titleEn = it
            errorMsg = null
          },
          label = { Text(if (isTamil) "நிகழ்வின் தலைப்பு (ஆங்கிலம்)*" else "Event Title (English)*") },
          placeholder = { Text("e.g. Good Friday Communion Service", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("event_title_en_input")
        )

        OutlinedTextField(
          value = titleTa,
          onValueChange = { titleTa = it },
          label = { Text(if (isTamil) "நிகழ்வின் தலைப்பு (தமிழ்)" else "Event Title (Tamil - Optional)") },
          placeholder = { Text("உ.ம். புனித வெள்ளி ஆராதனை", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = eventDate,
          onValueChange = {
            eventDate = it
            errorMsg = null
          },
          label = { Text(if (isTamil) "தேதி*" else "Event Date*") },
          placeholder = { Text("e.g. Sunday, 15 Dec 2026", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("event_date_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text(if (isTamil) "தொடக்கம்" else "Start Time") },
            placeholder = { Text("09:00 AM", color = TextMuted) },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text(if (isTamil) "முடிவு" else "End Time") },
            placeholder = { Text("12:00 PM", color = TextMuted) },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = venue,
          onValueChange = {
            venue = it
            errorMsg = null
          },
          label = { Text(if (isTamil) "இடம்*" else "Venue / Location*") },
          placeholder = { Text("Main Sanctuary / Fellowship Hall", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("event_venue_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = organizer,
            onValueChange = { organizer = it },
            label = { Text(if (isTamil) "ஒருங்கிணைப்பாளர்" else "Organizer") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(1.4f)
          )
          OutlinedTextField(
            value = capacityText,
            onValueChange = { input ->
              capacityText = input.filter { it.isDigit() }
            },
            label = { Text(if (isTamil) "இருக்கை" else "Capacity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(0.8f)
          )
        }

        OutlinedTextField(
          value = descEn,
          onValueChange = { descEn = it },
          label = { Text(if (isTamil) "விவரம் (ஆங்கிலம்)" else "Description (English)") },
          placeholder = { Text("Program schedule, speaker & fellowship notes", color = TextMuted) },
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = descTa,
          onValueChange = { descTa = it },
          label = { Text(if (isTamil) "விவரம் (தமிழ்)" else "Description (Tamil - Optional)") },
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isTamil) "முன்பதிவு (RSVP) தேவை" else "Require Member RSVP",
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextDeepNavy,
              fontWeight = FontWeight.SemiBold
            )
          )
          Switch(
            checked = isRsvpRequired,
            onCheckedChange = { isRsvpRequired = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = TextWhite,
              checkedTrackColor = ClassicChurchBlue
            )
          )
        }

        if (!errorMsg.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = errorMsg!!,
            style = MaterialTheme.typography.bodySmall.copy(
              color = ErrorRed,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (titleEn.isBlank() || eventDate.isBlank() || venue.isBlank()) {
            errorMsg = if (isTamil) {
              "தலைப்பு, தேதி மற்றும் இடத்தை உள்ளிடவும்"
            } else {
              "Please enter Event Title, Date, and Venue"
            }
            return@Button
          }
          val parsedCapacity = capacityText.toIntOrNull()?.coerceIn(1, 10000) ?: 150
          coroutineScope.launch {
            val target = ChurchEvent(
              eventId = existingEvent?.eventId ?: "",
              churchId = existingEvent?.churchId ?: "church_main",
              titleEnglish = titleEn.trim(),
              titleTamil = titleTa.trim().ifBlank { titleEn.trim() },
              descriptionEnglish = descEn.trim().ifBlank { "Church worship and fellowship gathering." },
              descriptionTamil = descTa.trim().ifBlank {
                descEn.trim().ifBlank { "சபை ஆராதனை மற்றும் ஐக்கிய கூடுகை." }
              },
              eventDate = eventDate.trim(),
              startTime = startTime.trim().ifBlank { "09:00 AM" },
              endTime = endTime.trim().ifBlank { "12:00 PM" },
              venue = venue.trim(),
              isRsvpRequired = isRsvpRequired,
              capacity = parsedCapacity,
              registeredCount = existingEvent?.registeredCount ?: 0,
              attendeeIds = existingEvent?.attendeeIds ?: emptyList(),
              isRegisteredByCurrentUser = existingEvent?.isRegisteredByCurrentUser ?: false,
              organizer = organizer.trim().ifBlank { "Pastoral Ministry Team" },
              category = category
            )
            if (isEdit) {
              churchEventRepository.updateEvent(target)
            } else {
              churchEventRepository.createEvent(target)
            }
            onDismiss()
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        modifier = Modifier.testTag("save_event_button")
      ) {
        Text(
          text = when {
            isEdit && isTamil -> "சேமி"
            isEdit -> "Save Changes"
            isTamil -> "உருவாக்கு"
            else -> "Create Event"
          },
          color = TextWhite,
          fontWeight = FontWeight.Bold
        )
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
        Text(
          text = if (isTamil) "ரத்து" else "Cancel",
          color = TextDeepNavy,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  )
}
