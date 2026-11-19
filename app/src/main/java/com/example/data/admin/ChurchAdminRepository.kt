package com.example.data.admin

import android.content.Context
import com.example.core.model.AccountStatus
import com.example.core.model.AuditLogEntry
import com.example.core.model.ChurchConfig
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.example.core.util.ChurchIdGenerator
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChurchAdminRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val _churchConfigFlow = MutableStateFlow(ChurchConfig())

  private val defaultAccounts = mutableListOf(
    UserProfile(
      uid = "local_pastor_1",
      phoneNumber = "+91 9876543210",
      email = "stephen.paul@gracechurch.org",
      displayName = "Rev. Dr. Stephen Paul",
      role = UserRole.PASTOR.name,
      churchId = "church_main",
      accountStatus = AccountStatus.APPROVED.name,
      membershipId = "GAG-P-2026-001"
    ),
    UserProfile(
      uid = "local_member_1",
      phoneNumber = "9876543210",
      email = "samuel@example.com",
      displayName = "Samuel Durai",
      role = UserRole.BELIEVER.name,
      churchId = "church_main",
      accountStatus = AccountStatus.APPROVED.name,
      membershipId = "GAG-2026-0001"
    )
  )

  private val _usersFlow = MutableStateFlow(defaultAccounts.toList())
  private val defaultLogs = mutableListOf<AuditLogEntry>()
  private val _auditLogsFlow = MutableStateFlow<List<AuditLogEntry>>(emptyList())

  fun observeChurchConfig(churchId: String = "church_main"): Flow<ChurchConfig> {
    if (auth.currentUser == null) {
      return _churchConfigFlow
    }

    val docRef = db.collection("churches").document(churchId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          val parsed = snapshot.toObject(ChurchConfig::class.java)
          if (parsed != null) {
            parsed.copy(churchId = parsed.churchId.ifBlank { churchId })
          } else {
            ChurchConfig(churchId = churchId)
          }
        } else {
          ChurchConfig(churchId = churchId)
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.GET, docRef.path)
        emit(ChurchConfig(churchId = churchId))
      }
  }

  suspend fun saveChurchConfig(
    config: ChurchConfig,
    actorName: String
  ): Result<ChurchConfig> = runCatching {
    val normalized = config.copy(
      churchId = config.churchId.ifBlank { "church_main" },
      name = config.name.trim(),
      address = config.address.trim(),
      phone = config.phone.trim(),
      email = config.email.trim(),
      serviceTimings = config.serviceTimings.trim(),
      defaultLanguage = if (config.defaultLanguage == "ta") "ta" else "en",
      monthlyDuesAmount = config.monthlyDuesAmount.coerceAtLeast(0.0),
      upiId = config.upiId.trim(),
      bankAccountDetails = config.bankAccountDetails.trim(),
      latestVersionCode = config.latestVersionCode.coerceAtLeast(1),
      latestVersionName = config.latestVersionName.trim().ifBlank { "1.0" },
      updateDownloadUrl = config.updateDownloadUrl.trim(),
      updateReleaseNotes = config.updateReleaseNotes.trim(),
      forceUpdate = config.forceUpdate
    )
    require(normalized.name.isNotBlank()) { "Church name is required" }

    _churchConfigFlow.value = normalized

    val currentUser = auth.currentUser
    if (currentUser != null) {
      val docRef = db.collection("churches").document(normalized.churchId)
      val payload = hashMapOf<String, Any?>(
        "churchId" to normalized.churchId,
        "name" to normalized.name,
        "address" to normalized.address,
        "phone" to normalized.phone,
        "email" to normalized.email,
        "defaultLanguage" to normalized.defaultLanguage,
        "serviceTimings" to normalized.serviceTimings,
        "monthlyDuesAmount" to normalized.monthlyDuesAmount,
        "upiId" to normalized.upiId,
        "bankAccountDetails" to normalized.bankAccountDetails,
        "latestVersionCode" to normalized.latestVersionCode,
        "latestVersionName" to normalized.latestVersionName,
        "updateDownloadUrl" to normalized.updateDownloadUrl,
        "updateReleaseNotes" to normalized.updateReleaseNotes,
        "forceUpdate" to normalized.forceUpdate,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload, SetOptions.merge()).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        throw e
      }
    }

    recordAuditLog(
      action = "Updated church profile (${normalized.name}), dues (₹${normalized.monthlyDuesAmount.toInt()}) & app release v${normalized.latestVersionName} (${normalized.latestVersionCode})",
      actorName = actorName,
      churchId = normalized.churchId
    )

    normalized
  }

  fun observeManagedUsers(churchId: String = "church_main"): Flow<List<UserProfile>> {
    if (auth.currentUser == null) {
      return _usersFlow
    }

    return db.collection("users")
      .whereEqualTo("churchId", churchId)
      .limit(100)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(UserProfile::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "users")
        emit(emptyList())
      }
  }

  suspend fun createPastorAccount(
    context: Context,
    fullName: String,
    email: String,
    phoneNumber: String,
    password: String,
    actorName: String,
    churchId: String = "church_main"
  ): Result<UserProfile> = runCatching {
    val cleanName = fullName.trim()
    val cleanEmail = email.trim()
    val emailLower = cleanEmail.lowercase()
    val cleanDigits = phoneNumber.filter { it.isDigit() }
    val normalizedPhone = when {
      phoneNumber.trim().startsWith("+") && cleanDigits.length >= 10 -> "+$cleanDigits"
      cleanDigits.length >= 10 -> "+91${cleanDigits.takeLast(10)}"
      else -> phoneNumber.trim()
    }

    require(cleanName.isNotBlank()) { "Pastor's full name is required." }
    require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Enter a valid pastor email address." }
    require(cleanDigits.length >= 10) { "Enter a valid 10-digit mobile number." }
    require(password.length >= 6) { "Temporary password must be at least 6 characters." }

    val membershipId = ChurchIdGenerator.nextPastorId(db, churchId)

    if (auth.currentUser == null) {
      val offlineProfile = UserProfile(
        uid = "pastor_${UUID.randomUUID().toString().take(8)}",
        phoneNumber = normalizedPhone,
        email = cleanEmail,
        displayName = cleanName,
        role = UserRole.PASTOR.name,
        churchId = churchId,
        accountStatus = AccountStatus.APPROVED.name,
        membershipId = membershipId,
        profileCompleted = true
      )
      defaultAccounts.add(0, offlineProfile)
      _usersFlow.value = defaultAccounts.toList()
      recordAuditLog(
        action = "Created Pastor account: $cleanName ($cleanEmail)",
        actorName = actorName,
        churchId = churchId
      )
      return@runCatching offlineProfile
    }

    val defaultApp = FirebaseApp.getInstance()
    val existingProvisionApp = FirebaseApp.getApps(context.applicationContext)
      .firstOrNull { it.name == PASTOR_PROVISIONER_APP }
    val provisionApp = existingProvisionApp
      ?: FirebaseApp.initializeApp(context.applicationContext, defaultApp.options, PASTOR_PROVISIONER_APP)
    val secondaryAuth = FirebaseAuth.getInstance(provisionApp)
    if (existingProvisionApp == null && defaultApp.options.projectId == "demo-no-project") {
      try {
        secondaryAuth.useEmulator("127.0.0.1", 9099)
      } catch (_: IllegalStateException) {
        // Already configured
      }
    }

    var createdUid: String? = null
    try {
      val authResult = secondaryAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
      createdUid = authResult.user?.uid
    } catch (_: FirebaseAuthUserCollisionException) {
      // User already exists in Firebase Auth; save pastor invite so login/promotion succeeds cleanly
      createdUid = null
    } finally {
      secondaryAuth.signOut()
    }

    // Store invite keys so Pastor login works via Email/Password, Google Sign-In, or Phone OTP
    val invitePayload = hashMapOf<String, Any?>(
      "inviteId" to "email_$emailLower",
      "churchId" to churchId,
      "displayName" to cleanName,
      "email" to cleanEmail,
      "emailLower" to emailLower,
      "phoneNormalized" to normalizedPhone,
      "role" to UserRole.PASTOR.name,
      "accountStatus" to AccountStatus.APPROVED.name,
      "membershipId" to membershipId,
      "createdBy" to (auth.currentUser?.uid ?: ""),
      "createdAt" to FieldValue.serverTimestamp()
    )
    db.collection("pastorInvites").document("email_$emailLower").set(invitePayload).await()
    if (normalizedPhone.startsWith("+")) {
      db.collection("pastorInvites").document("phone_$normalizedPhone")
        .set(invitePayload.toMutableMap().apply { put("inviteId", "phone_$normalizedPhone") })
        .await()
    }

    val pastorProfile = if (!createdUid.isNullOrBlank()) {
      val profile = UserProfile(
        uid = createdUid,
        phoneNumber = normalizedPhone,
        email = cleanEmail,
        displayName = cleanName,
        role = UserRole.PASTOR.name,
        churchId = churchId,
        accountStatus = AccountStatus.APPROVED.name,
        membershipId = membershipId,
        profileCompleted = true
      )
      val docRef = db.collection("users").document(createdUid)
      val payload = hashMapOf<String, Any?>(
        "uid" to createdUid,
        "phoneNumber" to normalizedPhone,
        "email" to cleanEmail,
        "displayName" to cleanName,
        "role" to UserRole.PASTOR.name,
        "churchId" to churchId,
        "accountStatus" to AccountStatus.APPROVED.name,
        "membershipId" to membershipId,
        "photoUrl" to "",
        "profileCompleted" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
      profile
    } else {
      // Check if an existing user doc matches this email and upgrade it to PASTOR + APPROVED
      val existingMatch = db.collection("users")
        .whereEqualTo("churchId", churchId)
        .limit(100)
        .get()
        .await()
        .documents
        .firstOrNull { (it.getString("email") ?: "").equals(cleanEmail, ignoreCase = true) }

      if (existingMatch != null) {
        existingMatch.reference.update(
          mapOf(
            "role" to UserRole.PASTOR.name,
            "accountStatus" to AccountStatus.APPROVED.name,
            "profileCompleted" to true,
            "displayName" to cleanName,
            "phoneNumber" to normalizedPhone,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      UserProfile(
        uid = existingMatch?.id ?: "invite_$emailLower",
        phoneNumber = normalizedPhone,
        email = cleanEmail,
        displayName = cleanName,
        role = UserRole.PASTOR.name,
        churchId = churchId,
        accountStatus = AccountStatus.APPROVED.name,
        membershipId = membershipId,
        profileCompleted = true
      )
    }

    recordAuditLog(
      action = "Created Pastor account: $cleanName ($cleanEmail)",
      actorName = actorName,
      churchId = churchId
    )
    pastorProfile
  }

  suspend fun updateUserRoleAndStatus(
    uid: String,
    role: UserRole,
    status: AccountStatus,
    actorName: String,
    churchId: String = "church_main"
  ): Result<Unit> = runCatching {
    var targetName = uid

    if (auth.currentUser != null) {
      val docRef = db.collection("users").document(uid)
      try {
        val snap = docRef.get().await()
        val fetchedName = snap.getString("displayName")
        if (!fetchedName.isNullOrBlank()) {
          targetName = fetchedName
        }
        val payload = mapOf(
          "role" to role.name,
          "accountStatus" to status.name,
          "profileCompleted" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()

        if (status == AccountStatus.APPROVED) {
          val believerRef = db.collection("believers").document("mem_$uid")
          val believerSnap = believerRef.get().await()
          if (believerSnap.exists()) {
            believerRef.update(
              mapOf(
                "membershipStatus" to "ACTIVE",
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          }
          val appRef = db.collection("membershipApplications").document(uid)
          val appSnap = appRef.get().await()
          if (appSnap.exists()) {
            appRef.update(
              mapOf(
                "status" to "APPROVED",
                "reviewedBy" to (auth.currentUser?.uid ?: ""),
                "reviewedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    } else {
      val idx = defaultAccounts.indexOfFirst { it.uid == uid }
      if (idx >= 0) {
        val updated = defaultAccounts[idx].copy(role = role.name, accountStatus = status.name)
        defaultAccounts[idx] = updated
        _usersFlow.value = defaultAccounts.toList()
        targetName = updated.displayName
      }
    }

    recordAuditLog(
      action = "Updated account $targetName → Role: ${role.name}, Status: ${status.name}",
      actorName = actorName,
      churchId = churchId
    )
  }

  fun observeAuditLogs(churchId: String = "church_main"): Flow<List<AuditLogEntry>> {
    if (auth.currentUser == null) {
      return _auditLogsFlow
    }

    return db.collection("auditLogs")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(AuditLogEntry::class.java) }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "auditLogs")
        emit(emptyList())
      }
  }

  suspend fun recordAuditLog(
    action: String,
    actorName: String,
    status: String = "SUCCESS",
    churchId: String = "church_main"
  ): Result<AuditLogEntry> = runCatching {
    val currentUser = auth.currentUser
    val timestampText = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    val entry = AuditLogEntry(
      logId = "log_${UUID.randomUUID().toString().take(12)}",
      churchId = churchId,
      action = action,
      actorName = actorName.ifBlank { "Church Admin" },
      actorUid = currentUser?.uid ?: "local_admin",
      timestampText = timestampText,
      status = status
    )

    defaultLogs.add(0, entry)
    _auditLogsFlow.value = defaultLogs.toList()

    if (currentUser != null) {
      val docRef = db.collection("auditLogs").document(entry.logId)
      val payload = hashMapOf<String, Any?>(
        "logId" to entry.logId,
        "churchId" to entry.churchId,
        "action" to entry.action,
        "actorName" to entry.actorName,
        "actorUid" to currentUser.uid,
        "timestampText" to entry.timestampText,
        "status" to entry.status,
        "createdAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }

    entry
  }

  private companion object {
    const val PASTOR_PROVISIONER_APP = "PastorProvisionerApp"
  }
}
