package com.example.presentation.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.BelieverMember
import com.example.core.model.FamilyMemberEntry
import com.example.data.member.MemberRepository
import com.example.presentation.common.MemberAvatar
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

val HOUSEHOLD_RELATIONSHIP_ROLES = listOf(
  "Father",
  "Mother",
  "Spouse",
  "Son",
  "Daughter",
  "Brother",
  "Sister",
  "Other"
)

@Composable
fun ConnectFamilyMemberDialog(
  churchId: String,
  memberRepository: MemberRepository,
  existingLinkedUids: List<String>,
  availableMembersForPicker: List<BelieverMember> = emptyList(),
  allowHeadAssignment: Boolean = true,
  onDismiss: () -> Unit,
  onConnectBeliever: (targetMember: BelieverMember, role: String, makeHead: Boolean) -> Unit,
  onAddManualDependent: ((FamilyMemberEntry) -> Unit)? = null
) {
  val coroutineScope = rememberCoroutineScope()
  var modeTab by remember { mutableIntStateOf(0) } // 0 = Connect Existing Believer, 1 = Manual Dependent

  if (modeTab == 1 && onAddManualDependent != null) {
    AddOrEditFamilyMemberDialog(
      existing = null,
      onDismiss = onDismiss,
      onSave = onAddManualDependent,
      onSwitchToConnectAccount = { modeTab = 0 }
    )
    return
  }

  var searchQuery by remember { mutableStateOf("") }
  var isSearching by remember { mutableStateOf(false) }
  var matchedMember by remember { mutableStateOf<BelieverMember?>(null) }
  var searchError by remember { mutableStateOf<String?>(null) }
  var selectedRole by remember { mutableStateOf("Father") }

  val roleChoices = remember(allowHeadAssignment) {
    if (allowHeadAssignment) listOf("Head") + HOUSEHOLD_RELATIONSHIP_ROLES else HOUSEHOLD_RELATIONSHIP_ROLES
  }

  val directorySuggestions = remember(availableMembersForPicker, searchQuery, existingLinkedUids) {
    if (availableMembersForPicker.isEmpty()) {
      emptyList()
    } else {
      val q = searchQuery.trim()
      availableMembersForPicker
        .filter { candidate ->
          candidate.uid !in existingLinkedUids &&
            (q.isBlank() ||
              candidate.fullName.contains(q, ignoreCase = true) ||
              candidate.membershipId.contains(q, ignoreCase = true) ||
              candidate.phoneNumber.contains(q))
        }
        .take(5)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Connect Church Member",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        Text(
          text = "Link an existing believer account to this household and assign their family role.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (onAddManualDependent != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = ClassicChurchBlue,
              modifier = Modifier
                .weight(1f)
                .clickable { modeTab = 0 }
            ) {
              Text(
                text = "Connect Account",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SoftBlueContainer,
              border = BorderStroke(1.dp, SoftBlueBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { modeTab = 1 }
                .testTag("switch_to_manual_dependent_tab")
            ) {
              Text(
                text = "Add Dependent",
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = {
              searchQuery = it
              searchError = null
            },
            label = {
              Text(
                text = if (availableMembersForPicker.isNotEmpty()) {
                  "Search Name, ID, or Phone"
                } else {
                  "Member ID or 10-digit Phone"
                },
                color = TextSlate,
                fontSize = 12.sp
              )
            },
            placeholder = { Text("e.g. GAG-2026-0002", color = TextMuted) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("connect_member_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextDeepNavy,
              unfocusedTextColor = TextDeepNavy,
              focusedBorderColor = ClassicChurchBlue,
              unfocusedBorderColor = SoftBlueBorder,
              focusedContainerColor = SurfaceWhite,
              unfocusedContainerColor = SurfaceWhite,
              cursorColor = ClassicChurchBlue
            )
          )

          Button(
            onClick = {
              searchError = null
              if (searchQuery.isBlank()) {
                searchError = "Enter a Membership ID or 10-digit phone number."
                return@Button
              }
              isSearching = true
              coroutineScope.launch {
                val res = memberRepository.lookupBelieverByIdOrPhone(searchQuery, churchId)
                isSearching = false
                res.onSuccess { found ->
                  if (found == null) {
                    matchedMember = null
                    searchError = "No believer found with that Membership ID or Phone Number."
                  } else if (found.uid in existingLinkedUids) {
                    matchedMember = found
                    searchError = "${found.fullName} is already connected to this family."
                  } else {
                    matchedMember = found
                  }
                }.onFailure { err ->
                  searchError = err.localizedMessage ?: "Search failed"
                }
              }
            },
            enabled = !isSearching,
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("lookup_believer_button")
          ) {
            if (isSearching) {
              CircularProgressIndicator(
                color = TextWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
            } else {
              Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = "Find Believer",
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        if (!searchError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ErrorRedContainer,
            border = BorderStroke(1.dp, ErrorRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = searchError!!,
              color = ErrorRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (matchedMember == null && directorySuggestions.isNotEmpty()) {
          Text(
            text = "Church Directory (Tap to Select)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ClassicChurchBlue
            )
          )
          directorySuggestions.forEach { candidate ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CleanBackground,
              border = BorderStroke(1.dp, SoftBlueBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  matchedMember = candidate
                  searchError = null
                }
                .testTag("directory_pick_${candidate.uid}")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                MemberAvatar(
                  photoUrl = candidate.profilePhotoUrl,
                  displayName = candidate.fullName,
                  size = 36.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = candidate.fullName,
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy,
                    fontSize = 13.sp
                  )
                  Text(
                    text = "${candidate.membershipId} • ${candidate.phoneNumber}",
                    color = TextSlate,
                    fontSize = 11.sp
                  )
                }
                Icon(
                  imageVector = Icons.Default.Link,
                  contentDescription = "Select",
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        if (matchedMember != null) {
          val target = matchedMember!!
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SuccessGreenContainer,
            border = BorderStroke(1.dp, SuccessGreen),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("matched_believer_card")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MemberAvatar(
                photoUrl = target.profilePhotoUrl,
                displayName = target.fullName,
                size = 44.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = target.fullName,
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy,
                  fontSize = 14.sp
                )
                Text(
                  text = "${target.membershipId} • ${target.phoneNumber}",
                  color = SuccessGreen,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp
                )
              }
            }
          }

          HorizontalDivider(color = SoftBlueBorder)

          Text(
            text = "Assign Family Role *",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            roleChoices.forEach { role ->
              val isSelected = selectedRole == role
              FilterChip(
                selected = isSelected,
                onClick = { selectedRole = role },
                label = {
                  Text(
                    text = role,
                    color = if (isSelected) TextWhite else TextDeepNavy,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ClassicChurchBlue,
                  selectedLabelColor = TextWhite,
                  containerColor = SurfaceWhite,
                  labelColor = TextDeepNavy
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = SoftBlueBorder,
                  selectedBorderColor = ClassicChurchBlue
                ),
                modifier = Modifier.testTag("role_chip_${role.lowercase()}")
              )
            }
          }
        }
      }
    },
    confirmButton = {
      val canConfirm = matchedMember != null && matchedMember!!.uid !in existingLinkedUids
      Button(
        onClick = {
          val target = matchedMember ?: return@Button
          onConnectBeliever(
            target,
            selectedRole,
            selectedRole.equals("Head", ignoreCase = true)
          )
        },
        enabled = canConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("confirm_connect_believer_button")
      ) {
        Text(
          text = if (canConfirm) "Connect as $selectedRole" else "Connect Member",
          color = TextWhite,
          fontWeight = FontWeight.Bold
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        border = BorderStroke(1.dp, SoftBlueBorder),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = TextDeepNavy
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}

@Composable
fun EditFamilyMemberRoleDialog(
  entry: FamilyMemberEntry,
  allowPromoteToHead: Boolean,
  onDismiss: () -> Unit,
  onSaveRole: (newRole: String, promoteToHead: Boolean) -> Unit
) {
  var selectedRole by remember(entry.id) {
    mutableStateOf(entry.relationship.ifBlank { "Family Member" })
  }
  val canBeHead = allowPromoteToHead && entry.linkedUid.isNotBlank()
  val roles = remember(canBeHead) {
    if (canBeHead) listOf("Head") + HOUSEHOLD_RELATIONSHIP_ROLES else HOUSEHOLD_RELATIONSHIP_ROLES
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = "Update Role for ${entry.name}",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (entry.membershipId.isNotBlank()) {
          Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
            Text(
              text = "Connected Account • ${entry.membershipId}",
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        Text(
          text = "Select Household Role",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          roles.forEach { role ->
            val selected = selectedRole.equals(role, ignoreCase = true)
            FilterChip(
              selected = selected,
              onClick = { selectedRole = role },
              leadingIcon = if (role == "Head") {
                {
                  Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (selected) TextWhite else ClassicChurchBlue,
                    modifier = Modifier.size(14.dp)
                  )
                }
              } else null,
              label = {
                Text(
                  text = role,
                  color = if (selected) TextWhite else TextDeepNavy,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ClassicChurchBlue,
                selectedLabelColor = TextWhite,
                containerColor = SurfaceWhite,
                labelColor = TextDeepNavy
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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val promote = canBeHead && selectedRole.equals("Head", ignoreCase = true)
          onSaveRole(selectedRole, promote)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("save_member_role_button")
      ) {
        Text("Update Role", color = TextWhite, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        border = BorderStroke(1.dp, SoftBlueBorder),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = TextDeepNavy
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
