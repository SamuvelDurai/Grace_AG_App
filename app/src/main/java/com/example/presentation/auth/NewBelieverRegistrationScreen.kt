package com.example.presentation.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.ProfilePhotoHelper
import com.example.presentation.common.MemberAvatar
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewBelieverRegistrationScreen(
  viewModel: AuthViewModel,
  onRegistrationSubmitted: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val authState by viewModel.uiState.collectAsState()
  val verifiedPhone by viewModel.phoneNumber.collectAsState()
  val isSubmitting by viewModel.isSubmittingRegistration.collectAsState()
  val submissionError by viewModel.registrationError.collectAsState()

  val currentProfile = (authState as? AuthUiState.Authenticated)?.profile

  var fullName by remember { mutableStateOf(currentProfile?.displayName ?: "") }
  var phoneInput by remember {
    mutableStateOf(
      currentProfile?.phoneNumber?.ifBlank { verifiedPhone } ?: verifiedPhone
    )
  }
  var email by remember { mutableStateOf(currentProfile?.email ?: "") }
  var dateOfBirth by remember { mutableStateOf("") }
  var gender by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var baptismStatus by remember { mutableStateOf("NOT_BAPTIZED") }
  var baptismDate by remember { mutableStateOf("") }
  var baptismPlace by remember { mutableStateOf("") }
  var baptizedBy by remember { mutableStateOf("") }
  var fatherName by remember { mutableStateOf("") }
  var motherName by remember { mutableStateOf("") }
  var spouseName by remember { mutableStateOf("") }
  var familyName by remember { mutableStateOf("") }
  var familyMembers by remember { mutableStateOf<List<com.example.core.model.FamilyMemberEntry>>(emptyList()) }
  var ministryGroup by remember { mutableStateOf("") }

  val coroutineScope = rememberCoroutineScope()
  var selectedPhotoUri by remember { mutableStateOf(currentProfile?.photoUrl.orEmpty()) }
  var isEncodingPhoto by remember { mutableStateOf(false) }

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
        } else {
          Toast.makeText(context, "Unable to read selected image. Please try another photo.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  var validationError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(currentProfile?.uid) {
    if (fullName.isBlank() && !currentProfile?.displayName.isNullOrBlank()) {
      fullName = currentProfile!!.displayName
    }
    if (phoneInput.isBlank()) {
      phoneInput = currentProfile?.phoneNumber?.ifBlank { verifiedPhone } ?: verifiedPhone
    }
    if (email.isBlank() && !currentProfile?.email.isNullOrBlank()) {
      email = currentProfile!!.email
    }
    if (selectedPhotoUri.isBlank() && !currentProfile?.photoUrl.isNullOrBlank()) {
      selectedPhotoUri = currentProfile!!.photoUrl
    }
  }

  val scrollState = rememberScrollState()
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

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("registration_screen"),
    color = CleanBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(scrollState)
        .padding(20.dp)
        .imePadding()
    ) {
      Text(
        text = stringResource(id = R.string.registration_title),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy,
          fontSize = 24.sp
        )
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = stringResource(id = R.string.registration_subtitle),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextSlate,
          lineHeight = 20.sp
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      val activeError = validationError ?: submissionError
      if (!activeError.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ErrorRedContainer,
          border = BorderStroke(1.dp, ErrorRed),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = activeError,
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Personal & Contact Details Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Personal & Contact Details",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isEncodingPhoto) {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .padding(vertical = 4.dp)
              .testTag("reg_photo_picker"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(contentAlignment = Alignment.BottomEnd) {
              MemberAvatar(
                photoUrl = selectedPhotoUri,
                displayName = fullName.ifBlank { "Member" },
                size = 64.dp
              )
              if (isEncodingPhoto) {
                CircularProgressIndicator(
                  color = ClassicChurchBlue,
                  strokeWidth = 2.5.dp,
                  modifier = Modifier.size(64.dp)
                )
              }
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(ClassicChurchBlue),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = "Upload Profile Photo",
                  tint = TextWhite,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (selectedPhotoUri.isNotBlank()) "Profile Photo Ready" else "Add Profile Photo (Optional)",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = if (isEncodingPhoto) "Processing photo..." else "Tap to choose your photo for Church Directory & Digital ID",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
              )
            }
          }

          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; validationError = null },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reg_name_input"),
            label = { Text(stringResource(id = R.string.full_name) + " *", color = TextSlate) },
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it; validationError = null },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reg_phone_input"),
            label = { Text(stringResource(id = R.string.phone_label) + " *", color = TextSlate) },
            placeholder = { Text("+91 9876543210", color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reg_email_input"),
            label = { Text(stringResource(id = R.string.email_optional), color = TextSlate) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { dateOfBirth = it; validationError = null },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reg_dob_input"),
            label = { Text("Date of Birth (DD/MM/YYYY) *", color = TextSlate) },
            placeholder = { Text("15/08/1995", color = TextMuted) },
            singleLine = true,
            colors = fieldColors
          )

          Text(
            text = "Gender *",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Male", "Female").forEach { option ->
              val selected = gender == option
              FilterChip(
                selected = selected,
                onClick = {
                  gender = option
                  validationError = null
                },
                label = {
                  Text(
                    text = option,
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
                ),
                modifier = Modifier.testTag("reg_gender_${option.lowercase()}")
              )
            }
          }

          OutlinedTextField(
            value = address,
            onValueChange = { address = it; validationError = null },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reg_address_input"),
            label = { Text("Residential Address *", color = TextSlate) },
            maxLines = 3,
            colors = fieldColors
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Baptism & Family Details Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Baptism & Family Details",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Text(
            text = "Water Baptism Status *",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "BAPTIZED" to "Baptized",
              "NOT_BAPTIZED" to "Not Yet",
              "PENDING" to "Requested"
            ).forEach { (value, label) ->
              val selected = baptismStatus == value
              FilterChip(
                selected = selected,
                onClick = { baptismStatus = value },
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

          if (baptismStatus == "BAPTIZED") {
            OutlinedTextField(
              value = baptismDate,
              onValueChange = { baptismDate = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Baptism Date (Optional)", color = TextSlate) },
              placeholder = { Text("DD/MM/YYYY", color = TextMuted) },
              singleLine = true,
              colors = fieldColors
            )
            OutlinedTextField(
              value = baptismPlace,
              onValueChange = { baptismPlace = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Baptism Church / Place (Optional)", color = TextSlate) },
              singleLine = true,
              colors = fieldColors
            )
            OutlinedTextField(
              value = baptizedBy,
              onValueChange = { baptizedBy = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Baptized By Pastor (Optional)", color = TextSlate) },
              singleLine = true,
              colors = fieldColors
            )
          }

          OutlinedTextField(
            value = fatherName,
            onValueChange = { fatherName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Father's Name (Optional)", color = TextSlate) },
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = motherName,
            onValueChange = { motherName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mother's Name (Optional)", color = TextSlate) },
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = spouseName,
            onValueChange = { spouseName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Spouse Name (If Married)", color = TextSlate) },
            singleLine = true,
            colors = fieldColors
          )

          OutlinedTextField(
            value = ministryGroup,
            onValueChange = { ministryGroup = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Preferred Area Cell / Ministry (Optional)", color = TextSlate) },
            placeholder = { Text("e.g., Choir, Youth, Anna Nagar Care Cell", color = TextMuted) },
            singleLine = true,
            colors = fieldColors
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Household / Family Members Card (1 Subscription per Family)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Household & Family Members (Optional)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Spacer(modifier = Modifier.height(10.dp))
          com.example.presentation.member.FamilyMembersSection(
            familyName = familyName,
            onFamilyNameChange = { familyName = it },
            familyMembers = familyMembers,
            onAddMember = { entry -> familyMembers = familyMembers + entry },
            onUpdateMember = { entry ->
              familyMembers = familyMembers.map { if (it.id == entry.id) entry else it }
            },
            onRemoveMember = { id ->
              familyMembers = familyMembers.filterNot { it.id == id }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      Button(
        onClick = {
          val phoneDigits = phoneInput.filter { it.isDigit() }
          when {
            fullName.isBlank() -> validationError = "Please enter your full name."
            phoneDigits.length < 10 -> validationError = "Please enter a valid 10-digit mobile number."
            dateOfBirth.isBlank() -> validationError = "Please enter your date of birth (DD/MM/YYYY)."
            gender.isBlank() -> validationError = "Please select your gender."
            address.isBlank() -> validationError = "Please enter your residential address."
            else -> {
              validationError = null
              viewModel.registerNewBeliever(
                fullName = fullName,
                phoneInput = phoneInput,
                emailInput = email,
                dateOfBirth = dateOfBirth,
                gender = gender,
                address = address,
                baptismStatus = baptismStatus,
                baptismDate = baptismDate,
                baptismPlace = baptismPlace,
                baptizedBy = baptizedBy,
                fatherName = fatherName,
                motherName = motherName,
                spouseName = spouseName,
                familyName = familyName,
                familyMembers = familyMembers,
                ministryGroup = ministryGroup,
                profilePhotoUrl = selectedPhotoUri,
                onComplete = onRegistrationSubmitted
              )
            }
          }
        },
        enabled = !isSubmitting,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("submit_application_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = TextWhite,
            strokeWidth = 2.dp
          )
        } else {
          Text(
            text = stringResource(id = R.string.submit_application),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextWhite
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
        onClick = { viewModel.signOut(context) },
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("reg_sign_out_button")
      ) {
        Text(
          text = "Use a different account (" + stringResource(id = R.string.sign_out) + ")",
          color = ErrorRed,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
