package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.localization.LanguageManager
import com.example.core.model.AccountStatus
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.example.core.util.findActivity
import com.example.data.auth.SessionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      val app = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.aistudio.churchconnect.chcapp")
          .setProjectId("demo-no-project")
          .setApiKey("fake-api-key-for-emulator")
          .build()
      )
      val dbId = context.getString(R.string.firestore_database_id)
      val firestore = FirebaseFirestore.getInstance(app, dbId)
      firestore.useEmulator("127.0.0.1", 8085)
      firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
        .build()
      FirebaseAuth.getInstance(app).useEmulator("127.0.0.1", 9099)
    }
  }

  @Test
  fun `read string from context verifies GRACE AG app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GRACE AG", appName)
  }

  @Test
  fun `user role parsing defaults to Believer on null or unknown`() {
    assertEquals(UserRole.BELIEVER, UserRole.fromString(null))
    assertEquals(UserRole.PASTOR, UserRole.fromString("PASTOR"))
    assertEquals(UserRole.SUPER_ADMIN, UserRole.fromString("SUPER_ADMIN"))
    assertEquals(UserRole.BELIEVER, UserRole.fromString("UNKNOWN"))
  }

  @Test
  fun `account status parsing defaults to Pending on null`() {
    assertEquals(AccountStatus.PENDING, AccountStatus.fromString(null))
    assertEquals(AccountStatus.APPROVED, AccountStatus.fromString("APPROVED"))
    assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromString("SUSPENDED"))
    assertEquals(AccountStatus.REJECTED, AccountStatus.fromString("REJECTED"))
  }

  @Test
  fun `user profile model instantiation has valid defaults`() {
    val profile = UserProfile(
      uid = "test_user_1",
      displayName = "John Doe",
      role = UserRole.BELIEVER.name
    )
    assertEquals("test_user_1", profile.uid)
    assertEquals(UserRole.BELIEVER, profile.userRole)
    assertEquals(AccountStatus.PENDING, profile.status)
    assertEquals("church_main", profile.churchId)
  }

  @Test
  fun `language manager switches and persists language`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = LanguageManager.getInstance(context)
    manager.setLanguage(LanguageManager.LANG_TAMIL)
    assertEquals(LanguageManager.LANG_TAMIL, manager.currentLanguage.value)

    val localizedTamilContext = manager.applyLocale(context)
    val tamilGreeting = localizedTamilContext.getString(R.string.greeting_hello)
    assertEquals("கர்த்தருக்குள் சமாதானம்,", tamilGreeting)

    manager.setLanguage(LanguageManager.LANG_ENGLISH)
    assertEquals(LanguageManager.LANG_ENGLISH, manager.currentLanguage.value)
    val localizedEnglishContext = manager.applyLocale(context)
    val englishGreeting = localizedEnglishContext.getString(R.string.greeting_hello)
    assertEquals("Peace be with you,", englishGreeting)
  }

  @Test
  fun `believer member model and verification token validation`() {
    val member = com.example.core.model.BelieverMember(
      memberId = "mem_100",
      uid = "u_100",
      membershipId = "CC-00100",
      fullName = "Samuel Durai",
      phoneNumber = "9876543210",
      verificationToken = "CC-VERIFY-100-XYZ"
    )
    assertEquals("CC-00100", member.membershipId)
    assertEquals("Samuel Durai", member.fullName)
    assertEquals("CC-VERIFY-100-XYZ", member.verificationToken)
  }

  @Test
  fun `qr code generator produces matrix from verification token`() {
    val matrix = com.example.core.util.QrCodeGenerator.generateMatrix("CC-VERIFY-TOKEN-TEST", 25)
    assertNotNull(matrix)
    assertEquals(25, matrix.size)
    assertEquals(25, matrix[0].size)
  }

  @Test
  fun `prayer request model instantiation retains confidentiality`() {
    val req = com.example.core.model.PrayerRequest(
      requestId = "pr_test",
      title = "Health and peace",
      privacy = com.example.core.model.PrayerPrivacy.PASTOR_ONLY,
      isAnonymous = true
    )
    assertEquals("pr_test", req.requestId)
    assertEquals(com.example.core.model.PrayerPrivacy.PASTOR_ONLY, req.privacy)
    assertEquals(true, req.isAnonymous)
  }

  @Test
  fun `offering transaction generates valid receipt number format`() {
    val tx = com.example.core.model.OfferingTransaction(
      transactionId = "tx_test",
      category = com.example.core.model.OfferingCategory.TITHE,
      amount = 2500.0,
      receiptNumber = "REC-TITHE-123456"
    )
    assertEquals(2500.0, tx.amount, 0.01)
    assert(tx.receiptNumber.startsWith("REC-TITHE-"))
  }

  @Test
  fun `fellowship repository provides area care cells and ministries`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.fellowship.FellowshipRepository(context)
    val allGroups = repo.observeFellowshipGroups().first()
    assert(allGroups.isNotEmpty())
    val youthGroups = repo.observeGroupsByCategory(
      com.example.core.model.FellowshipCategory.YOUTH_MINISTRY
    ).first()
    assertEquals(1, youthGroups.size)
    assertEquals("Ignite Christian Youth Fellowship (CYA)", youthGroups.first().nameEnglish)
  }

  @Test
  fun `fellowship repository supports pastor create edit member assignment and delete`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.fellowship.FellowshipRepository(context)
    val initialCount = repo.observeFellowshipGroups().first().size

    val created = repo.createGroup(
      com.example.core.model.FellowshipGroup(
        nameEnglish = "Sanctuary Praise Choir",
        nameTamil = "ஆலய பாடகர் குழு",
        areaZone = "Main Sanctuary",
        leaderName = "Bro. Victor",
        leaderPhone = "9876501234",
        meetingSchedule = "Every Saturday • 4:00 PM",
        meetingLocation = "Choir Loft",
        category = com.example.core.model.FellowshipCategory.CHOIR_WORSHIP
      )
    ).getOrThrow()

    assertEquals(initialCount + 1, repo.observeFellowshipGroups().first().size)
    assertEquals(com.example.core.model.FellowshipCategory.CHOIR_WORSHIP, created.category)

    repo.updateGroup(created.copy(leaderName = "Sis. Esther", category = com.example.core.model.FellowshipCategory.OTHER_GROUP))
    val afterUpdate = repo.observeFellowshipGroups().first().first { it.groupId == created.groupId }
    assertEquals("Sis. Esther", afterUpdate.leaderName)
    assertEquals(com.example.core.model.FellowshipCategory.OTHER_GROUP, afterUpdate.category)

    repo.updateGroupMembers(created.groupId, listOf("mem_1", "mem_2", "mem_2"))
    val afterMembers = repo.observeFellowshipGroups().first().first { it.groupId == created.groupId }
    assertEquals(listOf("mem_1", "mem_2"), afterMembers.memberIds)
    assertEquals(2, afterMembers.memberCount)

    repo.deleteGroup(created.groupId)
    val afterDelete = repo.observeFellowshipGroups().first()
    assertEquals(initialCount, afterDelete.size)
    assertFalse(afterDelete.any { it.groupId == created.groupId })
  }

  @Test
  fun `church event repository supports pastor create edit attendee assignment and delete`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.event.ChurchEventRepository(context)
    val initialCount = repo.observeEvents().first().size

    val created = repo.createEvent(
      com.example.core.model.ChurchEvent(
        titleEnglish = "Good Friday Worship Service",
        titleTamil = "புனித வெள்ளி ஆராதனை",
        eventDate = "Friday, 03 Apr 2026",
        startTime = "09:00 AM",
        endTime = "12:00 PM",
        venue = "Main Sanctuary",
        capacity = 250,
        category = com.example.core.model.EventCategory.SPECIAL_FESTIVAL
      )
    ).getOrThrow()

    assertEquals(initialCount + 1, repo.observeEvents().first().size)
    assertEquals(com.example.core.model.EventCategory.SPECIAL_FESTIVAL, created.category)

    repo.updateEvent(created.copy(venue = "Grace Hall", category = com.example.core.model.EventCategory.SUNDAY_SERVICE))
    val afterUpdate = repo.observeEvents().first().first { it.eventId == created.eventId }
    assertEquals("Grace Hall", afterUpdate.venue)
    assertEquals(com.example.core.model.EventCategory.SUNDAY_SERVICE, afterUpdate.category)

    repo.updateEventAttendees(created.eventId, listOf("mem_1", "mem_2", "mem_2"))
    val afterAttendees = repo.observeEvents().first().first { it.eventId == created.eventId }
    assertEquals(listOf("mem_1", "mem_2"), afterAttendees.attendeeIds)
    assertEquals(2, afterAttendees.registeredCount)

    repo.deleteEvent(created.eventId)
    val afterDelete = repo.observeEvents().first()
    assertEquals(initialCount, afterDelete.size)
    assertFalse(afterDelete.any { it.eventId == created.eventId })
  }

  @Test
  fun `pastoral care repository tracks visit requests and generates bilingual blessings`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.pastoralcare.PastoralCareRepository(context)
    val initialAlerts = repo.observeAlerts().first()
    val initialCount = initialAlerts.size
    assert(initialCount >= 3)

    val birthdayAlert = initialAlerts.first { it.careType == com.example.core.model.PastoralCareType.BIRTHDAY }
    val enBlessing = birthdayAlert.generateBlessingMessage(isTamil = false)
    val taBlessing = birthdayAlert.generateBlessingMessage(isTamil = true)
    assert(enBlessing.contains("Numbers 6:24"))
    assert(taBlessing.contains("எண்ணாகமம் 6:24"))

    repo.requestPastoralVisit(
      memberUid = "u_new",
      memberName = "Bro. Peter",
      memberPhone = "+91 90000 11111",
      membershipId = "CC-00250",
      careType = com.example.core.model.PastoralCareType.HOSPITAL_VISIT,
      notes = "Pray for speedy recovery",
      locationDetails = "Room 201, CMC Vellore"
    )
    val updatedAlerts = repo.observeAlerts().first()
    assertEquals(initialCount + 1, updatedAlerts.size)
    assertEquals("Bro. Peter", updatedAlerts.first().memberName)

    repo.updateAlertStatus(updatedAlerts.first().alertId, com.example.core.model.PastoralCareStatus.VISITED)
    val afterStatus = repo.observeAlerts().first()
    assertEquals(com.example.core.model.PastoralCareStatus.VISITED, afterStatus.first().status)
  }

  @Test
  fun `session manager persists login across instances until explicit clear`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val firstSessionManager = SessionManager(context)
    firstSessionManager.clearSession()
    assertFalse(firstSessionManager.isLoggedIn())
    assertNull(firstSessionManager.getSavedProfile())

    val profile = UserProfile(
      uid = "member_9876543210",
      phoneNumber = "9876543210",
      email = "grace@churchconnect.org",
      displayName = "Grace Jebaraj",
      role = UserRole.PASTOR.name,
      churchId = "church_main",
      accountStatus = AccountStatus.APPROVED.name,
      membershipId = "CC-2048",
      familyId = "fam_grace_987",
      photoUrl = "data:image/jpeg;base64,/9j/4AAQ",
      profileCompleted = true
    )
    firstSessionManager.saveSession(profile)

    // Simulate fresh app launch with a new SessionManager instance
    val restoredSessionManager = SessionManager(context)
    assertTrue(restoredSessionManager.isLoggedIn())
    val restoredProfile = restoredSessionManager.getSavedProfile()
    assertNotNull(restoredProfile)
    assertEquals("member_9876543210", restoredProfile?.uid)
    assertEquals("Grace Jebaraj", restoredProfile?.displayName)
    assertEquals("9876543210", restoredProfile?.phoneNumber)
    assertEquals("fam_grace_987", restoredProfile?.familyId)
    assertEquals("data:image/jpeg;base64,/9j/4AAQ", restoredProfile?.photoUrl)
    assertEquals(AccountStatus.APPROVED, restoredProfile?.status)
    assertEquals(UserRole.PASTOR, restoredProfile?.userRole)
    assertTrue(restoredProfile?.profileCompleted == true)

    // Simulate explicit user logout
    restoredSessionManager.clearSession()
    val afterLogoutSessionManager = SessionManager(context)
    assertFalse(afterLogoutSessionManager.isLoggedIn())
    assertNull(afterLogoutSessionManager.getSavedProfile())
  }

  @Test
  fun `csv export helper formats members and finance records cleanly`() {
    val members = listOf(
      com.example.core.model.BelieverMember(
        memberId = "m1",
        uid = "u1",
        membershipId = "CC-00101",
        fullName = "Samuel Durai",
        phoneNumber = "9876543210",
        address = "42, Cross Street, Chennai",
        baptismStatus = "BAPTIZED"
      )
    )
    val membersCsv = com.example.core.util.CsvExportHelper.buildMembersCsv(members)
    assertTrue(membersCsv.contains("Membership ID,Full Name"))
    assertTrue(membersCsv.contains("CC-00101,Samuel Durai,9876543210"))
    assertTrue(membersCsv.contains("\"42, Cross Street, Chennai\""))

    val offerings = listOf(
      com.example.core.model.OfferingTransaction(
        transactionId = "tx1",
        donorName = "Grace",
        membershipId = "CC-00102",
        category = com.example.core.model.OfferingCategory.TITHE,
        amount = 1500.0,
        receiptNumber = "REC-TITHE-101"
      )
    )
    val financeCsv = com.example.core.util.CsvExportHelper.buildFinanceCsv(offerings, emptyList())
    assertTrue(financeCsv.contains("REC-TITHE-101,Grace,CC-00102,TITHE,1500"))
  }

  @Test
  fun `church admin repository saves church config updates accounts and records audit logs`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.admin.ChurchAdminRepository(context)
    val initialConfig = repo.observeChurchConfig().first()
    assertEquals("GRACE AG", initialConfig.name)

    val saved = repo.saveChurchConfig(
      initialConfig.copy(name = "Grace Cathedral", monthlyDuesAmount = 650.0),
      actorName = "Super Admin"
    ).getOrThrow()
    assertEquals("Grace Cathedral", saved.name)
    assertEquals(650.0, repo.observeChurchConfig().first().monthlyDuesAmount, 0.01)

    repo.updateUserRoleAndStatus(
      uid = "local_member_1",
      role = UserRole.PASTOR,
      status = AccountStatus.APPROVED,
      actorName = "Super Admin"
    ).getOrThrow()
    val updatedUser = repo.observeManagedUsers().first().first { it.uid == "local_member_1" }
    assertEquals(UserRole.PASTOR, updatedUser.userRole)

    val logs = repo.observeAuditLogs().first()
    assertTrue(logs.any { it.action.contains("Grace Cathedral") })
    assertTrue(logs.any { it.action.contains("PASTOR") })
  }

  @Test
  fun `sermon and announcement repositories support publish and delete`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sermonRepo = com.example.data.sermon.SermonRepository(context)
    val initialSermonCount = sermonRepo.observeSermons().first().size

    val sermon = sermonRepo.publishSermon(
      titleEn = "Faith Over Fear",
      titleTa = "பயத்தை வெல்லும் விசுவாசம்",
      speaker = "Pastor Thomas",
      ref = "2 Timothy 1:7",
      notes = "Spirit of power and love",
      videoUrl = ""
    ).getOrThrow()
    assertEquals(initialSermonCount + 1, sermonRepo.observeSermons().first().size)

    sermonRepo.deleteSermon(sermon.sermonId)
    assertEquals(initialSermonCount, sermonRepo.observeSermons().first().size)
    assertFalse(sermonRepo.observeSermons().first().any { it.sermonId == sermon.sermonId })

    val annRepo = com.example.data.announcement.AnnouncementRepository(context)
    val initialAnnCount = annRepo.observeAnnouncements().first().size
    val ann = annRepo.publishAnnouncement(
      titleEn = "Harvest Thanksgiving",
      titleTa = "அறுவடை நன்றிப் பண்டிகை",
      bodyEn = "First Sunday next month",
      bodyTa = "அடுத்த மாதம் முதல் ஞாயிறு",
      category = com.example.core.model.AnnouncementCategory.SPECIAL_EVENT,
      author = "Pastor Office"
    ).getOrThrow()
    assertEquals(initialAnnCount + 1, annRepo.observeAnnouncements().first().size)

    annRepo.deleteAnnouncement(ann.announcementId)
    assertEquals(initialAnnCount, annRepo.observeAnnouncements().first().size)
    assertFalse(annRepo.observeAnnouncements().first().any { it.announcementId == ann.announcementId })
  }

  @Test
  fun `phone number helper sanitizes national input and formats E164 across countries`() {
    val helper = com.example.core.util.PhoneNumberHelper
    assertEquals("9876543210", helper.sanitizeNationalInput("+91 98765 43210", "+91"))
    assertEquals("9876543210", helper.sanitizeNationalInput("09876543210", "+91"))
    assertEquals("9876543210", helper.sanitizeNationalInput("919876543210", "+91"))
    assertEquals("+919876543210", helper.toE164OrNull("9876543210", "+91"))
    assertEquals("+14155551234", helper.toE164OrNull("(415) 555-1234", "+1"))
    assertEquals("+6591234567", helper.toE164OrNull("9123 4567", "+65"))
    assertNull(helper.toE164OrNull("12345", "+91"))
  }

  @Test
  fun `findActivity unwraps nested ContextWrapper around Activity`() {
    val activity = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).get()
    val wrapped = android.content.ContextWrapper(android.content.ContextWrapper(activity))
    assertEquals(activity, wrapped.findActivity())
  }

  @Test
  fun `believer member family helpers and csv export include family columns`() {
    val member = com.example.core.model.BelieverMember(
      memberId = "m_fam_1",
      uid = "u_fam_1",
      membershipId = "CC-00777",
      fullName = "Daniel Raj",
      phoneNumber = "9876543210",
      familyId = "fam_u_fam_1",
      familyName = "Daniel Raj Family",
      familyMembers = listOf(
        com.example.core.model.FamilyMemberEntry(id = "f1", name = "Sarah", relationship = "Spouse"),
        com.example.core.model.FamilyMemberEntry(id = "f2", name = "Caleb", relationship = "Son")
      )
    )
    assertEquals("fam_u_fam_1", member.effectiveFamilyId())
    assertEquals("Daniel Raj Family", member.effectiveFamilyName())
    assertEquals(3, member.totalFamilyMembersCount())

    val csv = com.example.core.util.CsvExportHelper.buildMembersCsv(listOf(member))
    assertTrue(csv.contains("Family Name,Family ID,Family Size"))
    assertTrue(csv.contains("Daniel Raj Family,fam_u_fam_1,3"))

    val financeCsv = com.example.core.util.CsvExportHelper.buildFinanceCsv(
      offerings = emptyList(),
      subscriptions = listOf(
        com.example.core.model.MonthlySubscription(
          subscriptionId = "sub_1",
          familyId = "fam_u_fam_1",
          familyName = "Daniel Raj Family",
          familyMemberCount = 3,
          memberName = "Daniel Raj",
          membershipId = "CC-00777",
          monthYear = "November 2026",
          amountDue = 500.0,
          amountPaid = 500.0,
          status = com.example.core.model.SubscriptionStatus.PAID,
          dueDate = "10 Nov 2026",
          paidDate = "05 Nov 2026",
          paidByName = "Sarah Daniel",
          receiptNumber = "REC-FAM-001"
        )
      )
    )
    assertTrue(financeCsv.contains("Family Name,Family ID,Members,Head Member"))
    assertTrue(financeCsv.contains("Daniel Raj Family,fam_u_fam_1,3,Daniel Raj"))
    assertTrue(financeCsv.contains("Sarah Daniel,REC-FAM-001"))
  }

  @Test
  fun `finance repository generates strictly one subscription per family and marks shared payment`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val financeRepo = com.example.data.finance.FinanceRepository(context)
    val members = listOf(
      com.example.core.model.BelieverMember(
        memberId = "m_head",
        uid = "u_head",
        membershipId = "CC-1001",
        fullName = "Isaac Thomas",
        familyId = "fam_thomas",
        familyName = "Thomas Household",
        familyRole = "FAMILY_HEAD",
        familyMembers = listOf(
          com.example.core.model.FamilyMemberEntry(id = "fm1", name = "Rebecca Thomas", relationship = "Spouse"),
          com.example.core.model.FamilyMemberEntry(id = "fm2", name = "Joel Thomas", relationship = "Son")
        )
      ),
      com.example.core.model.BelieverMember(
        memberId = "m_spouse",
        uid = "u_spouse",
        membershipId = "CC-1002",
        fullName = "Rebecca Thomas",
        familyId = "fam_thomas",
        familyName = "Thomas Household",
        familyRole = "FAMILY_MEMBER"
      )
    )

    val created = financeRepo.generateMonthlyFamilySubscriptions(
      members = members,
      amountDue = 600.0,
      monthKey = "2026_12",
      monthYear = "December 2026"
    ).getOrThrow()
    assertEquals(1, created)

    // Calling again for same month creates 0 duplicate bills
    val duplicateRun = financeRepo.generateMonthlyFamilySubscriptions(
      members = members,
      amountDue = 600.0,
      monthKey = "2026_12",
      monthYear = "December 2026"
    ).getOrThrow()
    assertEquals(0, duplicateRun)

    val subs = financeRepo.observeUserSubscriptions("u_spouse", "fam_thomas").first()
    val decBill = subs.first { it.familyId == "fam_thomas" && it.monthYear == "December 2026" }
    assertEquals(3, decBill.familyMemberCount)
    assertEquals(com.example.core.model.SubscriptionStatus.PENDING, decBill.status)

    com.example.core.util.ChurchIdGenerator.resetLocalCountersForTest()
    val submitted = financeRepo.submitFamilySubscriptionPayment(
      existingSubscriptionId = decBill.subscriptionId,
      familyId = "fam_thomas",
      familyName = "Thomas Household",
      familyMemberCount = 3,
      payerUid = "u_spouse",
      payerName = "Rebecca Thomas",
      membershipId = "CC-1002",
      amountDue = 600.0,
      paymentMethod = com.example.core.model.PaymentMethod.UPI_ONLINE,
      transactionReference = "UTR55667788"
    ).getOrThrow()
    assertEquals(com.example.core.model.SubscriptionStatus.PENDING_VERIFICATION, submitted.status)
    assertEquals(0.0, submitted.amountPaid, 0.01)
    assertTrue(submitted.receiptNumber.startsWith("REQ-SUB-"))
    assertTrue(submitted.receiptNumber.endsWith("-0001"))

    val verifiedPaid = financeRepo.paySubscription(
      subscriptionId = submitted.subscriptionId,
      paidByUid = "u_spouse",
      paidByName = "Rebecca Thomas",
      paymentMethod = com.example.core.model.PaymentMethod.UPI_ONLINE.name
    ).getOrThrow()
    assertEquals(com.example.core.model.SubscriptionStatus.PAID, verifiedPaid.status)
    assertEquals(600.0, verifiedPaid.amountPaid, 0.01)
    assertEquals("Rebecca Thomas", verifiedPaid.paidByName)
    assertEquals("u_spouse", verifiedPaid.paidByUid)
    assertTrue(verifiedPaid.receiptNumber.startsWith("REC-SUB-"))
    assertTrue(verifiedPaid.receiptNumber.endsWith("-0001"))
  }

  @Test
  fun `church id generator produces sequential non-randomized ids and offerings require verification`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.core.util.ChurchIdGenerator.resetLocalCountersForTest()
    val db = com.example.core.util.FirestoreProvider.getInstance(context)

    val m1 = com.example.core.util.ChurchIdGenerator.nextMembershipId(db, "church_main")
    val m2 = com.example.core.util.ChurchIdGenerator.nextMembershipId(db, "church_main")
    val p1 = com.example.core.util.ChurchIdGenerator.nextPastorId(db, "church_main")
    val p2 = com.example.core.util.ChurchIdGenerator.nextPastorId(db, "church_main")

    assertTrue(m1.endsWith("-0001"))
    assertTrue(m2.endsWith("-0002"))
    assertTrue(m1.startsWith("GAG-"))
    assertTrue(p1.startsWith("GAG-P-") && p1.endsWith("-001"))
    assertTrue(p2.startsWith("GAG-P-") && p2.endsWith("-002"))
    assertEquals("GAG-VERIFY-$m1", com.example.core.util.ChurchIdGenerator.verificationTokenFor(m1))

    val financeRepo = com.example.data.finance.FinanceRepository(context)
    val missingRefResult = financeRepo.submitOffering(
      uid = "u_believer",
      donorName = "Grace",
      membershipId = m1,
      category = com.example.core.model.OfferingCategory.TITHE,
      amount = 1200.0,
      paymentMethod = com.example.core.model.PaymentMethod.UPI_ONLINE,
      note = "Tithe",
      transactionReference = ""
    )
    assertTrue(missingRefResult.isFailure)

    val pendingTx = financeRepo.submitOffering(
      uid = "u_believer",
      donorName = "Grace",
      membershipId = m1,
      category = com.example.core.model.OfferingCategory.TITHE,
      amount = 1200.0,
      paymentMethod = com.example.core.model.PaymentMethod.UPI_ONLINE,
      note = "Tithe",
      transactionReference = "UTR90807060"
    ).getOrThrow()
    assertEquals(com.example.core.model.PaymentTransactionStatus.PENDING, pendingTx.status)
    assertTrue(pendingTx.receiptNumber.startsWith("REQ-TITH-"))
    assertTrue(pendingTx.receiptNumber.endsWith("-0001"))

    val verifiedTx = financeRepo.verifyOfferingTransaction(pendingTx.transactionId, approve = true).getOrThrow()
    assertEquals(com.example.core.model.PaymentTransactionStatus.SUCCESS, verifiedTx.status)
    assertTrue(verifiedTx.receiptNumber.startsWith("REC-TITH-"))
    assertTrue(verifiedTx.receiptNumber.endsWith("-0001"))
  }

  @Test
  fun `family viewmodel manages household head dependent members and shared subscription status`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val familyRepo = com.example.data.family.FamilyRepository(context)
    val viewModel = com.example.presentation.family.FamilyViewModel(familyRepo)

    val initialFamily = com.example.core.model.Family(
      familyId = "fam_vm_test",
      churchId = "church_main",
      familyName = "Jacob Household",
      headUid = "u_jacob",
      headMemberId = "mem_jacob",
      headName = "David Jacob",
      headPhone = "+919876543210",
      sharedSubscriptionStatus = com.example.core.model.SubscriptionStatus.PENDING
    )

    viewModel.createOrUpdateFamily(initialFamily)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    var current = familyRepo.getFamilyById("fam_vm_test")
    assertNotNull(current)
    assertEquals("David Jacob", current?.headName)
    assertEquals(com.example.core.model.SubscriptionStatus.PENDING, current?.sharedSubscriptionStatus)

    viewModel.addDependentMember(
      familyId = "fam_vm_test",
      name = "Luke Jacob",
      relationship = "Son",
      gender = "Male"
    )
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    current = familyRepo.getFamilyById("fam_vm_test")
    assertEquals(1, current?.dependentMembers?.size)
    assertEquals(2, current?.totalHouseholdCount())

    val depId = current!!.dependentMembers.first().id
    viewModel.linkMemberAccount(familyId = "fam_vm_test", memberUid = "u_mary_jacob")
    viewModel.updateSharedSubscriptionStatus(
      familyId = "fam_vm_test",
      status = com.example.core.model.SubscriptionStatus.PAID,
      subscriptionId = "sub_fam_vm_test_2026_11",
      monthYear = "November 2026",
      paidDate = "17 Nov 2026",
      paidByUid = "u_mary_jacob",
      paidByName = "Mary Jacob"
    )
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    current = familyRepo.getFamilyById("fam_vm_test")
    assertTrue(current!!.linkedMemberUids.contains("u_mary_jacob"))
    assertEquals(com.example.core.model.SubscriptionStatus.PAID, current.sharedSubscriptionStatus)
    assertEquals("Mary Jacob", current.lastPaidByName)

    viewModel.assignHouseholdHead(
      familyId = "fam_vm_test",
      headUid = "u_mary_jacob",
      headMemberId = "mem_mary",
      headName = "Mary Jacob",
      headPhone = "+919876500002"
    )
    viewModel.removeDependentMember(familyId = "fam_vm_test", dependentId = depId)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    current = familyRepo.getFamilyById("fam_vm_test")
    assertEquals("u_mary_jacob", current?.headUid)
    // Former head David Jacob is preserved in the household roster
    assertEquals(1, current?.dependentMembers?.size)
    assertEquals("u_jacob", current?.dependentMembers?.first()?.linkedUid)
  }

  @Test
  fun `profile photo helper compresses bitmap to valid data uri and decodes round trip`() {
    val bitmap = android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(android.graphics.Color.BLUE)

    val dataUri = com.example.core.util.ProfilePhotoHelper.encodeBitmapToCompressedDataUri(bitmap)
    assertTrue(dataUri.startsWith("data:image/jpeg;base64,"))
    assertTrue(dataUri.length <= com.example.core.util.ProfilePhotoHelper.MAX_DATA_URI_CHARS)

    val decoded = com.example.core.util.ProfilePhotoHelper.decodeDataUriBitmap(dataUri)
    assertNotNull(decoded)
    assertTrue((decoded?.width ?: 0) <= com.example.core.util.ProfilePhotoHelper.MAX_IMAGE_DIMENSION)
  }

  @Test
  fun `family repository connects registered accounts with household roles and counts accurately`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val familyRepo = com.example.data.family.FamilyRepository(context)

    val father = com.example.core.model.BelieverMember(
      memberId = "mem_father",
      uid = "u_father",
      membershipId = "GAG-2026-0101",
      fullName = "Joseph Peter",
      phoneNumber = "9876500101",
      familyId = "fam_joseph",
      familyName = "Joseph Household"
    )
    val mother = com.example.core.model.BelieverMember(
      memberId = "mem_mother",
      uid = "u_mother",
      membershipId = "GAG-2026-0102",
      fullName = "Martha Joseph",
      phoneNumber = "9876500102"
    )
    val son = com.example.core.model.BelieverMember(
      memberId = "mem_son",
      uid = "u_son",
      membershipId = "GAG-2026-0103",
      fullName = "Daniel Joseph",
      phoneNumber = "9876500103"
    )

    familyRepo.syncFromBelieverMember(father).getOrThrow()
    familyRepo.connectBelieverToFamily(
      familyId = "fam_joseph",
      targetMember = mother,
      relationshipRole = "Mother",
      defaultHeadMember = father
    ).getOrThrow()
    val withSon = familyRepo.connectBelieverToFamily(
      familyId = "fam_joseph",
      targetMember = son,
      relationshipRole = "Son",
      defaultHeadMember = father
    ).getOrThrow()

    assertEquals(3, withSon.totalHouseholdCount())
    assertTrue(withSon.linkedMemberUids.containsAll(listOf("u_father", "u_mother", "u_son")))
    assertEquals("Mother", withSon.dependentMembers.first { it.linkedUid == "u_mother" }.relationship)
    assertEquals("Son", withSon.dependentMembers.first { it.linkedUid == "u_son" }.relationship)

    val updatedRole = familyRepo.updateMemberRoleInFamily(
      familyId = "fam_joseph",
      entryIdOrUid = "u_mother",
      newRole = "Spouse"
    ).getOrThrow()
    assertEquals("Spouse", updatedRole.dependentMembers.first { it.linkedUid == "u_mother" }.relationship)

    val promotedMother = familyRepo.updateMemberRoleInFamily(
      familyId = "fam_joseph",
      entryIdOrUid = "u_mother",
      newRole = "Head",
      promoteToHead = true
    ).getOrThrow()
    assertEquals("u_mother", promotedMother.headUid)
    assertTrue(promotedMother.dependentMembers.any { it.linkedUid == "u_father" })
    assertEquals(3, promotedMother.totalHouseholdCount())
  }

  @Test
  fun `church settings drawer reflects updated church configuration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authRepo = com.example.data.auth.AuthRepository()
    val userRepo = com.example.data.user.UserRepository(context)
    val memberRepo = com.example.data.member.MemberRepository(context)
    val sessionManager = SessionManager(context)
    val authViewModel = com.example.presentation.auth.AuthViewModel(authRepo, userRepo, memberRepo, sessionManager)
    val languageManager = LanguageManager.getInstance(context)

    val customConfig = com.example.core.model.ChurchConfig(
      churchId = "church_main",
      name = "GRACE AG Tambaram",
      address = "88 GST Road, Tambaram, Chennai",
      phone = "+91 98765 01234",
      email = "tambaram@graceag.org",
      serviceTimings = "Sunday 07:30 AM & 09:30 AM",
      monthlyDuesAmount = 750.0
    )

    composeTestRule.setContent {
      com.example.ui.theme.ChurchConnectTheme {
        com.example.presentation.common.ChurchSettingsDrawerContent(
          userProfile = UserProfile(
            uid = "u_admin",
            displayName = "Admin Samuel",
            role = UserRole.SUPER_ADMIN.name,
            accountStatus = AccountStatus.APPROVED.name
          ),
          languageManager = languageManager,
          currentLanguage = LanguageManager.LANG_ENGLISH,
          viewModel = authViewModel,
          onCloseDrawer = {},
          churchConfig = customConfig
        )
      }
    }

    composeTestRule.onNodeWithTag("drawer_header_church_name")
      .assertTextContains("GRACE AG Tambaram")
    composeTestRule.onNodeWithTag("drawer_office_church_name")
      .assertTextContains("GRACE AG Tambaram")
    composeTestRule.onNodeWithTag("drawer_office_address")
      .assertTextContains("88 GST Road, Tambaram, Chennai")
    composeTestRule.onNodeWithTag("drawer_office_timings")
      .assertTextContains("Sunday 07:30 AM & 09:30 AM")
    composeTestRule.onNodeWithTag("drawer_office_phone")
      .assertTextContains("+91 98765 01234", substring = true)
    composeTestRule.onNodeWithTag("drawer_office_email")
      .assertTextContains("tambaram@graceag.org")
  }

  @Test
  fun `auto daily verse rotates across days and honors pastor pinned or manual mode`() {
    val customVerse = com.example.core.model.BibleVerse(
      verseId = "custom_1",
      bookReference = "John 3:16",
      textEnglish = "For God so loved the world...",
      textTamil = "தேவன் தம்முடைய ஒரேபேறான குமாரனைத் தந்தருளி...",
      pinDateKey = "2026-11-18",
      publishedByPastor = "Pastor Thomas"
    )
    val autoEnabledCfg = com.example.core.model.DailyVerseConfig(autoDailyEnabled = true)

    // Day 1 vs Day 2 rotate cleanly when nothing is pinned for that date
    val day1 = com.example.data.content.BibleContentRepository.resolveActiveDailyVerse(
      customVerses = emptyList(),
      autoConfig = autoEnabledCfg,
      dayOfYear = 1,
      todayKey = "2026-11-18"
    )
    val day2 = com.example.data.content.BibleContentRepository.resolveActiveDailyVerse(
      customVerses = emptyList(),
      autoConfig = autoEnabledCfg,
      dayOfYear = 2,
      todayKey = "2026-11-19"
    )
    assertEquals(com.example.data.content.DailyVerseSource.AUTO_DAILY, day1.source)
    assertTrue(day1.verse.bookReference != day2.verse.bookReference)

    // Pastor pinned verse overrides auto rotation on the pinned day
    val pinnedDay = com.example.data.content.BibleContentRepository.resolveActiveDailyVerse(
      customVerses = listOf(customVerse),
      autoConfig = autoEnabledCfg,
      dayOfYear = 1,
      todayKey = "2026-11-18"
    )
    assertEquals(com.example.data.content.DailyVerseSource.PASTOR_PINNED_TODAY, pinnedDay.source)
    assertEquals("John 3:16", pinnedDay.verse.bookReference)

    // When Auto Verse Daily is OFF, Pastor's latest custom verse stays active across days
    val manualCfg = com.example.core.model.DailyVerseConfig(autoDailyEnabled = false)
    val manualNextDay = com.example.data.content.BibleContentRepository.resolveActiveDailyVerse(
      customVerses = listOf(customVerse.copy(pinDateKey = "")),
      autoConfig = manualCfg,
      dayOfYear = 2,
      todayKey = "2026-11-19"
    )
    assertEquals(com.example.data.content.DailyVerseSource.PASTOR_CUSTOM, manualNextDay.source)
    assertEquals("John 3:16", manualNextDay.verse.bookReference)
  }

  @Test
  fun `church config update checker validates version code and download url`() {
    val upToDate = com.example.core.model.ChurchConfig(
      latestVersionCode = 1,
      latestVersionName = "1.0",
      updateDownloadUrl = "https://github.com/graceag/app/releases/download/v1.0/app.apk"
    )
    assertFalse(upToDate.hasUpdateAvailable(currentVersionCode = 1))

    val missingUrl = com.example.core.model.ChurchConfig(
      latestVersionCode = 2,
      latestVersionName = "1.1",
      updateDownloadUrl = ""
    )
    assertFalse(missingUrl.hasUpdateAvailable(currentVersionCode = 1))

    val available = com.example.core.model.ChurchConfig(
      latestVersionCode = 2,
      latestVersionName = "1.1",
      updateDownloadUrl = "https://github.com/graceag/app/releases/download/v1.1/app.apk"
    )
    assertTrue(available.hasUpdateAvailable(currentVersionCode = 1))
  }

  @Test
  fun `notification helper and scheduler cache verse and render update dialog`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.core.notifications.NotificationHelper.ensureChannels(context)
    com.example.core.notifications.NotificationHelper.cacheDailyVerseSnapshot(
      context = context,
      customVerses = listOf(
        com.example.core.model.BibleVerse(
          verseId = "c_cache",
          bookReference = "Psalm 100:5",
          textEnglish = "For the Lord is good; His mercy is everlasting.",
          textTamil = "கர்த்தர் நல்லவர்; அவருடைய கிருபை என்றென்றைக்கும் உள்ளது."
        )
      ),
      config = com.example.core.model.DailyVerseConfig(autoDailyEnabled = false)
    )
    val resolved = com.example.core.notifications.NotificationHelper.resolveVerseForReminder(context)
    assertEquals("Psalm 100:5", resolved.bookReference)

    val triggerMillis = com.example.core.notifications.DailyVerseAlarmScheduler.nextSixAmTriggerMillis()
    assertTrue(triggerMillis > System.currentTimeMillis())

    val updateCfg = com.example.core.model.ChurchConfig(
      name = "GRACE AG",
      latestVersionCode = 2,
      latestVersionName = "1.1",
      updateDownloadUrl = "https://github.com/graceag/app/releases/download/v1.1/app.apk",
      updateReleaseNotes = "Daily Auto Bible Verse and Notifications",
      forceUpdate = false
    )
    composeTestRule.setContent {
      com.example.ui.theme.ChurchConnectTheme {
        com.example.presentation.common.AppUpdateDialog(
          churchConfig = updateCfg,
          currentVersionName = "1.0",
          onDismiss = {}
        )
      }
    }
    composeTestRule.onNodeWithTag("app_update_download_button").assertExists()
    composeTestRule.onNodeWithTag("app_update_later_button").assertExists()
  }

  @Test
  fun `notification center dialog displays resolved daily verse and reminder controls`() {
    val sampleVerse = com.example.core.model.BibleVerse(
      verseId = "v_nc_1",
      bookReference = "Psalm 23:1",
      textEnglish = "The Lord is my shepherd; I shall not want.",
      textTamil = "கர்த்தர் என் மேய்ப்பராயிருக்கிறார்."
    )
    composeTestRule.setContent {
      com.example.ui.theme.ChurchConnectTheme {
        com.example.presentation.common.NotificationCenterDialog(
          churchId = "church_main",
          userRole = UserRole.BELIEVER,
          currentLanguage = LanguageManager.LANG_ENGLISH,
          todayVerse = sampleVerse,
          todayVerseSource = com.example.data.content.DailyVerseSource.AUTO_DAILY,
          announcements = emptyList(),
          onDismiss = {}
        )
      }
    }
    composeTestRule.onNodeWithTag("notif_center_daily_verse_card").assertExists()
    composeTestRule.onNodeWithTag("notif_center_daily_verse_switch").assertExists()
    composeTestRule.onNodeWithTag("notif_center_notices_switch").assertExists()
    composeTestRule.onNodeWithTag("notif_center_close_button").assertExists()
  }
}
