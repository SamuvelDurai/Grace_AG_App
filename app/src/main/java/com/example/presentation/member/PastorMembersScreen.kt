package com.example.presentation.member

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.BelieverMember
import com.example.core.model.FamilyMemberEntry
import com.example.core.model.MembershipApplication
import com.example.core.model.UserProfile
import com.example.core.util.CsvExportHelper
import com.example.core.util.ProfilePhotoHelper
import com.example.core.util.launchActivitySafely
import com.example.data.admin.ChurchAdminRepository
import com.example.data.member.MemberRepository
import com.example.presentation.common.MemberAvatar
import com.example.presentation.common.MemberPhotoPreviewDialog
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
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PastorMembersScreen(
  memberRepository: MemberRepository,
  currentPastorUid: String,
  churchId: String = "church_main",
  churchAdminRepository: ChurchAdminRepository? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val resolvedChurchId = churchId.ifBlank { "church_main" }
  val members by memberRepository.observeAllMembers(resolvedChurchId).collectAsState(initial = emptyList())
  val managedUsers by remember(churchAdminRepository, resolvedChurchId) {
    churchAdminRepository?.observeManagedUsers(resolvedChurchId) ?: flowOf(emptyList<UserProfile>())
  }.collectAsState(initial = emptyList())
  val allFamilies by memberRepository.familyRepository.observeAllFamilies(resolvedChurchId).collectAsState(initial = emptyList())
  val pendingApplications by memberRepository.observePendingApplications(resolvedChurchId).collectAsState(initial = emptyList())

  val userPhotoByUid = remember(managedUsers) {
    managedUsers.associate { it.uid to it.photoUrl }
  }
  val memberPhotoByUid = remember(members, userPhotoByUid, pendingApplications) {
    val map = mutableMapOf<String, String>()
    managedUsers.forEach { u ->
      val p = ProfilePhotoHelper.resolvePhotoUrl(u.photoUrl)
      if (p.isNotBlank()) map[u.uid] = p
    }
    pendingApplications.forEach { app ->
      val p = ProfilePhotoHelper.resolvePhotoUrl(app.profilePhotoUrl, map[app.uid])
      if (p.isNotBlank()) map[app.uid] = p
    }
    members.forEach { m ->
      val p = ProfilePhotoHelper.resolvePhotoUrl(m.profilePhotoUrl, map[m.uid])
      if (p.isNotBlank()) map[m.uid] = p
    }
    map
  }

  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, BAPTIZED

  var reviewingApplication by remember { mutableStateOf<MembershipApplication?>(null) }
  var selectedMemberDetail by remember { mutableStateOf<BelieverMember?>(null) }
  var photoPreviewTarget by remember { mutableStateOf<BelieverMember?>(null) }
  var isUploadingMemberPhoto by remember { mutableStateOf(false) }
  var rejectionNote by remember { mutableStateOf("") }
  var showRejectDialog by remember { mutableStateOf(false) }

  var isProcessingAppAction by remember { mutableStateOf(false) }

  val pastorPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val target = photoPreviewTarget ?: selectedMemberDetail ?: return@rememberLauncherForActivityResult
    uri?.let { pickedUri ->
      isUploadingMemberPhoto = true
      coroutineScope.launch {
        val compressed = withContext(Dispatchers.IO) {
          ProfilePhotoHelper.encodeUriToCompressedDataUri(context, pickedUri)
        }
        if (compressed != null) {
          val res = memberRepository.updateMemberPhoto(target, compressed)
          isUploadingMemberPhoto = false
          res.onSuccess { savedPhoto ->
            photoPreviewTarget = target.copy(profilePhotoUrl = savedPhoto)
            if (selectedMemberDetail?.uid == target.uid) {
              selectedMemberDetail = selectedMemberDetail?.copy(profilePhotoUrl = savedPhoto)
            }
            Toast.makeText(context, "Updated photo for ${target.fullName}", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Failed to save member photo", Toast.LENGTH_LONG).show()
          }
        } else {
          isUploadingMemberPhoto = false
          Toast.makeText(context, "Unable to read selected image.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  if (photoPreviewTarget != null) {
    val target = photoPreviewTarget!!
    val latestMember = members.firstOrNull { it.uid == target.uid } ?: target
    val resolvedPhoto = ProfilePhotoHelper.resolvePhotoUrl(
      latestMember.profilePhotoUrl,
      target.profilePhotoUrl,
      memberPhotoByUid[target.uid]
    )
    MemberPhotoPreviewDialog(
      photoUrl = resolvedPhoto,
      displayName = latestMember.fullName,
      subtitle = latestMember.membershipId.ifBlank { latestMember.phoneNumber },
      isUploading = isUploadingMemberPhoto,
      onChangePhoto = {
        pastorPhotoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
      onDismiss = { if (!isUploadingMemberPhoto) photoPreviewTarget = null }
    )
  }

  // Application Review Dialog
  if (reviewingApplication != null) {
    val app = reviewingApplication!!
    AlertDialog(
      onDismissRequest = { if (!isProcessingAppAction) reviewingApplication = null },
      containerColor = SurfaceWhite,
      title = {
        val appPhoto = ProfilePhotoHelper.resolvePhotoUrl(app.profilePhotoUrl, memberPhotoByUid[app.uid])
        Row(verticalAlignment = Alignment.CenterVertically) {
          MemberAvatar(
            photoUrl = appPhoto,
            displayName = app.fullName,
            size = 46.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Review Application",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = app.fullName,
              style = MaterialTheme.typography.labelMedium.copy(color = ClassicChurchBlue)
            )
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text("Full Name: ${app.fullName}", fontWeight = FontWeight.Bold, color = TextDeepNavy)
          Text("Phone: ${app.phoneNumber}", color = TextSlate)
          if (app.email.isNotBlank()) Text("Email: ${app.email}", color = TextSlate)
          if (app.dateOfBirth.isNotBlank()) Text("DOB: ${app.dateOfBirth}", color = TextSlate)
          if (app.gender.isNotBlank()) Text("Gender: ${app.gender}", color = TextSlate)
          if (app.address.isNotBlank()) Text("Address: ${app.address}", color = TextSlate)
          Text("Baptism Status: ${app.baptismStatus}", color = ClassicChurchBlue, fontWeight = FontWeight.SemiBold)
          if (app.familyName.isNotBlank() || app.familyMembers.isNotEmpty()) {
            HorizontalDivider(color = SoftBlueBorder, modifier = Modifier.padding(vertical = 4.dp))
            Text(
              text = "Family: ${app.familyName.ifBlank { "${app.fullName} Family" }} (${maxOf(1, app.familyMembersCount)} member(s))",
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold
            )
            app.familyMembers.forEach { fm ->
              Text(
                text = "• ${fm.name} (${fm.relationship}${if (fm.baptismStatus == "BAPTIZED") ", Baptized" else ""})",
                color = TextSlate,
                fontSize = 12.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Approving will generate a unique membership ID and activate the believer's church profile.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isProcessingAppAction = true
            coroutineScope.launch {
              val res = memberRepository.approveApplication(app, currentPastorUid)
              isProcessingAppAction = false
              res.onSuccess {
                Toast.makeText(context, "${app.fullName} approved (${it.membershipId})", Toast.LENGTH_SHORT).show()
                reviewingApplication = null
              }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to approve application", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isProcessingAppAction,
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          )
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TextWhite,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isProcessingAppAction) "Approving..." else "Approve Member", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showRejectDialog = true },
          enabled = !isProcessingAppAction,
          border = BorderStroke(1.dp, ErrorRed),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ErrorRed
          )
        ) {
          Text("Reject", color = ErrorRed, fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // Rejection Reason Dialog
  if (showRejectDialog && reviewingApplication != null) {
    AlertDialog(
      onDismissRequest = { showRejectDialog = false },
      containerColor = SurfaceWhite,
      title = { Text("Reject Application", color = ErrorRed, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Provide a reason for the applicant:", color = TextSlate)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = rejectionNote,
            onValueChange = { rejectionNote = it },
            placeholder = {
              Text(
                "e.g. Please verify address with church office",
                color = com.example.ui.theme.TextMuted
              )
            },
            modifier = Modifier.fillMaxWidth(),
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
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = memberRepository.rejectApplication(
                reviewingApplication!!.applicationId,
                currentPastorUid,
                rejectionNote
              )
              res.onSuccess {
                Toast.makeText(context, "Application rejected", Toast.LENGTH_SHORT).show()
                showRejectDialog = false
                reviewingApplication = null
                rejectionNote = ""
              }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to reject application", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = ErrorRed,
            contentColor = TextWhite
          )
        ) {
          Text("Confirm Rejection", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showRejectDialog = false },
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

  // Member Detail Sheet
  if (selectedMemberDetail != null) {
    val m = members.firstOrNull { it.uid == selectedMemberDetail!!.uid } ?: selectedMemberDetail!!
    val resolvedFamily = allFamilies.firstOrNull { fam ->
      fam.familyId == m.effectiveFamilyId() ||
        m.uid in fam.linkedMemberUids ||
        fam.headUid == m.uid
    }
    val activeFamilyId = resolvedFamily?.familyId?.ifBlank { m.effectiveFamilyId() } ?: m.effectiveFamilyId()
    val activeFamilyName = resolvedFamily?.resolvedFamilyName() ?: m.effectiveFamilyName()
    val householdCount = resolvedFamily?.totalHouseholdCount() ?: m.totalFamilyMembersCount()
    val rosterEntries = remember(resolvedFamily, m.familyMembers) {
      val headId = resolvedFamily?.headUid.orEmpty()
      val list = resolvedFamily?.dependentMembers ?: m.familyMembers
      list.filterNot { it.linkedUid.isNotBlank() && it.linkedUid == headId }
    }

    var editableFamilyId by remember(m.memberId, resolvedFamily?.familyId) { mutableStateOf(activeFamilyId) }
    var editableFamilyName by remember(m.memberId, resolvedFamily?.familyName) { mutableStateOf(activeFamilyName) }
    var isSavingFamilyLink by remember(m.memberId) { mutableStateOf(false) }
    var showPastorConnectDialog by remember(m.memberId) { mutableStateOf(false) }
    var editingPastorRoleEntry by remember(m.memberId) { mutableStateOf<FamilyMemberEntry?>(null) }

    if (showPastorConnectDialog) {
      ConnectFamilyMemberDialog(
        churchId = m.churchId.ifBlank { "church_main" },
        memberRepository = memberRepository,
        existingLinkedUids = ((resolvedFamily?.linkedMemberUids ?: emptyList()) + m.uid).distinct(),
        availableMembersForPicker = members,
        allowHeadAssignment = true,
        onDismiss = { showPastorConnectDialog = false },
        onConnectBeliever = { targetMember, role, makeHead ->
          coroutineScope.launch {
            val res = memberRepository.familyRepository.connectBelieverToFamily(
              familyId = activeFamilyId,
              targetMember = targetMember,
              relationshipRole = role,
              makeHead = makeHead,
              defaultHeadMember = m
            )
            showPastorConnectDialog = false
            res.onSuccess {
              Toast.makeText(context, "${targetMember.fullName} connected as $role", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
              Toast.makeText(context, err.localizedMessage ?: "Failed to connect believer", Toast.LENGTH_LONG).show()
            }
          }
        },
        onAddManualDependent = { entry ->
          coroutineScope.launch {
            val res = memberRepository.familyRepository.addDependentMember(activeFamilyId, entry)
            showPastorConnectDialog = false
            res.onSuccess {
              Toast.makeText(context, "${entry.name} added to family", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
              Toast.makeText(context, err.localizedMessage ?: "Failed to add dependent", Toast.LENGTH_LONG).show()
            }
          }
        }
      )
    }

    if (editingPastorRoleEntry != null) {
      EditFamilyMemberRoleDialog(
        entry = editingPastorRoleEntry!!,
        allowPromoteToHead = true,
        onDismiss = { editingPastorRoleEntry = null },
        onSaveRole = { newRole, promoteToHead ->
          val targetEntry = editingPastorRoleEntry!!
          coroutineScope.launch {
            val res = memberRepository.familyRepository.updateMemberRoleInFamily(
              familyId = activeFamilyId,
              entryIdOrUid = targetEntry.id,
              newRole = newRole,
              promoteToHead = promoteToHead
            )
            editingPastorRoleEntry = null
            res.onSuccess {
              Toast.makeText(context, "${targetEntry.name}'s role updated to $newRole", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
              Toast.makeText(context, err.localizedMessage ?: "Failed to update role", Toast.LENGTH_LONG).show()
            }
          }
        }
      )
    }

    AlertDialog(
      onDismissRequest = { selectedMemberDetail = null },
      containerColor = SurfaceWhite,
      title = {
        val detailPhoto = ProfilePhotoHelper.resolvePhotoUrl(m.profilePhotoUrl, memberPhotoByUid[m.uid])
        Row(verticalAlignment = Alignment.CenterVertically) {
          MemberAvatar(
            photoUrl = detailPhoto,
            displayName = m.fullName,
            size = 52.dp,
            onClick = { photoPreviewTarget = m.copy(profilePhotoUrl = detailPhoto) }
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(m.fullName, fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Text(m.membershipId, color = ClassicChurchBlue, style = MaterialTheme.typography.labelSmall)
            Text("Tap photo to view / update", color = TextSlate, fontSize = 10.sp)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text("Phone: ${m.phoneNumber}", color = TextSlate)
          if (m.email.isNotBlank()) Text("Email: ${m.email}", color = TextSlate)
          if (m.dateOfBirth.isNotBlank()) Text("DOB: ${m.dateOfBirth}", color = TextSlate)
          if (m.gender.isNotBlank()) Text("Gender: ${m.gender}", color = TextSlate)
          if (m.address.isNotBlank()) Text("Address: ${m.address}", color = TextSlate)
          if (m.fatherName.isNotBlank()) Text("Father: ${m.fatherName}", color = TextSlate)
          if (m.motherName.isNotBlank()) Text("Mother: ${m.motherName}", color = TextSlate)
          if (m.spouseName.isNotBlank()) Text("Spouse: ${m.spouseName}", color = TextSlate)
          Text("Baptism: ${m.baptismStatus}", fontWeight = FontWeight.SemiBold, color = TextDeepNavy)
          if (m.baptismPlace.isNotBlank()) Text("Baptized at: ${m.baptismPlace}", color = TextSlate)
          if (m.baptizedBy.isNotBlank()) Text("Baptized by: ${m.baptizedBy}", color = TextSlate)
          if (m.ministryGroup.isNotBlank()) Text("Ministry: ${m.ministryGroup}", color = ClassicChurchBlue)
          Text("Status: ${m.membershipStatus}", color = SuccessGreen, fontWeight = FontWeight.Bold)

          HorizontalDivider(color = SoftBlueBorder, modifier = Modifier.padding(vertical = 4.dp))
          Text(
            text = "Household & Family Management ($householdCount member(s))",
            fontWeight = FontWeight.Bold,
            color = ClassicChurchBlue,
            fontSize = 13.sp
          )

          // Head badge row
          val headUid = resolvedFamily?.headUid?.ifBlank { m.uid } ?: m.uid
          val headMember = members.firstOrNull { it.uid == headUid }
          val headName = resolvedFamily?.headName?.ifBlank { headMember?.fullName ?: m.fullName } ?: m.fullName
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftBlueContainer,
            border = BorderStroke(1.dp, SoftBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MemberAvatar(
                photoUrl = ProfilePhotoHelper.resolvePhotoUrl(headMember?.profilePhotoUrl, memberPhotoByUid[headUid], m.profilePhotoUrl),
                displayName = headName,
                size = 30.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(headName, fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 12.sp)
                Text("Household Head", color = ClassicChurchBlue, fontSize = 10.sp)
              }
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Head",
                tint = ClassicChurchBlue,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          rosterEntries.forEach { fm ->
            val linkedProfile = members.firstOrNull { it.uid == fm.linkedUid }
            val resolvedPhoto = ProfilePhotoHelper.resolvePhotoUrl(
              linkedProfile?.profilePhotoUrl,
              memberPhotoByUid[fm.linkedUid],
              fm.profilePhotoUrl
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CleanBackground,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                MemberAvatar(photoUrl = resolvedPhoto, displayName = fm.name, size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(fm.name, fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 12.sp)
                  Text(
                    text = listOfNotNull(
                      fm.relationship.ifBlank { "Member" },
                      fm.membershipId.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    color = TextSlate,
                    fontSize = 10.sp
                  )
                }
                IconButton(
                  onClick = { editingPastorRoleEntry = fm },
                  modifier = Modifier.size(40.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Role",
                    tint = ClassicChurchBlue,
                    modifier = Modifier.size(16.dp)
                  )
                }
                IconButton(
                  onClick = {
                    coroutineScope.launch {
                      val res = memberRepository.familyRepository.removeDependentMember(activeFamilyId, fm.id)
                      res.onSuccess {
                        Toast.makeText(context, "${fm.name} unlinked from family", Toast.LENGTH_SHORT).show()
                      }.onFailure { err ->
                        Toast.makeText(context, err.localizedMessage ?: "Failed to remove member", Toast.LENGTH_SHORT).show()
                      }
                    }
                  },
                  modifier = Modifier.size(40.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }

          OutlinedButton(
            onClick = { showPastorConnectDialog = true },
            border = BorderStroke(1.dp, ClassicChurchBlue),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = SoftBlueContainer,
              contentColor = ClassicChurchBlue
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("pastor_connect_family_member_button")
          ) {
            Icon(
              imageVector = Icons.Default.Link,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Connect / Add Family Member", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          OutlinedTextField(
            value = editableFamilyName,
            onValueChange = { editableFamilyName = it },
            label = { Text("Family Name", color = TextSlate) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
          OutlinedTextField(
            value = editableFamilyId,
            onValueChange = { editableFamilyId = it },
            label = { Text("Family ID (share ID to link household)", color = TextSlate) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pastor_edit_family_id_input"),
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
          OutlinedButton(
            onClick = {
              isSavingFamilyLink = true
              coroutineScope.launch {
                val res = memberRepository.updateMemberFamilyAssignment(
                  member = m,
                  newFamilyId = editableFamilyId,
                  newFamilyName = editableFamilyName
                )
                isSavingFamilyLink = false
                res.onSuccess { updated ->
                  selectedMemberDetail = updated
                  Toast.makeText(context, "Family assignment saved (${updated.familyId})", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                  Toast.makeText(context, err.localizedMessage ?: "Failed to update family", Toast.LENGTH_LONG).show()
                }
              }
            },
            enabled = !isSavingFamilyLink,
            border = BorderStroke(1.dp, ClassicChurchBlue),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("pastor_save_family_link_button")
          ) {
            Text(
              text = if (isSavingFamilyLink) "Saving Family Link..." else "Save Family Link",
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { selectedMemberDetail = null },
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          )
        ) {
          Text("Done", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        if (m.phoneNumber.isNotBlank()) {
          OutlinedButton(
            onClick = {
              val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${m.phoneNumber.replace(" ", "")}")
              }
              context.launchActivitySafely(dialIntent, "Dialer unavailable")
            },
            border = BorderStroke(1.dp, ClassicChurchBlue),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = SurfaceWhite,
              contentColor = ClassicChurchBlue
            )
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Call", color = ClassicChurchBlue, fontWeight = FontWeight.Bold)
          }
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Congregation Directory",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        Text(
          text = "${members.size} active • ${pendingApplications.size} pending",
          style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
        )
      }
      OutlinedButton(
        onClick = {
          val csv = CsvExportHelper.buildMembersCsv(members)
          CsvExportHelper.shareCsv(context, "Church Members Directory (CSV)", csv)
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClassicChurchBlue),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = ClassicChurchBlue
        ),
        modifier = Modifier
          .height(48.dp)
          .testTag("export_members_csv_button")
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = null,
          tint = ClassicChurchBlue,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Export CSV", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("member_search_input"),
      placeholder = { Text("Search by name, ID, or phone...", color = com.example.ui.theme.TextMuted) },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ClassicChurchBlue)
      },
      shape = RoundedCornerShape(12.dp),
      singleLine = true,
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

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = SurfaceWhite,
        labelColor = TextDeepNavy,
        selectedContainerColor = SoftBlueContainer,
        selectedLabelColor = ClassicChurchBlue
      )
      FilterChip(
        selected = selectedFilter == "ALL",
        onClick = { selectedFilter = "ALL" },
        label = {
          Text(
            "All Members (${members.size})",
            fontWeight = if (selectedFilter == "ALL") FontWeight.Bold else FontWeight.Medium,
            color = if (selectedFilter == "ALL") ClassicChurchBlue else TextDeepNavy
          )
        },
        colors = chipColors,
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selectedFilter == "ALL",
          borderColor = SoftBlueBorder,
          selectedBorderColor = ClassicChurchBlue
        )
      )

      FilterChip(
        selected = selectedFilter == "PENDING",
        onClick = { selectedFilter = "PENDING" },
        label = {
          Text(
            "Pending Apps (${pendingApplications.size})",
            fontWeight = if (selectedFilter == "PENDING") FontWeight.Bold else FontWeight.Medium,
            color = if (selectedFilter == "PENDING") ClassicChurchBlue else TextDeepNavy
          )
        },
        colors = chipColors,
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selectedFilter == "PENDING",
          borderColor = SoftBlueBorder,
          selectedBorderColor = ClassicChurchBlue
        )
      )

      FilterChip(
        selected = selectedFilter == "BAPTIZED",
        onClick = { selectedFilter = "BAPTIZED" },
        label = {
          Text(
            "Baptized",
            fontWeight = if (selectedFilter == "BAPTIZED") FontWeight.Bold else FontWeight.Medium,
            color = if (selectedFilter == "BAPTIZED") ClassicChurchBlue else TextDeepNavy
          )
        },
        colors = chipColors,
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selectedFilter == "BAPTIZED",
          borderColor = SoftBlueBorder,
          selectedBorderColor = ClassicChurchBlue
        )
      )
    }

    // List of Members or Applications
    if (selectedFilter == "PENDING") {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (pendingApplications.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No pending membership applications", color = TextSlate)
            }
          }
        }
        items(pendingApplications, key = { it.applicationId.ifBlank { it.uid } }) { app ->
          val appPhoto = ProfilePhotoHelper.resolvePhotoUrl(app.profilePhotoUrl, memberPhotoByUid[app.uid])
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, SoftBlueBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              MemberAvatar(
                photoUrl = appPhoto,
                displayName = app.fullName,
                size = 46.dp,
                onClick = {
                  photoPreviewTarget = BelieverMember(
                    memberId = "mem_${app.uid}",
                    uid = app.uid,
                    churchId = app.churchId.ifBlank { resolvedChurchId },
                    fullName = app.fullName,
                    phoneNumber = app.phoneNumber,
                    profilePhotoUrl = appPhoto
                  )
                }
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(app.fullName, fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 16.sp)
                Text("Phone: ${app.phoneNumber}", color = TextSlate, fontSize = 13.sp)
                Text("Baptism: ${app.baptismStatus}", color = ClassicChurchBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { reviewingApplication = app },
                colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Review", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }
    } else {
      // Filtered Members
      val filteredMembers = members.filter { m ->
        val matchesSearch = m.fullName.contains(searchQuery, ignoreCase = true) ||
          m.membershipId.contains(searchQuery, ignoreCase = true) ||
          m.phoneNumber.contains(searchQuery)
        val matchesBaptism = if (selectedFilter == "BAPTIZED") m.baptismStatus == "BAPTIZED" else true
        matchesSearch && matchesBaptism
      }

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (filteredMembers.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No members found matching '$searchQuery'", color = TextSlate)
            }
          }
        }
        items(filteredMembers, key = { it.memberId.ifBlank { it.uid } }) { member ->
          val memberFam = allFamilies.firstOrNull { fam ->
            fam.familyId == member.effectiveFamilyId() ||
              member.uid in fam.linkedMemberUids ||
              fam.headUid == member.uid
          }
          val famNameDisplay = memberFam?.resolvedFamilyName() ?: member.effectiveFamilyName()
          val famCountDisplay = memberFam?.totalHouseholdCount() ?: member.totalFamilyMembersCount()

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedMemberDetail = member },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, SoftBlueBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val resolvedMemberPhoto = ProfilePhotoHelper.resolvePhotoUrl(
                member.profilePhotoUrl,
                memberPhotoByUid[member.uid]
              )
              MemberAvatar(
                photoUrl = resolvedMemberPhoto,
                displayName = member.fullName,
                size = 48.dp,
                onClick = { photoPreviewTarget = member.copy(profilePhotoUrl = resolvedMemberPhoto) }
              )

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, fontWeight = FontWeight.Bold, color = TextDeepNavy, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(member.membershipId, color = ClassicChurchBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text(" • ${member.phoneNumber}", color = TextSlate, fontSize = 13.sp)
                }
                Text(
                  text = "$famNameDisplay • $famCountDisplay member(s)",
                  color = TextSlate,
                  fontSize = 11.sp
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (member.baptismStatus == "BAPTIZED") SuccessGreenContainer else SoftBlueContainer
              ) {
                Text(
                  text = member.baptismStatus,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (member.baptismStatus == "BAPTIZED") SuccessGreen else ClassicChurchBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
