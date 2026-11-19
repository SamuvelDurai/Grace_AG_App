package com.example.presentation.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.DailyVerseConfig
import com.example.core.model.PastoralCareType
import com.example.core.model.UserProfile
import com.example.core.model.ChurchConfig
import com.example.core.model.UserRole
import com.example.data.admin.ChurchAdminRepository
import com.example.data.announcement.AnnouncementRepository
import com.example.data.content.BibleContentRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.finance.FinanceRepository
import com.example.data.member.MemberRepository
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.example.presentation.announcement.PastorAnnouncementsScreen
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.common.ChurchSettingsDrawerContent
import com.example.presentation.common.NotificationCenterDialog
import com.example.presentation.common.WeeklyBirthdayReminderCard
import com.example.presentation.finance.PastorFinanceScreen
import com.example.presentation.member.PastorMembersScreen
import com.example.presentation.ministry.PastorMinistryScreen
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastorDashboardScreen(
  userProfile: UserProfile,
  viewModel: AuthViewModel,
  memberRepository: MemberRepository,
  bibleContentRepository: BibleContentRepository,
  announcementRepository: AnnouncementRepository,
  financeRepository: FinanceRepository,
  prayerRepository: PrayerRepository,
  churchEventRepository: ChurchEventRepository,
  sermonRepository: SermonRepository,
  fellowshipRepository: FellowshipRepository,
  pastoralCareRepository: PastoralCareRepository,
  churchAdminRepository: ChurchAdminRepository,
  languageManager: LanguageManager,
  currentLanguage: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  var selectedTab by remember { mutableIntStateOf(0) }
  var initialMinistryTab by remember { mutableIntStateOf(0) }
  var showNotificationCenter by remember { mutableStateOf(false) }
  val churchId = userProfile.churchId.ifBlank { "church_main" }
  val churchConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))
  val announcementsForNotif by announcementRepository.observeAnnouncements(churchId).collectAsState(initial = emptyList())
  val versesForNotif by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val verseConfigForNotif by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val todayAlertsForNotif by pastoralCareRepository.observeTodayAlerts().collectAsState(initial = emptyList())
  val churchCelebrations by memberRepository.celebrationRepository
    .observeChurchCelebrations(churchId)
    .collectAsState(initial = emptyList())
  val weeklyBirthdays = remember(churchCelebrations) {
    churchCelebrations.filter { it.isWithinNextWeek() }
  }
  val todayVerseResolutionForNotif = remember(versesForNotif, verseConfigForNotif) {
    BibleContentRepository.resolveActiveDailyVerse(versesForNotif, verseConfigForNotif)
  }

  if (showNotificationCenter) {
    val notes = buildList {
      if (weeklyBirthdays.isNotEmpty()) {
        val names = weeklyBirthdays.take(3).joinToString(", ") { "${it.memberName} (${it.displayDate})" }
        add("🎂 ${weeklyBirthdays.size} Church Birthday(s) in the next 7 days: $names")
      }
      if (todayAlertsForNotif.isNotEmpty()) {
        add("${todayAlertsForNotif.size} Pastoral Care follow-up alert(s) scheduled for today.")
      }
    }
    NotificationCenterDialog(
      churchId = churchId,
      userRole = userProfile.userRole,
      currentLanguage = currentLanguage,
      todayVerse = todayVerseResolutionForNotif.verse,
      todayVerseSource = todayVerseResolutionForNotif.source,
      announcements = announcementsForNotif,
      highlightNote = notes.joinToString("\n").ifBlank { null },
      onDismiss = { showNotificationCenter = false }
    )
  }

  BackHandler(enabled = drawerState.isOpen || selectedTab != 0) {
    if (drawerState.isOpen) {
      coroutineScope.launch { drawerState.close() }
    } else if (selectedTab != 0) {
      selectedTab = 0
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ChurchSettingsDrawerContent(
        userProfile = userProfile,
        languageManager = languageManager,
        currentLanguage = currentLanguage,
        viewModel = viewModel,
        onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
        churchConfig = churchConfig
      )
    }
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = { coroutineScope.launch { drawerState.open() } },
              modifier = Modifier.testTag("pastor_drawer_toggle")
            ) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Settings & Language",
                tint = ClassicChurchBlue
              )
            }
          },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_church_logo),
                contentDescription = null,
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(id = R.string.pastor_overview_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = userProfile.displayName.ifBlank { "Pastor" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { showNotificationCenter = true },
            modifier = Modifier
              .size(48.dp)
              .testTag("pastor_notification_center_button")
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = stringResource(id = R.string.notification_center_title),
              tint = ClassicChurchBlue
            )
          }
          // Quick Language Switcher Badge / Button
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = SoftBlueContainer,
            border = BorderStroke(1.dp, SoftBlueBorder),
            modifier = Modifier
              .padding(end = 8.dp)
              .clickable {
                coroutineScope.launch { drawerState.open() }
              }
              .testTag("pastor_quick_language_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (currentLanguage == LanguageManager.LANG_TAMIL) "🇮🇳 தமிழ்" else "🇬🇧 EN",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                )
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("pastor_bottom_nav")
      ) {
        val navItems = listOf(
          Triple(0, stringResource(id = R.string.nav_dashboard), Pair(Icons.Filled.Dashboard, Icons.Outlined.Dashboard)),
          Triple(1, stringResource(id = R.string.nav_members), Pair(Icons.Filled.Group, Icons.Outlined.Group)),
          Triple(2, stringResource(id = R.string.nav_announcements), Pair(Icons.Filled.Campaign, Icons.Outlined.Campaign)),
          Triple(3, stringResource(id = R.string.nav_finance), Pair(Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)),
          Triple(4, stringResource(id = R.string.nav_more), Pair(Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz))
        )

        navItems.forEach { (index, label, icons) ->
          val isSelected = selectedTab == index
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = index },
            icon = {
              Icon(
                imageVector = if (isSelected) icons.first else icons.second,
                contentDescription = label
              )
            },
            label = {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = ClassicChurchBlue,
              selectedTextColor = ClassicChurchBlue,
              indicatorColor = SoftBlueContainer,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted
            )
          )
        }
      }
    },
    containerColor = CleanBackground,
    modifier = modifier.testTag("pastor_dashboard")
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (selectedTab) {
        0 -> PastorHomeTab(
          memberRepository = memberRepository,
          prayerRepository = prayerRepository,
          pastoralCareRepository = pastoralCareRepository,
          bibleContentRepository = bibleContentRepository,
          churchCelebrations = churchCelebrations,
          churchId = churchId,
          currentUid = userProfile.uid,
          pastorName = userProfile.displayName.ifBlank { "Senior Pastor" },
          currentLanguage = currentLanguage,
          onNavigateToMembers = { selectedTab = 1 },
          onNavigateToPrayers = {
            initialMinistryTab = 0
            selectedTab = 4
          },
          onNavigateToCareAlerts = {
            initialMinistryTab = 1
            selectedTab = 4
          },
          onNavigateToCareCells = {
            initialMinistryTab = 2
            selectedTab = 4
          },
          onNavigateToEvents = {
            initialMinistryTab = 3
            selectedTab = 4
          },
          onNavigateToDailyVerse = {
            initialMinistryTab = 5
            selectedTab = 4
          }
        )
        1 -> PastorMembersScreen(
          memberRepository = memberRepository,
          currentPastorUid = userProfile.uid
        )
        2 -> PastorAnnouncementsScreen(
          announcementRepository = announcementRepository,
          pastorName = userProfile.displayName.ifBlank { "Senior Pastor" }
        )
        3 -> PastorFinanceScreen(
          financeRepository = financeRepository,
          memberRepository = memberRepository,
          churchAdminRepository = churchAdminRepository
        )
        4 -> PastorMinistryScreen(
          prayerRepository = prayerRepository,
          churchEventRepository = churchEventRepository,
          sermonRepository = sermonRepository,
          fellowshipRepository = fellowshipRepository,
          pastoralCareRepository = pastoralCareRepository,
          memberRepository = memberRepository,
          bibleContentRepository = bibleContentRepository,
          churchId = churchId,
          currentLanguage = currentLanguage,
          viewModel = viewModel,
          initialTab = initialMinistryTab,
          pastorName = userProfile.displayName.ifBlank { "Senior Pastor" }
        )
      }
    }
  }
}
}

