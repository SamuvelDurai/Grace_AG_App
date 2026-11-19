package com.example.presentation.member

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.util.ProfilePhotoHelper
import com.example.data.member.MemberRepository
import com.example.presentation.common.MemberAvatar
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelieverProfileDetailScreen(
  member: BelieverMember,
  memberRepository: MemberRepository,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var showDigitalCard by remember { mutableStateOf(false) }
  var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
  var saveErrorMessage by remember { mutableStateOf<String?>(null) }
  var isEncodingPhoto by remember { mutableStateOf(false) }
  var showConnectDialog by remember { mutableStateOf(false) }
  var editingLinkedRoleEntry by remember { mutableStateOf<FamilyMemberEntry?>(null) }

  // Form Fields
  var fullName by remember { mutableStateOf(member.fullName) }
  var phoneNumber by remember { mutableStateOf(member.phoneNumber) }
  var email by remember { mutableStateOf(member.email) }
  var dateOfBirth by remember { mutableStateOf(member.dateOfBirth) }
  var gender by remember { mutableStateOf(member.gender) }
  var address by remember { mutableStateOf(member.address) }
  var selectedPhotoUri by remember { mutableStateOf(member.profilePhotoUrl) }

  // Family Fields
  val observedFamily by memberRepository.familyRepository
    .observeFamilyForMember(member.uid, member.effectiveFamilyId())
    .collectAsState(initial = null)
  var fatherName by remember { mutableStateOf(member.fatherName) }
  var motherName by remember { mutableStateOf(member.motherName) }
  var spouseName by remember { mutableStateOf(member.spouseName) }
  var familyName by remember { mutableStateOf(member.effectiveFamilyName()) }
  var familyMembers by remember { mutableStateOf(member.familyMembers) }

  LaunchedEffect(observedFamily) {
    val fam = observedFamily ?: return@LaunchedEffect
    if (fam.familyName.isNotBlank()) familyName = fam.familyName
    if (fam.dependentMembers.isNotEmpty()) {
      familyMembers = fam.dependentMembers
    }
  }

  // Baptism & Spiritual Fields
  var baptismStatus by remember { mutableStateOf(member.baptismStatus) }
  var baptismDate by remember { mutableStateOf(member.baptismDate) }
  var baptismPlace by remember { mutableStateOf(member.baptismPlace) }
  var baptizedBy by remember { mutableStateOf(member.baptizedBy) }
  var ministryGroup by remember { mutableStateOf(member.ministryGroup) }

  // Android Photo Picker Launcher (Zero permissions required, 100% Play Policy compliant)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let { pickedUri ->
      isEncodingPhoto = true
      coroutineScope.launch {
        val compressed = withContext(Dispatchers.IO) {
          ProfilePhotoHelper.encodeUriToCompressedDataUri(context, pickedUri)
        }
        isEncodingPhoto = false
        if (compressed != null) {
          selectedPhotoUri = compressed
          saveSuccessMessage = null
        } else {
          Toast.makeText(context, "Unable to read selected image.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val scrollState = rememberScrollState()
  val activeFamilyId = observedFamily?.familyId?.ifBlank { member.effectiveFamilyId() } ?: member.effectiveFamilyId()
  val isHead = observedFamily == null || observedFamily?.headUid.isNullOrBlank() || observedFamily?.headUid == member.uid

  if (showConnectDialog) {
    ConnectFamilyMemberDialog(
      churchId = member.churchId.ifBlank { "church_main" },
      memberRepository = memberRepository,
      existingLinkedUids = ((observedFamily?.linkedMemberUids ?: emptyList()) + member.uid).distinct(),
      allowHeadAssignment = isHead,
      onDismiss = { showConnectDialog = false },
      onConnectBeliever = { target, role, makeHead ->
        coroutineScope.launch {
          val res = memberRepository.familyRepository.connectBelieverToFamily(
            familyId = activeFamilyId,
            targetMember = target,
            relationshipRole = role,
            makeHead = makeHead,
            defaultHeadMember = member.copy(fullName = fullName, profilePhotoUrl = selectedPhotoUri)
          )
          showConnectDialog = false
          res.onSuccess { updatedFam ->
            familyMembers = updatedFam.dependentMembers
            Toast.makeText(context, "${target.fullName} connected as $role", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Unable to connect member", Toast.LENGTH_LONG).show()
          }
        }
      },
      onAddManualDependent = { entry ->
        familyMembers = familyMembers + entry
        showConnectDialog = false
      }
    )
  }

  if (editingLinkedRoleEntry != null) {
    EditFamilyMemberRoleDialog(
      entry = editingLinkedRoleEntry!!,
      allowPromoteToHead = isHead,
      onDismiss = { editingLinkedRoleEntry = null },
      onSaveRole = { newRole, promote ->
        val entry = editingLinkedRoleEntry!!
        coroutineScope.launch {
          val res = memberRepository.familyRepository.updateMemberRoleInFamily(
            familyId = activeFamilyId,
            entryIdOrUid = entry.id,
            newRole = newRole,
            promoteToHead = promote
          )
          editingLinkedRoleEntry = null
          res.onSuccess { updatedFam ->
            familyMembers = updatedFam.dependentMembers
            Toast.makeText(context, "Updated ${entry.name}'s role to $newRole", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Failed to update role", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  if (showDigitalCard) {
    DigitalMembershipCardDialog(
      member = member.copy(
        fullName = fullName,
        profilePhotoUrl = selectedPhotoUri,
        baptismStatus = baptismStatus
      ),
      onDismiss = { showDigitalCard = false }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Member Profile & ID",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = ClassicChurchBlue
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showDigitalCard = true },
            modifier = Modifier.testTag("open_digital_card_button")
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = "Digital ID Card",
              tint = ClassicChurchBlue
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
      )
    },
    containerColor = CleanBackground,
    modifier = modifier.testTag("believer_profile_detail_screen")
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(scrollState)
        .padding(16.dp)
        .imePadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Photo and Membership ID Header Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
              .clickable(enabled = !isEncodingPhoto) {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .testTag("profile_photo_picker")
          ) {
            MemberAvatar(
              photoUrl = selectedPhotoUri,
              displayName = fullName,
              size = 90.dp
            )
            if (isEncodingPhoto) {
              CircularProgressIndicator(
                color = ClassicChurchBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(90.dp)
              )
            }

            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(ClassicChurchBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Upload Photo",
                tint = TextWhite,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = fullName.ifBlank { "Church Member" },
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Spacer(modifier = Modifier.height(4.dp))

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftBlueContainer
          ) {
            Text(
              text = member.membershipId.ifBlank { "Pending ID" },
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ClassicChurchBlue
              ),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { showDigitalCard = true },
            colors = ButtonDefaults.buttonColors(containerColor = SoftBlueContainer),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "View Digital Membership Card",
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // 1. Personal Details Section
      SectionCard(title = "Personal Information", icon = Icons.Default.Person) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = defaultFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone", color = TextSlate) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = defaultFieldColors()
          )
          OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { dateOfBirth = it },
            label = { Text("DOB (DD/MM/YYYY)", color = TextSlate) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = defaultFieldColors()
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = defaultFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Residential Address", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          colors = defaultFieldColors()
        )
      }

      // 2. Family Details Section
      SectionCard(title = "Family & Household", icon = Icons.Default.FamilyRestroom) {
        FamilyMembersSection(
          familyName = familyName,
          onFamilyNameChange = { familyName = it },
          familyMembers = familyMembers,
          onAddMember = { entry -> familyMembers = familyMembers + entry },
          onUpdateMember = { entry ->
            familyMembers = familyMembers.map { if (it.id == entry.id) entry else it }
          },
          onRemoveMember = { id ->
            val target = familyMembers.firstOrNull { it.id == id }
            familyMembers = familyMembers.filterNot { it.id == id }
            if (target != null && target.linkedUid.isNotBlank()) {
              coroutineScope.launch {
                memberRepository.familyRepository.removeDependentMember(activeFamilyId, id)
              }
            }
          },
          onOpenConnectAccount = if (isHead) {
            { showConnectDialog = true }
          } else null,
          onUpdateLinkedRole = if (isHead) {
            { entry -> editingLinkedRoleEntry = entry }
          } else null
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = fatherName,
          onValueChange = { fatherName = it },
          label = { Text("Father's Name", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = defaultFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = motherName,
          onValueChange = { motherName = it },
          label = { Text("Mother's Name", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = defaultFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = spouseName,
          onValueChange = { spouseName = it },
          label = { Text("Spouse Name (if married)", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = defaultFieldColors()
        )
      }

      // 3. Spiritual & Baptism Section
      SectionCard(title = "Spiritual & Baptism Record", icon = Icons.Default.WaterDrop) {
        Text(
          text = "Baptism Status",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = TextDeepNavy
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("BAPTIZED", "NOT_BAPTIZED", "PENDING").forEach { status ->
            val selected = baptismStatus == status
            FilterChip(
              selected = selected,
              onClick = { baptismStatus = status },
              label = {
                Text(
                  text = status.replace("_", " "),
                  fontSize = 12.sp,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) ClassicChurchBlue else TextDeepNavy
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
              )
            )
          }
        }

        if (baptismStatus == "BAPTIZED") {
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = baptismDate,
            onValueChange = { baptismDate = it },
            label = { Text("Baptism Date (DD/MM/YYYY)", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = defaultFieldColors()
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = baptismPlace,
            onValueChange = { baptismPlace = it },
            label = { Text("Baptism Location / Church", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = defaultFieldColors()
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = baptizedBy,
            onValueChange = { baptizedBy = it },
            label = { Text("Baptized By (Pastor)", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = defaultFieldColors()
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = ministryGroup,
          onValueChange = { ministryGroup = it },
          label = { Text("Ministry / Fellowship Group", color = TextSlate) },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("e.g. Choir, Sunday School, Youth", color = com.example.ui.theme.TextMuted) },
          singleLine = true,
          colors = defaultFieldColors()
        )
      }

      if (saveErrorMessage != null) {
        Surface(
          color = com.example.ui.theme.ErrorRedContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = saveErrorMessage ?: "",
            color = com.example.ui.theme.ErrorRed,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(14.dp)
          )
        }
      }

      if (saveSuccessMessage != null) {
        Surface(
          color = Color(0xFFDCFCE7),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = saveSuccessMessage ?: "",
              color = SuccessGreen,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }

      // Save Button
      Button(
        onClick = {
          saveSuccessMessage = null
          saveErrorMessage = null
          coroutineScope.launch {
            val updated = member.copy(
              fullName = fullName,
              phoneNumber = phoneNumber,
              email = email,
              dateOfBirth = dateOfBirth,
              gender = gender,
              address = address,
              profilePhotoUrl = selectedPhotoUri,
              fatherName = fatherName,
              motherName = motherName,
              spouseName = spouseName,
              familyId = activeFamilyId,
              familyName = familyName.trim().ifBlank { "${fullName.trim().ifBlank { "Member" }} Family" },
              familyMembers = familyMembers,
              baptismStatus = baptismStatus,
              baptismDate = baptismDate,
              baptismPlace = baptismPlace,
              baptizedBy = baptizedBy,
              ministryGroup = ministryGroup
            )
            val result = memberRepository.saveMemberProfile(updated)
            result.fold(
              onSuccess = { saveSuccessMessage = "Profile updated successfully!" },
              onFailure = { err -> saveErrorMessage = err.localizedMessage ?: "Failed to save profile." }
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_profile_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        )
      ) {
        Text(
          text = "Save Profile Changes",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextWhite
          )
        )
      }
    }
  }
}

@Composable
private fun SectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, SoftBlueBorder)
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SoftBlueContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      content()
    }
  }
}

@Composable
private fun defaultFieldColors() = OutlinedTextFieldDefaults.colors(
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
