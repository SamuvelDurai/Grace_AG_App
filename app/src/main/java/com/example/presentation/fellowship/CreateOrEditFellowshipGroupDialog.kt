package com.example.presentation.fellowship

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.FellowshipCategory
import com.example.core.model.FellowshipGroup
import com.example.data.fellowship.FellowshipRepository
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
fun CreateOrEditFellowshipGroupDialog(
  existingGroup: FellowshipGroup?,
  isTamil: Boolean,
  fellowshipRepository: FellowshipRepository,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val isEdit = existingGroup != null

  var category by remember {
    mutableStateOf(existingGroup?.category ?: FellowshipCategory.AREA_CARE_CELL)
  }
  var nameEn by remember { mutableStateOf(existingGroup?.nameEnglish ?: "") }
  var nameTa by remember { mutableStateOf(existingGroup?.nameTamil ?: "") }
  var areaZone by remember { mutableStateOf(existingGroup?.areaZone ?: "") }
  var leaderName by remember { mutableStateOf(existingGroup?.leaderName ?: "") }
  var leaderPhone by remember { mutableStateOf(existingGroup?.leaderPhone ?: "") }
  var schedule by remember { mutableStateOf(existingGroup?.meetingSchedule ?: "") }
  var location by remember { mutableStateOf(existingGroup?.meetingLocation ?: "") }
  var studyTopicEn by remember { mutableStateOf(existingGroup?.currentStudyTopicEnglish ?: "") }
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
          isEdit && isTamil -> "குழு விவரங்களைத் திருத்து"
          isEdit -> "Edit Fellowship Group"
          isTamil -> "புதிய ஐக்கியம் / குழுவை உருவாக்கு"
          else -> "Create Cell / Youth / Group"
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
          text = if (isTamil) "குழு வகை" else "Group Category",
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
          FellowshipCategory.entries.forEach { cat ->
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
              modifier = Modifier.testTag("category_chip_${cat.name}")
            )
          }
        }

        OutlinedTextField(
          value = nameEn,
          onValueChange = {
            nameEn = it
            errorMsg = null
          },
          label = { Text(if (isTamil) "குழுவின் பெயர் (ஆங்கிலம்)*" else "Group Name (English)*") },
          placeholder = { Text("e.g. Besant Nagar Care Cell", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_name_en_input")
        )

        OutlinedTextField(
          value = nameTa,
          onValueChange = { nameTa = it },
          label = { Text(if (isTamil) "குழுவின் பெயர் (தமிழ்)" else "Group Name (Tamil - Optional)") },
          placeholder = { Text("உ.ம். பெசன்ட் நகர் ஐக்கியம்", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = areaZone,
          onValueChange = { areaZone = it },
          label = { Text(if (isTamil) "பகுதி / மண்டலம்" else "Area / Zone") },
          placeholder = { Text("e.g. Zone 2 • Youth Wing", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = leaderName,
          onValueChange = {
            leaderName = it
            errorMsg = null
          },
          label = { Text(if (isTamil) "தலைவர் பெயர்*" else "Leader Name*") },
          placeholder = { Text("e.g. Bro. John Samuel", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_leader_name_input")
        )

        OutlinedTextField(
          value = leaderPhone,
          onValueChange = { leaderPhone = it },
          label = { Text(if (isTamil) "தலைவர் தொலைபேசி" else "Leader Phone") },
          placeholder = { Text("+91 98765 43210", color = TextMuted) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_leader_phone_input")
        )

        OutlinedTextField(
          value = schedule,
          onValueChange = { schedule = it },
          label = { Text(if (isTamil) "கூட்ட நேரம்" else "Meeting Schedule") },
          placeholder = { Text("Every Friday • 7:00 PM", color = TextMuted) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text(if (isTamil) "இடம் / முகவரி" else "Venue / Location") },
          placeholder = { Text("Church Hall / Member Home", color = TextMuted) },
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = studyTopicEn,
          onValueChange = { studyTopicEn = it },
          label = { Text(if (isTamil) "வேத தியான தலைப்பு" else "Bible Study / Activity Focus") },
          placeholder = { Text("e.g. Discipleship & Prayer", color = TextMuted) },
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth()
        )

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
          if (nameEn.isBlank() || leaderName.isBlank()) {
            errorMsg = if (isTamil) {
              "குழு பெயர் மற்றும் தலைவர் பெயரை உள்ளிடவும்"
            } else {
              "Please enter Group Name and Leader Name"
            }
            return@Button
          }
          coroutineScope.launch {
            val target = FellowshipGroup(
              groupId = existingGroup?.groupId ?: "",
              churchId = existingGroup?.churchId ?: "church_main",
              nameEnglish = nameEn.trim(),
              nameTamil = nameTa.trim().ifBlank { nameEn.trim() },
              areaZone = areaZone.trim().ifBlank { category.displayLabel(isTamil) },
              leaderName = leaderName.trim(),
              leaderPhone = leaderPhone.trim(),
              meetingSchedule = schedule.trim().ifBlank { "Weekly Gathering" },
              meetingLocation = location.trim().ifBlank { "Church Campus" },
              memberCount = existingGroup?.memberCount ?: 0,
              memberIds = existingGroup?.memberIds ?: emptyList(),
              currentStudyTopicEnglish = studyTopicEn.trim().ifBlank { "Fellowship & Word" },
              currentStudyTopicTamil = existingGroup?.currentStudyTopicTamil?.takeIf { it.isNotBlank() }
                ?: studyTopicEn.trim().ifBlank { "ஐக்கியம் & வேத தியானம்" },
              category = category
            )
            if (isEdit) {
              fellowshipRepository.updateGroup(target)
            } else {
              fellowshipRepository.createGroup(target)
            }
            onDismiss()
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        modifier = Modifier.testTag("save_fellowship_group_button")
      ) {
        Text(
          text = when {
            isEdit && isTamil -> "சேமி"
            isEdit -> "Save Changes"
            isTamil -> "உருவாக்கு"
            else -> "Create Group"
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
