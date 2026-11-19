package com.example.data.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AuthRepository {
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithGoogle(
    activity: Activity,
    credentialManager: CredentialManager
  ): Result<FirebaseUser> {
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return Result.failure(IllegalStateException("Web Client ID configuration missing"))
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    return try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(authCredential).await()
        val user = authResult.user ?: throw IllegalStateException("Firebase user was null after sign in")
        Result.success(user)
      } else {
        Result.failure(IllegalArgumentException("Unsupported credential type: ${credential.type}"))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("AuthRepository", "Google Sign-In dismissed or cancelled: ${e.message}")
      Result.failure(e)
    } catch (e: NoCredentialException) {
      Log.w("AuthRepository", "No Google account credentials available: ${e.message}")
      Result.failure(IllegalStateException("No Google account found on this device. Please add a Google account in device Settings or sign in with Phone/Email.", e))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AuthRepository", "Google Sign-In failed", e)
      Result.failure(e)
    }
  }

  fun sendPhoneVerificationCode(
    phoneNumberE164: String,
    activity: Activity,
    resendToken: PhoneAuthProvider.ForceResendingToken?,
    onVerificationCompleted: (PhoneAuthCredential) -> Unit,
    onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
    onVerificationFailed: (String) -> Unit
  ) {
    auth.useAppLanguage()
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        onVerificationCompleted(credential)
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.e("AuthRepository", "Phone verification failed: ${e::class.java.simpleName} - ${e.message}", e)
        onVerificationFailed(mapPhoneAuthError(e))
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        onCodeSent(verificationId, token)
      }

      override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
        Log.d("AuthRepository", "SMS auto-retrieval timed out for $verificationId; waiting for manual OTP entry.")
      }
    }

    val builder = PhoneAuthOptions.newBuilder(auth)
      .setPhoneNumber(phoneNumberE164)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setActivity(activity)
      .setCallbacks(callbacks)

    if (resendToken != null) {
      builder.setForceResendingToken(resendToken)
    }

    PhoneAuthProvider.verifyPhoneNumber(builder.build())
  }

  private fun mapPhoneAuthError(e: Exception): String {
    if (e is com.google.firebase.FirebaseTooManyRequestsException) {
      return "Too many SMS requests from this number or device. Please wait a few minutes and try again."
    }
    if (e is com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException) {
      return "Unable to launch verification check. Please reopen the login screen and try again."
    }
    val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
    return when (code) {
      "ERROR_OPERATION_NOT_ALLOWED" ->
        "Mobile OTP sign-in is not yet enabled in Firebase Console (Authentication → Sign-in method → Phone)."
      "ERROR_INVALID_PHONE_NUMBER" ->
        "Invalid phone number format. Include country code (e.g. +91 9876543210)."
      "ERROR_QUOTA_EXCEEDED", "ERROR_TOO_MANY_REQUESTS" ->
        "SMS verification limit reached temporarily. Please try again later or sign in with Google/Email."
      "ERROR_APP_NOT_AUTHORIZED", "ERROR_MISSING_CLIENT_IDENTIFIER", "ERROR_CAPTCHA_CHECK_FAILED" ->
        "App verification failed (SHA-1/Play Integrity check). Please ensure Phone Auth & SHA-1/SHA-256 are enabled in Firebase Console."
      "ERROR_INVALID_VERIFICATION_CODE" ->
        "Incorrect 6-digit OTP code. Please check the SMS and try again."
      "ERROR_SESSION_EXPIRED" ->
        "OTP expired. Please tap Resend OTP to get a new code."
      else -> e.localizedMessage ?: "SMS verification failed. Please check your mobile number and internet connection."
    }
  }

  suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
    return try {
      val authResult = auth.signInWithCredential(credential).await()
      val user = authResult.user ?: throw IllegalStateException("Firebase user was null after OTP verification")
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AuthRepository", "Phone OTP sign-in failed", e)
      Result.failure(IllegalStateException(mapPhoneAuthError(e), e))
    }
  }

  suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: throw IllegalStateException("Authentication failed: empty user")
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AuthRepository", "Email Sign-In failed", e)
      Result.failure(e)
    }
  }

  suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: throw IllegalStateException("Registration failed: empty user")
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AuthRepository", "Email Sign-Up failed", e)
      Result.failure(e)
    }
  }

  suspend fun sendPasswordReset(email: String): Result<Unit> {
    return try {
      auth.sendPasswordResetEmail(email.trim()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AuthRepository", "Password reset failed", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithPhoneOtpCode(
    verificationId: String,
    smsCode: String
  ): Result<FirebaseUser> {
    val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
    return signInWithPhoneCredential(credential)
  }

  suspend fun signOut(credentialManager: CredentialManager) {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("AuthRepository", "Failed clearing credential state: ${e.message}")
      } finally {
        auth.signOut()
      }
    }
  }
}