@Composable
private fun PastorHomeTab(
  memberRepository: MemberRepository,
  prayerRepository: PrayerRepository,
  pastoralCareRepository: PastoralCareRepository,
  bibleContentRepository: BibleContentRepository,
  churchCelebrations: List<com.example.core.model.ChurchCelebration>,
  churchId: String,
  currentUid: String,
  pastorName: String,
  currentLanguage: String,
  onNavigateToMembers: () -> Unit = {},
  onNavigateToPrayers: () -> Unit = {},
  onNavigateToCareAlerts: () -> Unit = {},
  onNavigateToCareCells: () -> Unit = {},
  onNavigateToEvents: () -> Unit = {},
  onNavigateToDailyVerse: () -> Unit = {}
) {
  val members by memberRepository.observeAllMembers().collectAsState(initial = emptyList())
  val pendingApps by memberRepository.observePendingApplications().collectAsState(initial = emptyList())
  val prayers by prayerRepository.observeAllPrayerRequests().collectAsState(initial = emptyList())
  val todayAlerts by pastoralCareRepository.observeTodayAlerts().collectAsState(initial = emptyList())
  val customVerses by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val dailyVerseConfig by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val activeVerseResolution = remember(customVerses, dailyVerseConfig) {
    BibleContentRepository.resolveActiveDailyVerse(customVerses, dailyVerseConfig)
  }
  val birthdayCount = todayAlerts.count { it.careType == PastoralCareType.BIRTHDAY }
  val anniversaryCount = todayAlerts.count { it.careType == PastoralCareType.WEDDING_ANNIVERSARY }
  val visitCount = todayAlerts.count { it.careType == PastoralCareType.HOSPITAL_VISIT || it.careType == PastoralCareType.HOME_BLESSING }
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Weekly Church Birthday Reminder (Next 7 Days) for Pastor
    item {
      WeeklyBirthdayReminderCard(
        celebrations = churchCelebrations,
        churchId = churchId,
        currentUid = currentUid,
        actorName = pastorName,
        currentLanguage = currentLanguage,
        celebrationRepository = memberRepository.celebrationRepository
      )
    }

    // Morning Pastoral Care Alert Card
    item {
      Card(
        onClick = onNavigateToCareAlerts,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pastor_morning_care_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, ClassicChurchBlue)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.pastoral_care_title),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ClassicChurchBlue
            ) {
              Text(
                text = "${todayAlerts.size} Today",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextWhite,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (isTamil) {
              "🎂 $birthdayCount பிறந்தநாள் • 💍 $anniversaryCount திருமண நாள் • 🏥 $visitCount சந்திப்பு"
            } else {
              "🎂 $birthdayCount Birthdays • 💍 $anniversaryCount Anniversaries • 🏥 $visitCount Care Visits"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextDeepNavy,
              fontWeight = FontWeight.SemiBold
            )
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onNavigateToCareAlerts,
            colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
          ) {
            Text(
              text = if (isTamil) "வாழ்த்து & சந்திப்பு" else "Send Blessings & Visit",
              color = TextWhite,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onNavigateToCareCells,
              border = BorderStroke(1.dp, ClassicChurchBlue),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("pastor_quick_care_cells_button")
            ) {
              Text(
                text = if (isTamil) "ஐக்கியங்கள்" else "Care Cells",
                color = ClassicChurchBlue,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
            OutlinedButton(
              onClick = onNavigateToEvents,
              border = BorderStroke(1.dp, ClassicChurchBlue),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("pastor_quick_events_button")
            ) {
              Text(
                text = if (isTamil) "நிகழ்வுகள்" else "Manage Events",
                color = ClassicChurchBlue,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }

    // Daily Bible Verse & Auto Rotation Quick Card
    item {
      Card(
        onClick = onNavigateToDailyVerse,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pastor_daily_verse_quick_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isTamil) "தினசரி வேத வசனம்" else "Daily Bible Verse",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SoftBlueContainer
            ) {
              Text(
                text = if (dailyVerseConfig.autoDailyEnabled) "Auto Daily: ON" else "Auto Daily: OFF",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${activeVerseResolution.verse.bookReference} — ${if (isTamil) activeVerseResolution.verse.textTamil else activeVerseResolution.verse.textEnglish}",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 19.sp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = onNavigateToDailyVerse,
            border = BorderStroke(1.dp, ClassicChurchBlue),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = SoftBlueContainer),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("pastor_manage_daily_verse_button")
          ) {
            Text(
              text = if (isTamil) "வசனம் உருவாக்கு & தானியங்கி அமைப்பு" else "Publish Verse & Auto Daily Settings",
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.labelMedium
            )
          }
        }
      }
    }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        PastorMetricCard(
          title = "Total Believers",
          value = "${members.size}",
          subtitle = "Active Directory",
          modifier = Modifier.weight(1f),
          onClick = onNavigateToMembers
        )
        PastorMetricCard(
          title = "Pending Apps",
          value = "${pendingApps.size}",
          subtitle = if (pendingApps.isEmpty()) "All Reviewed" else "Awaiting Review",
          modifier = Modifier.weight(1f),
          highlight = pendingApps.isNotEmpty(),
          onClick = onNavigateToMembers
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        PastorMetricCard(
          title = "Care Alerts",
          value = "${todayAlerts.size}",
          subtitle = "Today's Follow-ups",
          modifier = Modifier.weight(1f),
          onClick = onNavigateToCareAlerts
        )
        PastorMetricCard(
          title = "Prayer Requests",
          value = "${prayers.size}",
          subtitle = "Tap to Respond",
          modifier = Modifier.weight(1f),
          onClick = onNavigateToPrayers
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Pending Membership Applications",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Spacer(modifier = Modifier.height(14.dp))
          val firstPending = pendingApps.firstOrNull()
          if (firstPending != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = firstPending.fullName,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = "Phone: ${firstPending.phoneNumber} • Pending",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSlate,
                    fontWeight = FontWeight.Medium
                  )
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = onNavigateToMembers,
                colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Review", style = MaterialTheme.typography.labelMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
              }
            }
          } else {
            Text(
              text = "All membership applications have been reviewed.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PastorMetricCard(
  title: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  onClick: () -> Unit = {}
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (highlight) SoftBlueContainer else SurfaceWhite
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, SoftBlueBorder)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          color = if (highlight) ClassicChurchBlue else TextSlate,
          fontWeight = FontWeight.Bold
        )
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = if (highlight) ClassicChurchBlue else TextDeepNavy
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextSlate,
          fontWeight = FontWeight.Medium
        )
      )
    }
  }
}
