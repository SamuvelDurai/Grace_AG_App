package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.AccountStatus
import com.example.core.model.UserProfile
import com.example.core.model.UserRole

class SessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

  fun saveSession(profile: UserProfile) {
    prefs.edit()
      .putBoolean(KEY_IS_LOGGED_IN, true)
      .putString(KEY_UID, profile.uid)
      .putString(KEY_PHONE, profile.phoneNumber)
      .putString(KEY_EMAIL, profile.email)
      .putString(KEY_DISPLAY_NAME, profile.displayName)
      .putString(KEY_ROLE, profile.role)
      .putString(KEY_CHURCH_ID, profile.churchId)
      .putString(KEY_ACCOUNT_STATUS, profile.accountStatus)
      .putString(KEY_MEMBERSHIP_ID, profile.membershipId)
      .putString(KEY_FAMILY_ID, profile.familyId)
      .putString(KEY_PHOTO_URL, profile.photoUrl)
      .putBoolean(KEY_PROFILE_COMPLETED, profile.profileCompleted)
      .apply()
  }

  fun getSavedProfile(): UserProfile? {
    if (!isLoggedIn()) return null
    val uid = prefs.getString(KEY_UID, null)
    if (uid.isNullOrBlank()) return null

    return UserProfile(
      uid = uid,
      phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
      email = prefs.getString(KEY_EMAIL, "") ?: "",
      displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
      role = prefs.getString(KEY_ROLE, UserRole.BELIEVER.name) ?: UserRole.BELIEVER.name,
      churchId = prefs.getString(KEY_CHURCH_ID, "church_main") ?: "church_main",
      accountStatus = prefs.getString(KEY_ACCOUNT_STATUS, AccountStatus.PENDING.name)
        ?: AccountStatus.PENDING.name,
      membershipId = prefs.getString(KEY_MEMBERSHIP_ID, "") ?: "",
      familyId = prefs.getString(KEY_FAMILY_ID, "") ?: "",
      photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: "",
      profileCompleted = prefs.getBoolean(KEY_PROFILE_COMPLETED, false)
    )
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_NAME = "church_connect_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_UID = "uid"
    private const val KEY_PHONE = "phone_number"
    private const val KEY_EMAIL = "email"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_ROLE = "role"
    private const val KEY_CHURCH_ID = "church_id"
    private const val KEY_ACCOUNT_STATUS = "account_status"
    private const val KEY_MEMBERSHIP_ID = "membership_id"
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_PHOTO_URL = "photo_url"
    private const val KEY_PROFILE_COMPLETED = "profile_completed"
  }
}
