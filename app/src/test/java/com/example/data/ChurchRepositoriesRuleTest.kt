package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.core.model.AnnouncementCategory
import com.example.core.model.ChurchConfig
import com.example.core.model.ChurchEvent
import com.example.core.model.EventCategory
import com.example.core.model.FellowshipCategory
import com.example.core.model.FellowshipGroup
import com.example.core.model.MembershipApplication
import com.example.core.model.OfferingCategory
import com.example.core.model.PaymentMethod
import com.example.core.model.PastoralCareStatus
import com.example.core.model.PastoralCareType
import com.example.core.model.PrayerPrivacy
import com.example.data.admin.ChurchAdminRepository
import com.example.data.announcement.AnnouncementRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.finance.FinanceRepository
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChurchRepositoriesRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun submitPrayerRequest_authenticatedBeliever_writesAndQueriesOwnRequests() = runBlocking {
    val uid = signInTestUser("believer_prayer_${UUID.randomUUID().toString().take(6)}@test.com")
    val repo = PrayerRepository(firestore)

    val result = withTimeout(TIMEOUT_MS) {
      repo.submitPrayerRequest(
        uid = uid,
        name = "Samuel",
        title = "Thanksgiving Prayer",
        description = "Thanking God for guidance",
        privacy = PrayerPrivacy.PASTOR_ONLY,
        isAnonymous = false
      )
    }
    assertTrue(result.isSuccess)
    val req = result.getOrThrow()

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserPrayerRequests(uid).first { list ->
        list.any { it.requestId == req.requestId }
      }
    }
    assertTrue(observed.any { it.requestId == req.requestId })
  }

  @Test
  fun submitOffering_authenticatedBeliever_writesPending_andPastorVerifies() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_off_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Samuel",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }
    val adminUid = signInTestUser("admin_off_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf("role" to "PASTOR", "accountStatus" to "APPROVED")
      ).await()
    }

    val uid = signInTestUser("believer_offer_$suffix@test.com")
    val repo = FinanceRepository(firestore)

    val result = withTimeout(TIMEOUT_MS) {
      repo.submitOffering(
        uid = uid,
        donorName = "Grace",
        membershipId = "GAG-2026-0001",
        category = OfferingCategory.TITHE,
        amount = 1000.0,
        paymentMethod = PaymentMethod.UPI_ONLINE,
        note = "November Tithe",
        transactionReference = "UTR44556677"
      )
    }
    assertTrue(result.isSuccess)
    val tx = result.getOrThrow()

    val doc = withTimeout(TIMEOUT_MS) {
      firestore.collection("offerings").document(tx.transactionId).get().await()
    }
    assertTrue(doc.exists())
    assertEquals(1000.0, doc.getDouble("amount") ?: 0.0, 0.01)
    assertEquals("PENDING", doc.getString("status"))
    assertEquals("UTR44556677", doc.getString("transactionReference"))
    assertTrue((doc.getString("receiptNumber") ?: "").startsWith("REQ-TITH-"))

    // Pastor verifies and upgrades to SUCCESS with official sequential REC- receipt
    signInTestUser(pastorEmail)
    val verified = withTimeout(TIMEOUT_MS) {
      repo.verifyOfferingTransaction(tx.transactionId, approve = true)
    }
    assertTrue(verified.isSuccess)
    val verifiedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("offerings").document(tx.transactionId).get().await()
    }
    assertEquals("SUCCESS", verifiedDoc.getString("status"))
    assertTrue((verifiedDoc.getString("receiptNumber") ?: "").startsWith("REC-TITH-"))
  }

  @Test
  fun unauthenticatedCaller_cannotReadUserProfileDocument() = runBlocking {
    auth.signOut()
    var denied = false
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("users").document("some_uid").get().await()
      }
    } catch (e: FirebaseFirestoreException) {
      denied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    assertTrue("Expected PERMISSION_DENIED for unauthenticated read", denied)
  }

  @Test
  fun fellowshipGroups_pastorCanCreateUpdateManageMembersDelete_believerDeniedWrite() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_fg_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Grace",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }

    val adminUid = signInTestUser("admin_fg_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf(
          "role" to "PASTOR",
          "accountStatus" to "APPROVED"
        )
      ).await()
    }

    // Sign back in as the approved PASTOR
    signInTestUser(pastorEmail)
    val repo = FellowshipRepository(firestore)
    val created = withTimeout(TIMEOUT_MS) {
      repo.createGroup(
        FellowshipGroup(
          groupId = "fg_rule_$suffix",
          churchId = "church_main",
          nameEnglish = "Maranatha Care Cell",
          nameTamil = "மாரநாதா ஜெப ஐக்கியம்",
          areaZone = "West Zone",
          leaderName = "Bro. Samuel",
          leaderPhone = "9876543210",
          meetingSchedule = "Fridays 7 PM",
          meetingLocation = "T. Nagar",
          memberCount = 1,
          memberIds = listOf("mem_1"),
          category = FellowshipCategory.AREA_CARE_CELL
        )
      )
    }
    assertTrue(created.isSuccess)
    val group = created.getOrThrow()

    val createdDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("fellowshipGroups").document(group.groupId).get().await()
    }
    assertTrue(createdDoc.exists())
    assertEquals("Maranatha Care Cell", createdDoc.getString("nameEnglish"))

    // Pastor updates members list
    withTimeout(TIMEOUT_MS) {
      repo.updateGroupMembers(group.groupId, listOf("mem_1", "mem_2", "mem_3"))
    }
    val updatedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("fellowshipGroups").document(group.groupId).get().await()
    }
    assertEquals(listOf("mem_1", "mem_2", "mem_3"), updatedDoc.get("memberIds"))

    // Believer can read, but cannot create or delete
    val believerUid = signInTestUser("believer_fg_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(believerUid).set(
        mapOf(
          "uid" to believerUid,
          "displayName" to "Believer Ruth",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }
    val believerRead = withTimeout(TIMEOUT_MS) {
      firestore.collection("fellowshipGroups").document(group.groupId).get().await()
    }
    assertTrue(believerRead.exists())

    var believerDeleteDenied = false
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("fellowshipGroups").document(group.groupId).delete().await()
      }
    } catch (e: FirebaseFirestoreException) {
      believerDeleteDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    assertTrue("Expected PERMISSION_DENIED when believer tries to delete group", believerDeleteDenied)

    // Sign back in as approved PASTOR to delete group
    signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      repo.deleteGroup(group.groupId)
    }
    val deletedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("fellowshipGroups").document(group.groupId).get().await()
    }
    assertFalse(deletedDoc.exists())
  }

  @Test
  fun events_pastorCanCreateUpdateManageAttendeesDelete_believerDeniedWrite() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_ev_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Samuel",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }

    val adminUid = signInTestUser("admin_ev_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf(
          "role" to "PASTOR",
          "accountStatus" to "APPROVED"
        )
      ).await()
    }

    // Sign back in as approved PASTOR
    signInTestUser(pastorEmail)
    val repo = ChurchEventRepository(firestore)
    val created = withTimeout(TIMEOUT_MS) {
      repo.createEvent(
        ChurchEvent(
          eventId = "ev_rule_$suffix",
          churchId = "church_main",
          titleEnglish = "Youth Revival Night",
          titleTamil = "வாலிபர் எழுப்புதல் கூட்டம்",
          eventDate = "Saturday, 28 Nov 2026",
          startTime = "05:30 PM",
          endTime = "08:30 PM",
          venue = "Youth Hall",
          capacity = 200,
          registeredCount = 1,
          attendeeIds = listOf("mem_1"),
          category = EventCategory.YOUTH_EVENT
        )
      )
    }
    assertTrue(created.isSuccess)
    val ev = created.getOrThrow()

    val createdDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("events").document(ev.eventId).get().await()
    }
    assertTrue(createdDoc.exists())
    assertEquals("Youth Revival Night", createdDoc.getString("titleEnglish"))

    // Pastor updates attendees
    withTimeout(TIMEOUT_MS) {
      repo.updateEventAttendees(ev.eventId, listOf("mem_1", "mem_2"))
    }
    val updatedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("events").document(ev.eventId).get().await()
    }
    assertEquals(listOf("mem_1", "mem_2"), updatedDoc.get("attendeeIds"))

    // Believer can read, but cannot delete
    val believerUid = signInTestUser("believer_ev_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(believerUid).set(
        mapOf(
          "uid" to believerUid,
          "displayName" to "Believer Hannah",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }
    val believerRead = withTimeout(TIMEOUT_MS) {
      firestore.collection("events").document(ev.eventId).get().await()
    }
    assertTrue(believerRead.exists())

    var believerDeleteDenied = false
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("events").document(ev.eventId).delete().await()
      }
    } catch (e: FirebaseFirestoreException) {
      believerDeleteDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    assertTrue("Expected PERMISSION_DENIED when believer tries to delete event", believerDeleteDenied)

    // Pastor deletes event
    signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      repo.deleteEvent(ev.eventId)
    }
    val deletedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("events").document(ev.eventId).get().await()
    }
    assertFalse(deletedDoc.exists())
  }

  @Test
  fun adminConfigAndAuditLogs_andSermonAnnouncementDelete_workEndToEnd() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val adminEmail = "admin_cfg_$suffix@test.com"
    val adminUid = signInTestUser(adminEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Admin Esther",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
    }

    val adminRepo = ChurchAdminRepository(firestore)
    val saved = withTimeout(TIMEOUT_MS) {
      adminRepo.saveChurchConfig(
        ChurchConfig(
          churchId = "church_main",
          name = "Grace Baptist Church",
          address = "Chennai",
          phone = "+91 44 2456 7890",
          email = "office@gracechurch.org",
          defaultLanguage = "ta",
          serviceTimings = "Sunday 9 AM",
          monthlyDuesAmount = 750.0
        ),
        actorName = "Admin Esther"
      )
    }
    assertTrue(saved.isSuccess)
    val churchDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("churches").document("church_main").get().await()
    }
    assertEquals(750.0, churchDoc.getDouble("monthlyDuesAmount") ?: 0.0, 0.01)

    val auditLogs = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAuditLogs().first { list -> list.any { it.action.contains("750") } }
    }
    assertTrue(auditLogs.any { it.actorName == "Admin Esther" })

    val sermonRepo = SermonRepository(firestore)
    val sermon = withTimeout(TIMEOUT_MS) {
      sermonRepo.publishSermon(
        titleEn = "Hope in Christ",
        titleTa = "கிறிஸ்துவில் நம்பிக்கை",
        speaker = "Admin Esther",
        ref = "Romans 15:13",
        notes = "Abundant hope",
        videoUrl = ""
      )
    }.getOrThrow()
    assertTrue(withTimeout(TIMEOUT_MS) { firestore.collection("sermons").document(sermon.sermonId).get().await() }.exists())
    withTimeout(TIMEOUT_MS) { sermonRepo.deleteSermon(sermon.sermonId) }
    assertFalse(withTimeout(TIMEOUT_MS) { firestore.collection("sermons").document(sermon.sermonId).get().await() }.exists())

    val announcementRepo = AnnouncementRepository(firestore)
    val notice = withTimeout(TIMEOUT_MS) {
      announcementRepo.publishAnnouncement(
        titleEn = "Special Prayer",
        titleTa = "சிறப்பு ஜெபம்",
        bodyEn = "Join Friday evening",
        bodyTa = "வெள்ளி மாலை",
        category = AnnouncementCategory.FASTING_PRAYER,
        author = "Admin Esther"
      )
    }.getOrThrow()
    assertTrue(withTimeout(TIMEOUT_MS) { firestore.collection("announcements").document(notice.announcementId).get().await() }.exists())
    withTimeout(TIMEOUT_MS) { announcementRepo.deleteAnnouncement(notice.announcementId) }
    assertFalse(withTimeout(TIMEOUT_MS) { firestore.collection("announcements").document(notice.announcementId).get().await() }.exists())
  }

  @Test
  fun pastoralCareAlerts_believerCanRequestVisit_pastorCanListAndUpdateStatus() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_pc_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Paul",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }

    val adminUid = signInTestUser("admin_pc_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf(
          "role" to "PASTOR",
          "accountStatus" to "APPROVED"
        )
      ).await()
    }

    val believerUid = signInTestUser("believer_pc_$suffix@test.com")
    val repo = PastoralCareRepository(firestore)
    val created = withTimeout(TIMEOUT_MS) {
      repo.requestPastoralVisit(
        memberUid = believerUid,
        memberName = "Sister Martha",
        memberPhone = "+91 98400 12345",
        membershipId = "CC-2001",
        careType = PastoralCareType.HOSPITAL_VISIT,
        notes = "Pray before surgery",
        locationDetails = "Room 12, Apollo Hospital"
      )
    }
    assertTrue(created.isSuccess)
    val alert = created.getOrThrow()

    // Pastor can list church pastoral care alerts and update status
    signInTestUser(pastorEmail)
    val alerts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAlerts().first { list -> list.any { it.alertId == alert.alertId } }
    }
    assertTrue(alerts.any { it.alertId == alert.alertId })

    val updateResult = withTimeout(TIMEOUT_MS) {
      repo.updateAlertStatus(alert.alertId, PastoralCareStatus.VISITED)
    }
    assertTrue(updateResult.isSuccess)

    val updatedDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("pastoralCareAlerts").document(alert.alertId).get().await()
    }
    assertEquals("VISITED", updatedDoc.getString("status"))
  }

  @Test
  fun superAdminCanCreatePastorAccount_andBelieverRegistrationPreservesDetailsOnApproval() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val adminUid = signInTestUser("admin_create_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED",
          "profileCompleted" to true
        )
      ).await()
    }

    val appContext = ApplicationProvider.getApplicationContext<Context>()
    val adminRepo = ChurchAdminRepository(firestore)
    val pastorEmail = "pastor_created_$suffix@church.org"
    val pastorResult = withTimeout(TIMEOUT_MS) {
      adminRepo.createPastorAccount(
        context = appContext,
        fullName = "Pastor Caleb",
        email = pastorEmail,
        phoneNumber = "9876543219",
        password = "PastorPass123",
        actorName = "Super Admin"
      )
    }
    assertTrue(pastorResult.isSuccess)
    val pastorProfile = pastorResult.getOrThrow()
    assertEquals("PASTOR", pastorProfile.role)
    assertEquals("APPROVED", pastorProfile.accountStatus)
    assertTrue(pastorProfile.profileCompleted)
    assertTrue(
      "Expected sequential Pastor ID like GAG-P-2026-001, got ${pastorProfile.membershipId}",
      pastorProfile.membershipId.matches(Regex("GAG-P-\\d{4}-\\d{3}"))
    )

    // Verify Super Admin stayed signed in cleanly while secondary app created pastor auth
    assertEquals(adminUid, auth.currentUser?.uid)

    val inviteDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("pastorInvites").document("email_${pastorEmail.lowercase()}").get().await()
    }
    assertTrue(inviteDoc.exists())
    assertEquals("PASTOR", inviteDoc.getString("role"))

    // Believer signs up, submits full registration details, and gets approved without losing details
    val believerUid = signInTestUser("believer_reg_$suffix@test.com")
    val userRepo = com.example.data.user.UserRepository(firestore)
    val memberRepo = com.example.data.member.MemberRepository(firestore)

    withTimeout(TIMEOUT_MS) {
      userRepo.createUserProfile(
        com.example.core.model.UserProfile(
          uid = believerUid,
          email = "believer_reg_$suffix@test.com",
          role = "BELIEVER",
          churchId = "church_main",
          accountStatus = "PENDING",
          membershipId = "CC-5555",
          profileCompleted = false
        )
      ).getOrThrow()

      userRepo.submitMembershipApplication(
        fullName = "Hannah Grace",
        phoneNumber = "+919876500001",
        email = "believer_reg_$suffix@test.com",
        address = "22 Church Road, Madurai",
        dateOfBirth = "14/05/1998",
        gender = "Female",
        baptismStatus = "BAPTIZED",
        churchId = "church_main"
      ).getOrThrow()

      memberRepo.saveMemberProfile(
        com.example.core.model.BelieverMember(
          memberId = "mem_$believerUid",
          uid = believerUid,
          churchId = "church_main",
          membershipId = "CC-5555",
          fullName = "Hannah Grace",
          phoneNumber = "+919876500001",
          email = "believer_reg_$suffix@test.com",
          dateOfBirth = "14/05/1998",
          gender = "Female",
          address = "22 Church Road, Madurai",
          fatherName = "Joseph",
          baptismStatus = "BAPTIZED",
          membershipStatus = "PENDING"
        )
      ).getOrThrow()
    }

    // Re-authenticate as Pastor Caleb using the password Admin assigned
    auth.signInWithEmailAndPassword(pastorEmail, "PastorPass123").await()
    val pastorAuthUid = auth.currentUser!!.uid
    val loggedInPastor = userRepo.getUserProfile(pastorAuthUid)
    assertEquals("PASTOR", loggedInPastor?.role)
    assertEquals("APPROVED", loggedInPastor?.accountStatus)

    withTimeout(TIMEOUT_MS) {
      memberRepo.approveApplication(
        application = MembershipApplication(
          applicationId = believerUid,
          uid = believerUid,
          churchId = "church_main",
          fullName = "Hannah Grace",
          phoneNumber = "+919876500001",
          email = "believer_reg_$suffix@test.com",
          dateOfBirth = "14/05/1998",
          gender = "Female",
          address = "22 Church Road, Madurai",
          baptismStatus = "BAPTIZED",
          status = "PENDING"
        ),
        reviewerUid = pastorAuthUid
      ).getOrThrow()
    }

    val mergedMemberDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("believers").document("mem_$believerUid").get().await()
    }
    assertEquals("ACTIVE", mergedMemberDoc.getString("membershipStatus"))
    assertEquals("14/05/1998", mergedMemberDoc.getString("dateOfBirth"))
    assertEquals("Joseph", mergedMemberDoc.getString("fatherName"))
    assertEquals("BAPTIZED", mergedMemberDoc.getString("baptismStatus"))
  }

  @Test
  fun familyLinkingAndMonthlySubscription_createsOneBillPerFamily_andSharedPaymentSyncs() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_fam_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor John",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }

    val adminUid = signInTestUser("admin_fam_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf("role" to "PASTOR", "accountStatus" to "APPROVED")
      ).await()
    }

    val memberRepo = com.example.data.member.MemberRepository(firestore)
    val userRepo = com.example.data.user.UserRepository(firestore)

    // 1. Head of family signs up (PENDING) and saves profile with family members
    val headUid = signInTestUser("head_fam_$suffix@test.com")
    val sharedFamilyId = "fam_$headUid"
    val headMember = com.example.core.model.BelieverMember(
      memberId = "mem_$headUid",
      uid = headUid,
      churchId = "church_main",
      membershipId = "CC-9001",
      fullName = "David Jacob",
      phoneNumber = "+919876543001",
      familyId = sharedFamilyId,
      familyName = "Jacob Household",
      familyRole = "FAMILY_HEAD",
      familyMembers = listOf(
        com.example.core.model.FamilyMemberEntry(id = "fm_1", name = "Mary Jacob", relationship = "Spouse"),
        com.example.core.model.FamilyMemberEntry(id = "fm_2", name = "Luke Jacob", relationship = "Son")
      ),
      membershipStatus = "PENDING"
    )
    withTimeout(TIMEOUT_MS) {
      userRepo.createUserProfile(
        com.example.core.model.UserProfile(
          uid = headUid,
          displayName = "David Jacob",
          role = "BELIEVER",
          churchId = "church_main",
          accountStatus = "PENDING",
          familyId = sharedFamilyId
        )
      ).getOrThrow()
      memberRepo.saveMemberProfile(headMember).getOrThrow()
    }

    // 2. Spouse also registers separately (PENDING)
    val spouseUid = signInTestUser("spouse_fam_$suffix@test.com")
    val spouseMember = com.example.core.model.BelieverMember(
      memberId = "mem_$spouseUid",
      uid = spouseUid,
      churchId = "church_main",
      membershipId = "CC-9002",
      fullName = "Mary Jacob",
      phoneNumber = "+919876543002",
      membershipStatus = "PENDING"
    )
    withTimeout(TIMEOUT_MS) {
      userRepo.createUserProfile(
        com.example.core.model.UserProfile(
          uid = spouseUid,
          displayName = "Mary Jacob",
          role = "BELIEVER",
          churchId = "church_main",
          accountStatus = "PENDING",
          familyId = "fam_$spouseUid"
        )
      ).getOrThrow()
      memberRepo.saveMemberProfile(spouseMember).getOrThrow()
    }

    // Pastor approves both members, links spouse to head's familyId, and generates 1 monthly family subscription
    signInTestUser(pastorEmail)
    firestore.collection("users").document(headUid).update("accountStatus", "APPROVED").await()
    firestore.collection("users").document(spouseUid).update("accountStatus", "APPROVED").await()
    memberRepo.saveMemberProfile(headMember.copy(membershipStatus = "ACTIVE")).getOrThrow()
    val financeRepo = FinanceRepository(firestore)
    val linkedSpouse = withTimeout(TIMEOUT_MS) {
      memberRepo.updateMemberFamilyAssignment(
        member = spouseMember.copy(membershipStatus = "ACTIVE"),
        newFamilyId = sharedFamilyId,
        newFamilyName = "Jacob Household",
        familyRole = "FAMILY_MEMBER"
      ).getOrThrow()
    }
    assertEquals(sharedFamilyId, linkedSpouse.familyId)

    val monthKey = "2026_11"
    val monthYear = "November 2026"
    val generatedCount = withTimeout(TIMEOUT_MS) {
      financeRepo.generateMonthlyFamilySubscriptions(
        members = listOf(headMember, linkedSpouse),
        amountDue = 500.0,
        churchId = "church_main",
        monthKey = monthKey,
        monthYear = monthYear
      ).getOrThrow()
    }
    assertEquals(1, generatedCount)

    // 3. Linked spouse submits payment proof (PENDING_VERIFICATION); self-marking PAID is blocked
    signInTestUser("spouse_fam_$suffix@test.com")
    val expectedSubId = financeRepo.subscriptionDocId(sharedFamilyId, monthKey)
    val selfMarkPaidResult = withTimeout(TIMEOUT_MS) {
      financeRepo.paySubscription(
        subscriptionId = expectedSubId,
        paidByUid = spouseUid,
        paidByName = "Mary Jacob",
        paymentMethod = PaymentMethod.UPI_ONLINE.name
      )
    }
    assertTrue("Believer should not be allowed to mark subscription PAID directly", selfMarkPaidResult.isFailure)

    val submittedResult = withTimeout(TIMEOUT_MS) {
      financeRepo.submitFamilySubscriptionPayment(
        existingSubscriptionId = expectedSubId,
        familyId = sharedFamilyId,
        familyName = "Jacob Household",
        familyMemberCount = 3,
        payerUid = spouseUid,
        payerName = "Mary Jacob",
        membershipId = "CC-9002",
        amountDue = 500.0,
        paymentMethod = PaymentMethod.UPI_ONLINE,
        transactionReference = "UTR99887766"
      )
    }
    assertTrue(submittedResult.isSuccess)
    val submittedSub = submittedResult.getOrThrow()
    assertEquals(com.example.core.model.SubscriptionStatus.PENDING_VERIFICATION, submittedSub.status)
    assertEquals(0.0, submittedSub.amountPaid, 0.01)
    assertTrue(submittedSub.receiptNumber.startsWith("REQ-SUB-"))

    // Pastor verifies payment proof and officially marks the family bill PAID
    signInTestUser(pastorEmail)
    val verifiedResult = withTimeout(TIMEOUT_MS) {
      financeRepo.paySubscription(
        subscriptionId = expectedSubId,
        paidByUid = spouseUid,
        paidByName = "Mary Jacob",
        paymentMethod = PaymentMethod.UPI_ONLINE.name
      )
    }
    assertTrue(verifiedResult.isSuccess)
    val verifiedSub = verifiedResult.getOrThrow()
    assertEquals(com.example.core.model.SubscriptionStatus.PAID, verifiedSub.status)
    assertEquals(500.0, verifiedSub.amountPaid, 0.01)
    assertTrue(verifiedSub.receiptNumber.startsWith("REC-SUB-"))

    // 4. Head of family observes the PAID status on the single shared household subscription
    signInTestUser("head_fam_$suffix@test.com")
    val headSubs = withTimeout(FLOW_TIMEOUT_MS) {
      financeRepo.observeUserSubscriptions(headUid, sharedFamilyId).first { list ->
        list.any { it.subscriptionId == expectedSubId && it.status == com.example.core.model.SubscriptionStatus.PAID }
      }
    }
    val famSubs = headSubs.filter { it.familyId == sharedFamilyId && it.monthYear == monthYear }
    assertEquals(1, famSubs.size)
    assertEquals("Mary Jacob", famSubs.first().paidByName)
    assertEquals(3, famSubs.first().familyMemberCount)

    // 5. Verify dedicated /families/{familyId} Firestore entity reflects head, dependents, linked member, and PAID status
    val familyRepo = com.example.data.family.FamilyRepository(firestore)
    val familyEntity = withTimeout(TIMEOUT_MS) {
      familyRepo.getFamilyById(sharedFamilyId)
    }
    assertEquals(headUid, familyEntity?.headUid)
    assertEquals("David Jacob", familyEntity?.headName)
    assertEquals(2, familyEntity?.dependentMembers?.size)
    assertTrue(familyEntity?.linkedMemberUids?.containsAll(listOf(headUid, spouseUid)) == true)
    assertEquals(com.example.core.model.SubscriptionStatus.PAID, familyEntity?.sharedSubscriptionStatus)
    assertEquals("Mary Jacob", familyEntity?.lastPaidByName)
  }

  @Test
  fun familyConnectionAndProfilePhotoSync_worksEndToEndBetweenHeadRelativeAndPastor() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_con_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Thomas",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }

    val adminUid = signInTestUser("admin_con_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf("role" to "PASTOR", "accountStatus" to "APPROVED")
      ).await()
    }

    val userRepo = com.example.data.user.UserRepository(firestore)
    val memberRepo = com.example.data.member.MemberRepository(firestore)
    val samplePhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/"

    // 1. Believer B (Mother) saves profile with compressed photo Data URI -> syncs to /users, /believers, and /memberLookup
    val motherEmail = "mother_con_$suffix@test.com"
    val motherUid = signInTestUser(motherEmail)
    val motherMemberIdStr = "GAG-2026-8102"
    val motherMember = com.example.core.model.BelieverMember(
      memberId = "mem_$motherUid",
      uid = motherUid,
      churchId = "church_main",
      membershipId = motherMemberIdStr,
      fullName = "Ruth Isaac",
      profilePhotoUrl = samplePhotoUri,
      phoneNumber = "+919840098402",
      baptismStatus = "BAPTIZED",
      membershipStatus = "ACTIVE"
    )
    withTimeout(TIMEOUT_MS) {
      userRepo.createUserProfile(
        com.example.core.model.UserProfile(
          uid = motherUid,
          displayName = "Ruth Isaac",
          role = "BELIEVER",
          churchId = "church_main",
          accountStatus = "PENDING",
          membershipId = motherMemberIdStr,
          familyId = "fam_$motherUid"
        )
      ).getOrThrow()
      memberRepo.saveMemberProfile(motherMember).getOrThrow()
    }

    val motherUserDoc = withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(motherUid).get().await()
    }
    assertEquals(samplePhotoUri, motherUserDoc.getString("photoUrl"))
    val lookupById = withTimeout(TIMEOUT_MS) {
      firestore.collection("memberLookup").document("id_${motherMemberIdStr.uppercase()}").get().await()
    }
    assertTrue(lookupById.exists())
    assertEquals(samplePhotoUri, lookupById.getString("profilePhotoUrl"))

    // 2. Believer Head signs in, looks up Mother by Membership ID, connects her as "Mother", then updates role to "Spouse"
    val headEmail = "head_con_$suffix@test.com"
    val headUid = signInTestUser(headEmail)
    val headMember = com.example.core.model.BelieverMember(
      memberId = "mem_$headUid",
      uid = headUid,
      churchId = "church_main",
      membershipId = "GAG-2026-8101",
      fullName = "Isaac Daniel",
      phoneNumber = "+919840098401",
      familyId = "fam_$headUid",
      familyName = "Isaac Household",
      familyRole = "FAMILY_HEAD",
      membershipStatus = "ACTIVE"
    )
    withTimeout(TIMEOUT_MS) {
      userRepo.createUserProfile(
        com.example.core.model.UserProfile(
          uid = headUid,
          displayName = "Isaac Daniel",
          role = "BELIEVER",
          churchId = "church_main",
          accountStatus = "PENDING",
          membershipId = "GAG-2026-8101",
          familyId = "fam_$headUid"
        )
      ).getOrThrow()
      memberRepo.saveMemberProfile(headMember).getOrThrow()
    }

    val foundMother = withTimeout(TIMEOUT_MS) {
      memberRepo.lookupBelieverByIdOrPhone(motherMemberIdStr, "church_main").getOrThrow()
    }
    assertEquals(motherUid, foundMother?.uid)
    assertEquals(samplePhotoUri, foundMother?.profilePhotoUrl)

    val connectedFamily = withTimeout(TIMEOUT_MS) {
      memberRepo.familyRepository.connectBelieverToFamily(
        familyId = "fam_$headUid",
        targetMember = foundMother!!,
        relationshipRole = "Mother",
        makeHead = false,
        defaultHeadMember = headMember
      ).getOrThrow()
    }
    assertTrue(connectedFamily.linkedMemberUids.containsAll(listOf(headUid, motherUid)))
    assertEquals("Mother", connectedFamily.dependentMembers.first { it.linkedUid == motherUid }.relationship)

    val roleUpdatedFamily = withTimeout(TIMEOUT_MS) {
      memberRepo.familyRepository.updateMemberRoleInFamily(
        familyId = "fam_$headUid",
        entryIdOrUid = motherUid,
        newRole = "Spouse",
        promoteToHead = false
      ).getOrThrow()
    }
    assertEquals("Spouse", roleUpdatedFamily.dependentMembers.first { it.linkedUid == motherUid }.relationship)

    // 3. Mother signs back in, observes the connected household, and adopts shared familyId
    signInTestUser(motherEmail)
    val observedForMother = withTimeout(FLOW_TIMEOUT_MS) {
      memberRepo.familyRepository.observeFamilyForMember(motherUid, "fam_$motherUid").first { fam ->
        fam?.familyId == "fam_$headUid"
      }
    }
    assertEquals("fam_$headUid", observedForMother?.familyId)
    assertEquals("Isaac Daniel", observedForMother?.headName)

    withTimeout(TIMEOUT_MS) {
      memberRepo.adoptLinkedFamilyForMember(
        member = motherMember,
        targetFamilyId = observedForMother!!.familyId,
        targetFamilyName = observedForMother.familyName,
        isHead = false
      ).getOrThrow()
    }
    val updatedMotherUser = withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(motherUid).get().await()
    }
    assertEquals("fam_$headUid", updatedMotherUser.getString("familyId"))

    // 4. Pastor can promote Mother to Head or unlink her cleanly
    signInTestUser(pastorEmail)
    val promotedFamily = withTimeout(TIMEOUT_MS) {
      memberRepo.familyRepository.updateMemberRoleInFamily(
        familyId = "fam_$headUid",
        entryIdOrUid = motherUid,
        newRole = "Head",
        promoteToHead = true
      ).getOrThrow()
    }
    assertEquals(motherUid, promotedFamily.headUid)
    assertTrue(promotedFamily.dependentMembers.any { it.linkedUid == headUid })
  }

  @Test
  fun bibleContentAndUpdater_pastorManagesDailyVerseAndAutoRotation_adminSavesAppRelease() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val pastorEmail = "pastor_dv_$suffix@test.com"
    val pastorUid = signInTestUser(pastorEmail)
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(pastorUid).set(
        mapOf(
          "uid" to pastorUid,
          "displayName" to "Pastor Thomas",
          "role" to "BELIEVER",
          "churchId" to "church_main",
          "accountStatus" to "PENDING"
        )
      ).await()
    }
    val adminUid = signInTestUser("admin_dv_$suffix@test.com")
    withTimeout(TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).set(
        mapOf(
          "uid" to adminUid,
          "displayName" to "Super Admin",
          "role" to "SUPER_ADMIN",
          "churchId" to "church_main",
          "accountStatus" to "APPROVED"
        )
      ).await()
      firestore.collection("users").document(pastorUid).update(
        mapOf("role" to "PASTOR", "accountStatus" to "APPROVED")
      ).await()
    }

    // Super Admin saves APK update metadata in ChurchConfig
    val adminRepo = ChurchAdminRepository(firestore)
    val savedCfg = withTimeout(TIMEOUT_MS) {
      adminRepo.saveChurchConfig(
        ChurchConfig(
          churchId = "church_main",
          name = "GRACE AG",
          latestVersionCode = 5,
          latestVersionName = "1.5",
          updateDownloadUrl = "https://github.com/graceag/app/releases/download/v1.5/grace-ag.apk",
          updateReleaseNotes = "Added Auto Daily Verse & Notifications",
          forceUpdate = true
        ),
        actorName = "Super Admin"
      ).getOrThrow()
    }
    assertEquals(5, savedCfg.latestVersionCode)
    assertTrue(savedCfg.hasUpdateAvailable(currentVersionCode = 1))

    // Pastor enables/disables Auto Verse Daily and publishes/deletes a custom verse
    signInTestUser(pastorEmail)
    val contentRepo = com.example.data.content.BibleContentRepository(firestore)
    val autoDisabled = withTimeout(TIMEOUT_MS) {
      contentRepo.setAutoDailyVerseEnabled(
        enabled = false,
        pastorName = "Pastor Thomas",
        churchId = "church_main"
      ).getOrThrow()
    }
    assertFalse(autoDisabled.autoDailyEnabled)

    val published = withTimeout(TIMEOUT_MS) {
      contentRepo.publishVerse(
        reference = "Isaiah 40:31",
        textEn = "Those who wait on the Lord shall renew their strength.",
        textTa = "கர்த்தருக்குக் காத்திருக்கிறவர்களோ புதுப்பெலன் அடைவார்கள்.",
        devEn = "Wait on God in prayer today.",
        devTa = "இன்று ஜெபத்தில் கர்த்தருக்குக் காத்திருங்கள்.",
        pastorName = "Pastor Thomas",
        pinForToday = true,
        churchId = "church_main"
      ).getOrThrow()
    }
    assertEquals("Isaiah 40:31", published.bookReference)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      contentRepo.observeDailyVerses("church_main").first { list ->
        list.any { it.verseId == published.verseId }
      }
    }
    assertTrue(observed.any { it.verseId == published.verseId })

    withTimeout(TIMEOUT_MS) {
      contentRepo.deleteVerse(published.verseId).getOrThrow()
    }
    val verseSnap = withTimeout(TIMEOUT_MS) {
      firestore.collection("bibleVerses").document(published.verseId).get().await()
    }
    assertFalse(verseSnap.exists())
  }

  private companion object {
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
