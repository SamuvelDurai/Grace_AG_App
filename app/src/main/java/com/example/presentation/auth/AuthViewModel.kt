package com.example.presentation.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AccountStatus
import com.example.core.model.BelieverMember
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.example.data.auth.AuthRepository
import com.example.data.auth.SessionManager
import com.example.data.member.MemberRepository
import com.example.data.user.UserRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import com.example.core.util.ChurchIdGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
  data object Initial : AuthUiState
  data object Loading : AuthUiState
  data class Unauthenticated(val message: String? = null) : AuthUiState
  data class Authenticated(val profile: UserProfile) : AuthUiState
  data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
  private val authRepository: AuthRepository,
  private val userRepository: UserRepository,
  private val memberRepository: MemberRepository,
  private val sessionManager: SessionManager
) : ViewModel() {

  private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  // Phone Verification State (Firebase PhoneAuthProvider)
  val phoneNumber = MutableStateFlow("")
  val otpCode = MutableStateFlow("")
  val isOtpSent = MutableStateFlow(false)
  val timerSeconds = MutableStateFlow(60)
  val isPhoneLoading = MutableStateFlow(false)
  val phoneError = MutableStateFlow<String?>(null)

  // Email Sign-In / Sign-Up & Registration State
  val isEmailLoading = MutableStateFlow(false)
  val authActionError = MutableStateFlow<String?>(null)
  val authInfoMessage = MutableStateFlow<String?>(null)
  val isSubmittingRegistration = MutableStateFlow(false)
  val registrationError = MutableStateFlow<String?>(null)

  private var phoneVerificationId: String? = null
  private var phoneResendToken: PhoneAuthProvider.ForceResendingToken? = null
  private var timerJob: Job? = null

  init {
    restoreSavedSessionIfFirebaseAuthenticated()
    observeAuthState()
  }

  private fun restoreSavedSessionIfFirebaseAuthenticated() {
    val firebaseUser = authRepository.currentUser
    if (firebaseUser == null) {
      sessionManager.clearSession()
      return
    }

    val savedProfile = sessionManager.getSavedProfile()
    if (savedProfile != null && savedProfile.uid == firebaseUser.uid) {
      _uiState.value = AuthUiState.Authenticated(savedProfile)
    }
  }

  private fun observeAuthState() {
    viewModelScope.launch {
      authRepository.observeAuthState().collect { firebaseUser ->
        if (firebaseUser != null) {
          loadUserProfile(firebaseUser)
        } else {
          sessionManager.clearSession()
          _uiState.value = AuthUiState.Unauthenticated()
        }
      }
    }
  }

  private fun applyAuthenticatedProfile(profile: UserProfile) {
    sessionManager.saveSession(profile)
    _uiState.value = AuthUiState.Authenticated(profile)
  }

  fun clearAuthMessages() {
    authActionError.value = null
    authInfoMessage.value = null
    phoneError.value = null
  }

  fun loadUserProfile(firebaseUser: FirebaseUser) {
    val existingAuth = (_uiState.value as? AuthUiState.Authenticated)?.profile
    if (existingAuth == null) {
      _uiState.value = AuthUiState.Loading
    }
    viewModelScope.launch {
      val fetched = userRepository.getUserProfile(firebaseUser.uid)
      val email = firebaseUser.email ?: ""
      val displayName = firebaseUser.displayName ?: ""
      val phone = firebaseUser.phoneNumber ?: phoneNumber.value
      val isBootstrapOwner = OWNER_EMAILS.any { it.equals(email, ignoreCase = true) }

      val resolvedProfile = if (fetched != null) {
        when {
          isBootstrapOwner &&
            (fetched.userRole != UserRole.SUPER_ADMIN || fetched.status != AccountStatus.APPROVED || !fetched.profileCompleted) -> {
            val upgraded = fetched.copy(
              role = UserRole.SUPER_ADMIN.name,
              accountStatus = AccountStatus.APPROVED.name,
              displayName = fetched.displayName.ifBlank { displayName.ifBlank { "Church Admin" } },
              email = fetched.email.ifBlank { email },
              profileCompleted = true
            )
            userRepository.updateUserProfile(upgraded)
            userRepository.seedDefaultChurchInfo(adminUid = firebaseUser.uid, adminEmail = email)
            upgraded
          }
          fetched.userRole == UserRole.BELIEVER -> {
            val invite = userRepository.findMatchingPastorInvite(email, phone)
            if (invite != null) {
              val promoted = fetched.copy(
                role = UserRole.PASTOR.name,
                accountStatus = AccountStatus.APPROVED.name,
                displayName = fetched.displayName.ifBlank { (invite["displayName"] as? String) ?: displayName.ifBlank { "Pastor" } },
                phoneNumber = fetched.phoneNumber.ifBlank { (invite["phoneNormalized"] as? String) ?: phone },
                email = fetched.email.ifBlank { email },
                membershipId = fetched.membershipId.ifBlank {
                  (invite["membershipId"] as? String)?.takeIf { it.isNotBlank() }
                    ?: userRepository.allocatePastorId(fetched.churchId.ifBlank { "church_main" })
                },
                profileCompleted = true
              )
              userRepository.updateUserProfile(promoted)
              promoted
            } else if (!fetched.profileCompleted &&
              (fetched.status == AccountStatus.APPROVED || userRepository.hasMembershipApplication(firebaseUser.uid))
            ) {
              val synced = fetched.copy(profileCompleted = true)
              userRepository.updateUserProfile(synced)
              synced
            } else {
              fetched
            }
          }
          !fetched.profileCompleted && (fetched.userRole == UserRole.PASTOR || fetched.userRole == UserRole.SUPER_ADMIN) -> {
            val synced = fetched.copy(profileCompleted = true)
            userRepository.updateUserProfile(synced)
            synced
          }
          else -> fetched
        }
      } else {
        val pastorInvite = userRepository.findMatchingPastorInvite(email, phone)
        val adminClaimed = userRepository.isChurchAdminClaimed()
        val isInitialAdmin = isBootstrapOwner || (pastorInvite == null && !adminClaimed)

        val (initialRole, initialStatus, initialCompleted) = when {
          isInitialAdmin -> Triple(UserRole.SUPER_ADMIN, AccountStatus.APPROVED, true)
          pastorInvite != null -> Triple(UserRole.PASTOR, AccountStatus.APPROVED, true)
          else -> Triple(UserRole.BELIEVER, AccountStatus.PENDING, false)
        }

        val resolvedName = when {
          pastorInvite != null -> (pastorInvite["displayName"] as? String)?.ifBlank { displayName } ?: displayName.ifBlank { "Pastor" }
          isInitialAdmin -> displayName.ifBlank { "Church Admin" }
          else -> displayName
        }

        val resolvedMembershipId = when {
          isInitialAdmin -> ChurchIdGenerator.adminId()
          pastorInvite != null -> (pastorInvite["membershipId"] as? String)?.takeIf { it.isNotBlank() }
            ?: userRepository.allocatePastorId("church_main")
          else -> ""
        }

        val newProfile = UserProfile(
          uid = firebaseUser.uid,
          displayName = resolvedName,
          email = email,
          phoneNumber = (pastorInvite?.get("phoneNormalized") as? String)?.ifBlank { phone } ?: phone,
          role = initialRole.name,
          churchId = "church_main",
          accountStatus = initialStatus.name,
          membershipId = resolvedMembershipId,
          profileCompleted = initialCompleted
        )
        userRepository.createUserProfile(newProfile)
        if (isInitialAdmin) {
          userRepository.seedDefaultChurchInfo(
            adminUid = firebaseUser.uid,
            adminEmail = email
          )
        }
        newProfile
      }

      applyAuthenticatedProfile(resolvedProfile)
    }
  }

  fun signInWithEmail(email: String, password: String) {
    val cleanEmail = email.trim()
    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      authActionError.value = "Please enter a valid email address."
      return
    }
    if (password.isBlank()) {
      authActionError.value = "Please enter your password."
      return
    }
    clearAuthMessages()
    isEmailLoading.value = true
    viewModelScope.launch {
      val result = authRepository.signInWithEmail(cleanEmail, password)
      isEmailLoading.value = false
      result.onSuccess { user ->
        loadUserProfile(user)
      }.onFailure { e ->
        authActionError.value = e.localizedMessage ?: "Invalid email or password. Please check your credentials."
      }
    }
  }

  fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
    val cleanEmail = email.trim()
    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      authActionError.value = "Please enter a valid email address."
      return
    }
    if (password.length < 6) {
      authActionError.value = "Password must be at least 6 characters."
      return
    }
    if (password != confirmPassword) {
      authActionError.value = "Passwords do not match."
      return
    }
    clearAuthMessages()
    isEmailLoading.value = true
    viewModelScope.launch {
      val result = authRepository.signUpWithEmail(cleanEmail, password)
      isEmailLoading.value = false
      result.onSuccess { user ->
        loadUserProfile(user)
      }.onFailure { e ->
        authActionError.value = e.localizedMessage ?: "Unable to create account with this email."
      }
    }
  }

  fun sendPasswordReset(email: String) {
    val cleanEmail = email.trim()
    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      authActionError.value = "Enter your email address first to reset your password."
      return
    }
    clearAuthMessages()
    isEmailLoading.value = true
    viewModelScope.launch {
      val result = authRepository.sendPasswordReset(cleanEmail)
      isEmailLoading.value = false
      result.onSuccess {
        authInfoMessage.value = "Password reset link sent to $cleanEmail"
      }.onFailure { e ->
        authActionError.value = e.localizedMessage ?: "Could not send password reset email."
      }
    }
  }

  fun signInWithGoogle(activity: Activity) {
    clearAuthMessages()
    _uiState.value = AuthUiState.Loading
    val credentialManager = CredentialManager.create(activity)
    viewModelScope.launch {
      val result = authRepository.signInWithGoogle(activity, credentialManager)
      result.onSuccess { user ->
        loadUserProfile(user)
      }.onFailure { e ->
        val errMsg = if (e is androidx.credentials.exceptions.GetCredentialCancellationException) {
          "Google Sign-In was cancelled."
        } else {
          e.localizedMessage ?: "Google Sign-In failed"
        }
        authActionError.value = errMsg
        _uiState.value = AuthUiState.Unauthenticated(errMsg)
      }
    }
  }

  fun sendPhoneOtp(
    phone: String,
    activity: Activity,
    countryCode: String = "+91",
    onCodeSent: () -> Unit = {}
  ) {
    val e164Phone = com.example.core.util.PhoneNumberHelper.toE164OrNull(phone, countryCode)
    if (e164Phone == null) {
      phoneError.value = if (countryCode == "+91") {
        "Enter a valid 10-digit mobile number"
      } else {
        "Enter a valid mobile number for $countryCode"
      }
      return
    }

    clearAuthMessages()
    phoneNumber.value = e164Phone
    isPhoneLoading.value = true

    authRepository.sendPhoneVerificationCode(
      phoneNumberE164 = e164Phone,
      activity = activity,
      resendToken = phoneResendToken,
      onVerificationCompleted = { credential ->
        viewModelScope.launch {
          val result = authRepository.signInWithPhoneCredential(credential)
          isPhoneLoading.value = false
          result.onSuccess { user ->
            loadUserProfile(user)
          }.onFailure { e ->
            phoneError.value = e.localizedMessage ?: "Verification failed"
          }
        }
      },
      onCodeSent = { verificationId, token ->
        phoneVerificationId = verificationId
        phoneResendToken = token
        isPhoneLoading.value = false
        isOtpSent.value = true
        startTimer()
        onCodeSent()
      },
      onVerificationFailed = { err ->
        isPhoneLoading.value = false
        phoneError.value = err
      }
    )
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerSeconds.value = 60
    timerJob = viewModelScope.launch {
      while (timerSeconds.value > 0) {
        delay(1000)
        timerSeconds.value -= 1
      }
    }
  }

  fun verifyPhoneOtp(
    code: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    if (code.length != 6 || !code.all { it.isDigit() }) {
      onError("Please enter the 6-digit SMS verification code")
      return
    }
    val verificationId = phoneVerificationId
    if (verificationId.isNullOrBlank()) {
      onError("Verification session expired. Please request a new OTP.")
      return
    }

    isPhoneLoading.value = true
    viewModelScope.launch {
      val result = authRepository.signInWithPhoneOtpCode(verificationId, code)
      isPhoneLoading.value = false
      result.onSuccess { user ->
        loadUserProfile(user)
        onSuccess()
      }.onFailure { e ->
        onError(e.localizedMessage ?: "Invalid verification code. Please check and try again.")
      }
    }
  }

  fun registerNewBeliever(
    fullName: String,
    phoneInput: String,
    emailInput: String,
    dateOfBirth: String,
    gender: String,
    address: String,
    baptismStatus: String,
    baptismDate: String = "",
    baptismPlace: String = "",
    baptizedBy: String = "",
    fatherName: String = "",
    motherName: String = "",
    spouseName: String = "",
    familyName: String = "",
    familyMembers: List<com.example.core.model.FamilyMemberEntry> = emptyList(),
    ministryGroup: String = "",
    onComplete: () -> Unit
  ) {
    val firebaseUser = authRepository.currentUser ?: return
    val current = (_uiState.value as? AuthUiState.Authenticated)?.profile
      ?: sessionManager.getSavedProfile()

    val cleanPhone = phoneInput.trim().ifBlank {
      phoneNumber.value.ifBlank { current?.phoneNumber ?: (firebaseUser.phoneNumber ?: "") }
    }
    val cleanEmail = emailInput.trim().ifBlank {
      current?.email?.ifBlank { null } ?: (firebaseUser.email ?: "")
    }
    val churchId = current?.churchId?.ifBlank { null } ?: "church_main"
    val resolvedFamilyId = current?.familyId?.ifBlank { null } ?: "fam_${firebaseUser.uid}"
    val resolvedFamilyName = familyName.trim().ifBlank { "${fullName.trim()} Family" }

    registrationError.value = null
    isSubmittingRegistration.value = true

    viewModelScope.launch {
      try {
        val membershipId = current?.membershipId?.ifBlank { null }
          ?: memberRepository.allocateMembershipId(churchId)
        val updatedProfile = UserProfile(
          uid = firebaseUser.uid,
          displayName = fullName.trim(),
          phoneNumber = cleanPhone,
          email = cleanEmail,
          churchId = churchId,
          role = current?.role ?: UserRole.BELIEVER.name,
          accountStatus = current?.accountStatus ?: AccountStatus.PENDING.name,
          membershipId = membershipId,
          familyId = resolvedFamilyId,
          profileCompleted = true
        )
        userRepository.updateUserProfile(updatedProfile).getOrThrow()

        userRepository.submitMembershipApplication(
          fullName = fullName.trim(),
          phoneNumber = cleanPhone,
          email = cleanEmail,
          address = address.trim(),
          dateOfBirth = dateOfBirth.trim(),
          gender = gender.trim(),
          baptismStatus = baptismStatus.ifBlank { "NOT_BAPTIZED" },
          churchId = churchId,
          familyId = resolvedFamilyId,
          familyName = resolvedFamilyName,
          familyMembers = familyMembers
        ).getOrThrow()

        val token = ChurchIdGenerator.verificationTokenFor(membershipId)
        val memberRecord = BelieverMember(
          memberId = "mem_${firebaseUser.uid}",
          uid = firebaseUser.uid,
          churchId = churchId,
          membershipId = membershipId,
          fullName = fullName.trim(),
          phoneNumber = cleanPhone,
          email = cleanEmail,
          dateOfBirth = dateOfBirth.trim(),
          gender = gender.trim(),
          address = address.trim(),
          fatherName = fatherName.trim(),
          motherName = motherName.trim(),
          spouseName = spouseName.trim(),
          familyId = resolvedFamilyId,
          familyName = resolvedFamilyName,
          familyRole = "FAMILY_HEAD",
          familyMembers = familyMembers,
          churchJoiningDate = "Today",
          membershipStatus = if (updatedProfile.status == AccountStatus.APPROVED) "ACTIVE" else "PENDING",
          baptismStatus = baptismStatus.ifBlank { "NOT_BAPTIZED" },
          baptismDate = baptismDate.trim(),
          baptismPlace = baptismPlace.trim(),
          baptizedBy = baptizedBy.trim(),
          ministryGroup = ministryGroup.trim(),
          verificationToken = token
        )
        memberRepository.saveMemberProfile(memberRecord).getOrThrow()

        applyAuthenticatedProfile(updatedProfile)
        isSubmittingRegistration.value = false
        onComplete()
      } catch (e: Exception) {
        isSubmittingRegistration.value = false
        registrationError.value = e.localizedMessage ?: "Failed to save your details. Please try again."
      }
    }
  }

  fun refreshProfile(onResult: ((AccountStatus?) -> Unit)? = null) {
    val uid = authRepository.currentUser?.uid
    if (uid != null) {
      viewModelScope.launch {
        val fetched = userRepository.getUserProfile(uid)
        if (fetched != null) {
          applyAuthenticatedProfile(fetched)
          onResult?.invoke(fetched.status)
        } else {
          onResult?.invoke(null)
        }
      }
    } else {
      onResult?.invoke(null)
    }
  }

  fun signOut(context: Context) {
    val credentialManager = CredentialManager.create(context)
    sessionManager.clearSession()
    phoneNumber.value = ""
    otpCode.value = ""
    isOtpSent.value = false
    clearAuthMessages()
    registrationError.value = null
    phoneVerificationId = null
    phoneResendToken = null
    timerJob?.cancel()
    _uiState.value = AuthUiState.Unauthenticated()

    viewModelScope.launch {
      authRepository.signOut(credentialManager)
    }
  }

  companion object {
    private val OWNER_EMAILS = setOf("samuveldurai6@gmail.com")
  }
}
