package com.example.presentation.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FamilyMemberEntry
import com.example.presentation.common.MemberAvatar
import com.example.ui.theme.ClassicChurchBlue
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
import java.util.UUID

@Composable
fun FamilyMembersSection(
  familyName: String,
  onFamilyNameChange: (String) -> Unit,
  familyMembers: List<FamilyMemberEntry>,
  onAddMember: (FamilyMemberEntry) -> Unit,
  onUpdateMember: (FamilyMemberEntry) -> Unit,
  onRemoveMember: (String) -> Unit,
  modifier: Modifier = Modifier,
  onOpenConnectAccount: (() -> Unit)? = null,
  onUpdateLinkedRole: ((FamilyMemberEntry) -> Unit)? = null
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<FamilyMemberEntry?>(null) }

  if (showAddDialog) {
    AddOrEditFamilyMemberDialog(
      existing = null,
      onDismiss = { showAddDialog = false },
      onSave = { entry ->
        onAddMember(entry)
        showAddDialog = false
      },
      onSwitchToConnectAccount = if (onOpenConnectAccount != null) {
        {
          showAddDialog = false
          onOpenConnectAccount()
        }
      } else null
    )
  }

  if (editingEntry != null) {
    AddOrEditFamilyMemberDialog(
      existing = editingEntry,
      onDismiss = { editingEntry = null },
      onSave = { entry ->
        onUpdateMember(entry)
        editingEntry = null
      }
    )
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = SoftBlueContainer,
      border = BorderStroke(1.dp, SoftBlueBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.FamilyRestroom,
          contentDescription = null,
          tint = ClassicChurchBlue,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "One monthly subscription covers your entire family (${1 + familyMembers.size} member${if (familyMembers.isEmpty()) "" else "s"}).",
          style = MaterialTheme.typography.bodySmall.copy(
            color = ClassicChurchBlue,
            fontWeight = FontWeight.SemiBold
          )
        )
      }
    }

    OutlinedTextField(
      value = familyName,
      onValueChange = onFamilyNameChange,
      label = { Text("Family Name (e.g., Samuel Durai Family)", color = TextSlate) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("family_name_input"),
      singleLine = true,
      colors = editorFieldColors()
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Family Members (${familyMembers.size})",
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        ),
        modifier = Modifier.weight(1f)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (onOpenConnectAccount != null) {
          OutlinedButton(
            onClick = onOpenConnectAccount,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ClassicChurchBlue),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = SoftBlueContainer,
              contentColor = ClassicChurchBlue
            ),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("editor_connect_believer_button")
          ) {
            Icon(
              imageVector = Icons.Default.Link,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Connect", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
        OutlinedButton(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ClassicChurchBlue
          ),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("add_family_member_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Member", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    if (familyMembers.isEmpty()) {
      Text(
        text = "No additional family members added yet. Connect a registered believer account or tap 'Add Member' for spouse, children, or parents.",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
      )
    } else {
      familyMembers.forEach { memberEntry ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SurfaceWhite,
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            MemberAvatar(
              photoUrl = memberEntry.profilePhotoUrl,
              displayName = memberEntry.name,
              size = 38.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = memberEntry.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              val subParts = listOfNotNull(
                memberEntry.relationship.takeIf { it.isNotBlank() },
                memberEntry.gender.takeIf { it.isNotBlank() },
                memberEntry.dateOfBirth.takeIf { it.isNotBlank() }?.let { "DOB: $it" }
              ).joinToString(" • ")
              if (subParts.isNotBlank()) {
                Text(
                  text = subParts,
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                )
              }
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (memberEntry.baptismStatus == "BAPTIZED") SuccessGreenContainer else SoftBlueContainer
                ) {
                  Text(
                    text = memberEntry.baptismStatus.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = if (memberEntry.baptismStatus == "BAPTIZED") SuccessGreen else ClassicChurchBlue,
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                if (memberEntry.linkedUid.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SoftBlueContainer
                  ) {
                    Text(
                      text = memberEntry.membershipId.ifBlank { "Linked Account" },
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = ClassicChurchBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
            IconButton(
              onClick = {
                if (memberEntry.linkedUid.isNotBlank() && onUpdateLinkedRole != null) {
                  onUpdateLinkedRole(memberEntry)
                } else {
                  editingEntry = memberEntry
                }
              },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit ${memberEntry.name}",
                tint = ClassicChurchBlue,
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = { onRemoveMember(memberEntry.id) },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove ${memberEntry.name}",
                tint = ErrorRed,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AddOrEditFamilyMemberDialog(
  existing: FamilyMemberEntry?,
  onDismiss: () -> Unit,
  onSave: (FamilyMemberEntry) -> Unit,
  onSwitchToConnectAccount: (() -> Unit)? = null
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var relationship by remember { mutableStateOf(existing?.relationship?.ifBlank { "Spouse" } ?: "Spouse") }
  var gender by remember { mutableStateOf(existing?.gender ?: "") }
  var dateOfBirth by remember { mutableStateOf(existing?.dateOfBirth ?: "") }
  var baptismStatus by remember { mutableStateOf(existing?.baptismStatus?.ifBlank { "NOT_BAPTIZED" } ?: "NOT_BAPTIZED") }
  var phoneNumber by remember { mutableStateOf(existing?.phoneNumber ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val relationships = remember(existing?.relationship) {
    val base = listOf("Spouse", "Son", "Daughter", "Father", "Mother", "Brother", "Sister", "Other")
    if (!existing?.relationship.isNullOrBlank() && existing!!.relationship !in base) {
      base + existing.relationship
    } else {
      base
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = if (existing == null) "Add Family Member" else "Edit Family Member",
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
        if (onSwitchToConnectAccount != null && existing == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SoftBlueContainer,
              border = BorderStroke(1.dp, SoftBlueBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSwitchToConnectAccount() }
                .testTag("switch_to_connect_account_tab")
            ) {
              Text(
                text = "Connect Account",
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = ClassicChurchBlue,
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "Add Dependent",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
              )
            }
          }
        }
        if (!errorText.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ErrorRedContainer,
            border = BorderStroke(1.dp, ErrorRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorText!!,
              color = ErrorRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it; errorText = null },
          label = { Text("Full Name *", color = TextSlate) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("family_member_name_input"),
          singleLine = true,
          colors = editorFieldColors()
        )

        Text(
          text = "Relationship *",
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
          relationships.forEach { rel ->
            val selected = relationship == rel
            FilterChip(
              selected = selected,
              onClick = { relationship = rel },
              label = {
                Text(
                  text = rel,
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

        Text(
          text = "Gender",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Male", "Female").forEach { opt ->
            val selected = gender == opt
            FilterChip(
              selected = selected,
              onClick = { gender = opt },
              label = {
                Text(
                  text = opt,
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

        OutlinedTextField(
          value = dateOfBirth,
          onValueChange = { dateOfBirth = it },
          label = { Text("Date of Birth (DD/MM/YYYY)", color = TextSlate) },
          placeholder = { Text("15/08/2005", color = TextMuted) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = editorFieldColors()
        )

        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it },
          label = { Text("Mobile Number (Optional)", color = TextSlate) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = editorFieldColors()
        )

        Text(
          text = "Baptism Status",
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
          listOf("BAPTIZED" to "Baptized", "NOT_BAPTIZED" to "Not Yet", "PENDING" to "Requested").forEach { (code, label) ->
            val selected = baptismStatus == code
            FilterChip(
              selected = selected,
              onClick = { baptismStatus = code },
              label = {
                Text(
                  text = label,
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
          if (name.isBlank()) {
            errorText = "Please enter the family member's full name."
            return@Button
          }
          onSave(
            FamilyMemberEntry(
              id = existing?.id?.ifBlank { null } ?: "fm_${UUID.randomUUID().toString().take(8)}",
              name = name.trim(),
              relationship = relationship,
              dateOfBirth = dateOfBirth.trim(),
              gender = gender,
              baptismStatus = baptismStatus,
              phoneNumber = phoneNumber.trim(),
              linkedUid = existing?.linkedUid ?: "",
              membershipId = existing?.membershipId ?: "",
              profilePhotoUrl = existing?.profilePhotoUrl ?: ""
            )
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("save_family_member_button")
      ) {
        Text("Save Member", color = TextWhite, fontWeight = FontWeight.Bold)
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
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
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
