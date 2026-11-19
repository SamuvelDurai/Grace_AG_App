package com.example.presentation.fellowship

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.FellowshipGroup
import com.example.core.util.launchActivitySafely
import com.example.data.fellowship.FellowshipRepository
import com.example.data.member.MemberRepository
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
fun ManageGroupMembersDialog(
  group: FellowshipGroup,
  isTamil: Boolean,
  fellowshipRepository: FellowshipRepository,
  memberRepository: MemberRepository,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val allMembers by memberRepository.observeAllMembers().collectAsState(initial = emptyList())
  var searchQuery by remember { mutableStateOf("") }
  var selectedMemberIds by remember(group.groupId) {
    mutableStateOf(group.memberIds.toSet())
  }

  val filteredMembers = remember(allMembers, searchQuery) {
    val sorted = allMembers.sortedByDescending { it.memberId in selectedMemberIds }
    if (searchQuery.isBlank()) {
      sorted
    } else {
      sorted.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
          it.membershipId.contains(searchQuery, ignoreCase = true) ||
          it.phoneNumber.contains(searchQuery)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Column {
        Text(
          text = if (isTamil) "குழு உறுப்பினர்களை நிர்வகி" else "Manage Group Members",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        Text(
          text = "${if (isTamil) group.nameTamil else group.nameEnglish} • ${selectedMemberIds.size} assigned",
          style = MaterialTheme.typography.labelSmall.copy(
            color = ClassicChurchBlue,
            fontWeight = FontWeight.SemiBold
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = if (isTamil) "பெயர், எண் அல்லது தொலைபேசி..." else "Search member name, ID, phone...",
              color = TextMuted
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = ClassicChurchBlue)
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDeepNavy,
            unfocusedTextColor = TextDeepNavy,
            focusedBorderColor = ClassicChurchBlue,
            unfocusedBorderColor = SoftBlueBorder,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            cursorColor = ClassicChurchBlue
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_group_member_search")
        )

        if (filteredMembers.isEmpty()) {
          Text(
            text = if (isTamil) "உறுப்பினர்கள் இல்லை" else "No matching believers found.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate),
            modifier = Modifier.padding(vertical = 12.dp)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredMembers, key = { it.memberId }) { member ->
              val isChecked = member.memberId in selectedMemberIds
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isChecked) SoftBlueContainer else SurfaceWhite,
                border = BorderStroke(1.dp, if (isChecked) ClassicChurchBlue else SoftBlueBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedMemberIds = if (isChecked) {
                      selectedMemberIds - member.memberId
                    } else {
                      selectedMemberIds + member.memberId
                    }
                  }
                  .testTag("group_member_row_${member.memberId}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                      selectedMemberIds = if (checked) {
                        selectedMemberIds + member.memberId
                      } else {
                        selectedMemberIds - member.memberId
                      }
                    },
                    colors = CheckboxDefaults.colors(
                      checkedColor = ClassicChurchBlue,
                      uncheckedColor = TextSlate,
                      checkmarkColor = TextWhite
                    )
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = member.fullName,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDeepNavy
                      ),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${member.membershipId} • ${member.phoneNumber}",
                      style = MaterialTheme.typography.labelSmall.copy(color = TextSlate),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  if (member.phoneNumber.isNotBlank()) {
                    IconButton(
                      onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                          data = Uri.parse("tel:${member.phoneNumber.replace(" ", "")}")
                        }
                        context.launchActivitySafely(dialIntent, "Dialer unavailable")
                      },
                      modifier = Modifier.size(48.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${member.fullName}",
                        tint = ClassicChurchBlue,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          coroutineScope.launch {
            val delta = selectedMemberIds.size - group.memberIds.size
            val nextCount = (group.memberCount + delta).coerceAtLeast(selectedMemberIds.size)
            val updatedGroup = group.copy(
              memberIds = selectedMemberIds.toList(),
              memberCount = nextCount
            )
            val groupResult = fellowshipRepository.updateGroup(updatedGroup)
            if (groupResult.isFailure) {
              Toast.makeText(
                context,
                groupResult.exceptionOrNull()?.localizedMessage ?: "Failed to update group members",
                Toast.LENGTH_LONG
              ).show()
              return@launch
            }

            // Sync ministryGroup on newly assigned members
            val newlyAdded = selectedMemberIds - group.memberIds.toSet()
            allMembers.filter { it.memberId in newlyAdded }.forEach { member ->
              memberRepository.saveMemberProfile(
                member.copy(ministryGroup = group.nameEnglish)
              )
            }
            Toast.makeText(
              context,
              if (isTamil) "குழு உறுப்பினர்கள் சேமிக்கப்பட்டனர்" else "Group members updated (${selectedMemberIds.size} assigned)",
              Toast.LENGTH_SHORT
            ).show()
            onDismiss()
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        modifier = Modifier.testTag("save_group_members_button")
      ) {
        Text(
          text = if (isTamil) "சேமி" else "Save Members",
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
