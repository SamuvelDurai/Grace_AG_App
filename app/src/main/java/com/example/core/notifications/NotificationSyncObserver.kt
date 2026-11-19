package com.example.core.notifications

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.core.localization.LanguageManager
import com.example.core.model.CelebrationType
import com.example.core.model.DailyVerseConfig
import com.example.core.model.PaymentTransactionStatus
import com.example.core.model.SubscriptionStatus
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.example.data.announcement.AnnouncementRepository
import com.example.data.content.BibleContentRepository
import com.example.data.finance.FinanceRepository
import com.example.data.pastoralcare.CelebrationRepository

@Composable
fun NotificationSyncObserver(
  userProfile: UserProfile,
  currentLanguage: String,
  announcementRepository: AnnouncementRepository,
  financeRepository: FinanceRepository,
  bibleContentRepository: BibleContentRepository,
  celebrationRepository: CelebrationRepository? = null
) {
  val context = LocalContext.current
  val churchId = userProfile.churchId.ifBlank { "church_main" }
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      DailyVerseAlarmScheduler.syncSchedule(context)
      NotificationHelper.syncFcmSubscriptions(
        context = context,
        churchId = churchId,
        role = userProfile.userRole
      )
    }
  }

  LaunchedEffect(userProfile.uid, churchId, userProfile.role) {
    NotificationHelper.ensureChannels(context)
    DailyVerseAlarmScheduler.syncSchedule(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      !NotificationHelper.hasNotificationPermission(context)
    ) {
      val prefs = context.applicationContext.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
      if (!prefs.getBoolean(NotificationHelper.PREF_NOTIF_PROMPTED, false)) {
        prefs.edit().putBoolean(NotificationHelper.PREF_NOTIF_PROMPTED, true).apply()
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      NotificationHelper.syncFcmSubscriptions(
        context = context,
        churchId = churchId,
        role = userProfile.userRole
      )
    }
  }

  // Keep daily verse offline cache fresh for the 6:00 AM AlarmReceiver
  val verses by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val dailyConfig by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  LaunchedEffect(verses, dailyConfig) {
    NotificationHelper.cacheDailyVerseSnapshot(context, verses, dailyConfig)
  }

  // Church Family Birthdays (ALL members get today's birthday alerts; Pastors & Super Admins also get 7-day weekly reminders)
  if (celebrationRepository != null) {
    val celebrations by celebrationRepository.observeChurchCelebrations(churchId).collectAsState(initial = emptyList())
    LaunchedEffect(celebrations, userProfile.role, isTamil) {
      if (!NotificationHelper.isNoticesEnabled(context) || celebrations.isEmpty()) return@LaunchedEffect
      val todayKey = BibleContentRepository.todayDateKey()

      val todayCelebrations = celebrations.filter { it.isToday() }
      todayCelebrations.take(4).forEach { cel ->
        val isAnniv = cel.celebrationType == CelebrationType.WEDDING_ANNIVERSARY
        val title = if (isAnniv) {
          if (isTamil) "💍 திருமண நாள் வாழ்த்துக்கள் ${cel.memberName}!" else "💍 Happy Anniversary ${cel.memberName}!"
        } else {
          if (isTamil) "🎂 பிறந்தநாள் வாழ்த்துக்கள் ${cel.memberName}!" else "🎂 Happy Birthday ${cel.memberName}!"
        }
        val body = if (isAnniv) {
          if (isTamil) {
            "GRACE AG சபைக் குடும்பத்தின் திருமண நாள் வாழ்த்துக்கள் — ${cel.memberName}! வாழ்த்து அனுப்ப தட்டவும்."
          } else {
            "Anniversary wishes from GRACE AG Church Family to ${cel.memberName}! Tap to send your blessing."
          }
        } else {
          if (isTamil) {
            "GRACE AG சபைக் குடும்பத்தின் பிறந்தநாள் வாழ்த்துக்கள் — ${cel.memberName}! வாழ்த்து அனுப்ப தட்டவும்."
          } else {
            "Birthday wishes from GRACE AG Church Family to ${cel.memberName}! Tap to send your blessing."
          }
        }
        NotificationHelper.notifyIfNew(
          context = context,
          dedupeKey = "bday_${todayKey}_${cel.celebrationId}",
          channelId = NotificationHelper.CHANNEL_BIRTHDAYS,
          title = title,
          body = body
        )
      }

      if (userProfile.userRole == UserRole.PASTOR || userProfile.userRole == UserRole.SUPER_ADMIN) {
        val upcomingWeek = celebrations.filter { it.isWithinNextWeek() }
        if (upcomingWeek.isNotEmpty()) {
          val summaryList = upcomingWeek.take(5).joinToString(" • ") { item ->
            val whenText = when (item.daysUntil()) {
              0 -> if (isTamil) "இன்று" else "Today"
              1 -> if (isTamil) "நாளை" else "Tomorrow"
              else -> item.displayDate
            }
            "${item.memberName} ($whenText)"
          }
          NotificationHelper.notifyIfNew(
            context = context,
            dedupeKey = "weekly_bday_${todayKey}_${upcomingWeek.size}",
            channelId = NotificationHelper.CHANNEL_BIRTHDAYS,
            title = if (isTamil) {
              "🗓️ இந்த வார சபை பிறந்தநாள்கள் (${upcomingWeek.size})"
            } else {
              "🗓️ This Week's Church Birthdays (${upcomingWeek.size})"
            },
            body = summaryList
          )
        }
      }
    }
  }

  // Notify on newly added Announcements (suppressing startup historical announcements)
  val announcements by announcementRepository.observeAnnouncements(churchId).collectAsState(initial = emptyList())
  var seededAnn by remember(userProfile.uid) { mutableStateOf(false) }
  var knownAnnIds by remember(userProfile.uid) { mutableStateOf<Set<String>>(emptySet()) }

  LaunchedEffect(announcements) {
    val currentIds = announcements.map { it.announcementId }.filter { it.isNotBlank() }.toSet()
    if (!seededAnn) {
      knownAnnIds = currentIds
      seededAnn = true
    } else {
      val newItems = announcements.filter { it.announcementId.isNotBlank() && !knownAnnIds.contains(it.announcementId) }
      knownAnnIds = knownAnnIds + currentIds
      if (NotificationHelper.isNoticesEnabled(context)) {
        newItems.take(2).forEach { ann ->
          val title = if (isTamil) ann.titleTamil.ifBlank { ann.titleEnglish } else ann.titleEnglish
          val body = if (isTamil) ann.bodyTamil.ifBlank { ann.bodyEnglish } else ann.bodyEnglish
          NotificationHelper.notifyIfNew(
            context = context,
            dedupeKey = "ann_${ann.announcementId}",
            channelId = NotificationHelper.CHANNEL_NOTICES,
            title = if (isTamil) "புதிய சபை அறிவிப்பு: $title" else "Church Notice: $title",
            body = body.ifBlank { title }
          )
        }
      }
    }
  }

  // Notify Believers when their offering or monthly subscription receipt is verified
  if (userProfile.userRole == UserRole.BELIEVER) {
    val offerings by financeRepository.observeUserOfferings(userProfile.uid).collectAsState(initial = emptyList())
    var seededOfferings by remember(userProfile.uid) { mutableStateOf(false) }
    var knownSuccessOfferingIds by remember(userProfile.uid) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(offerings) {
      val successOfferings = offerings.filter { it.status == PaymentTransactionStatus.SUCCESS }
      val currentIds = successOfferings.map { it.transactionId }.toSet()
      if (!seededOfferings) {
        knownSuccessOfferingIds = currentIds
        seededOfferings = true
      } else {
        val newlyVerified = successOfferings.filter { !knownSuccessOfferingIds.contains(it.transactionId) }
        knownSuccessOfferingIds = knownSuccessOfferingIds + currentIds
        if (NotificationHelper.isNoticesEnabled(context)) {
          newlyVerified.forEach { tx ->
            NotificationHelper.notifyIfNew(
              context = context,
              dedupeKey = "off_${tx.transactionId}_${tx.receiptNumber}",
              channelId = NotificationHelper.CHANNEL_ACCOUNT,
              title = if (isTamil) "காணிக்கை ரசீது உறுதிசெய்யப்பட்டது" else "Offering Receipt Verified",
              body = "₹${tx.amount.toInt()} • ${tx.receiptNumber}"
            )
          }
        }
      }
    }

    val subs by financeRepository.observeUserSubscriptions(userProfile.uid, userProfile.effectiveFamilyId())
      .collectAsState(initial = emptyList())
    var seededSubs by remember(userProfile.uid) { mutableStateOf(false) }
    var knownPaidSubIds by remember(userProfile.uid) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(subs) {
      val paidSubs = subs.filter { it.status == SubscriptionStatus.PAID }
      val currentIds = paidSubs.map { it.subscriptionId }.toSet()
      if (!seededSubs) {
        knownPaidSubIds = currentIds
        seededSubs = true
      } else {
        val newlyPaid = paidSubs.filter { !knownPaidSubIds.contains(it.subscriptionId) }
        knownPaidSubIds = knownPaidSubIds + currentIds
        if (NotificationHelper.isNoticesEnabled(context)) {
          newlyPaid.forEach { sub ->
            NotificationHelper.notifyIfNew(
              context = context,
              dedupeKey = "sub_${sub.subscriptionId}_${sub.receiptNumber}",
              channelId = NotificationHelper.CHANNEL_ACCOUNT,
              title = if (isTamil) "மாதாந்திர சந்தா ரசீது" else "Monthly Subscription Verified",
              body = "${sub.monthYear} • ${sub.receiptNumber}"
            )
          }
        }
      }
    }
  }
}
