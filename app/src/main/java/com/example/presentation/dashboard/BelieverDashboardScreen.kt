package com.example.presentation.dashboard

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import com.example.presentation.common.ChurchSettingsDrawerContent
import com.example.presentation.common.MemberAvatar
import com.example.presentation.common.NotificationCenterDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.BelieverMember
import com.example.core.model.ChurchConfig
import com.example.core.model.DailyVerseConfig
import com.example.core.model.Family
import com.example.core.model.FamilyMemberEntry
import com.example.core.model.PastoralCareType
import com.example.core.model.PaymentMethod
import com.example.core.model.PaymentTransactionStatus
import com.example.core.model.SubscriptionStatus
import com.example.core.model.UserProfile
import com.example.core.util.launchActivitySafely
import com.example.core.util.launchUpiPayment
import com.example.data.admin.ChurchAdminRepository
import com.example.data.announcement.AnnouncementRepository
import com.example.data.content.BibleContentRepository
import com.example.data.content.DailyVerseSource
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.finance.FinanceRepository
import com.example.data.member.MemberRepository
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.fellowship.FellowshipScreen
import com.example.presentation.finance.OfferingPaymentDialog
import com.example.presentation.member.BelieverProfileDetailScreen
import com.example.presentation.member.ConnectFamilyMemberDialog
import com.example.presentation.member.DigitalMembershipCardDialog
import com.example.presentation.member.EditFamilyMemberRoleDialog
import com.example.presentation.pastoralcare.RequestPastoralVisitDialog
import com.example.presentation.prayer.PrayerRequestDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftAmberContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelieverDashboardScreen(
  userProfile: UserProfile,
  viewModel: AuthViewModel,
  memberRepository: MemberRepository,
  bibleContentRepository: BibleContentRepository,
  announcementRepository: AnnouncementRepository,
  prayerRepository: PrayerRepository,
  financeRepository: FinanceRepository,
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
  var initialEventsSubTab by remember { mutableIntStateOf(0) }
  var showDigitalCard by remember { mutableStateOf(false) }
  var isEditingProfile by remember { mutableStateOf(false) }
  var showOfferingDialog by remember { mutableStateOf(false) }
  var showPrayerDialog by remember { mutableStateOf(false) }
  var showPastoralVisitDialog by remember { mutableStateOf(false) }
  var showNotificationCenter by remember { mutableStateOf(false) }
  val churchId = userProfile.churchId.ifBlank { "church_main" }
  val churchConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))
  val announcementsForNotif by announcementRepository.observeAnnouncements(churchId).collectAsState(initial = emptyList())
  val versesForNotif by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val verseConfigForNotif by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val todayVerseResolutionForNotif = remember(versesForNotif, verseConfigForNotif) {
    BibleContentRepository.resolveActiveDailyVerse(versesForNotif, verseConfigForNotif)
  }

  val observedMember by memberRepository.observeMemberByUid(userProfile.uid).collectAsState(initial = null)
  val baseMember = observedMember ?: BelieverMember(
    memberId = "mem_${userProfile.uid}",
    uid = userProfile.uid,
    churchId = userProfile.churchId,
    membershipId = userProfile.membershipId.ifBlank { "Pending ID" },
    fullName = userProfile.displayName.ifBlank { "Church Member" },
    profilePhotoUrl = userProfile.photoUrl,
    phoneNumber = userProfile.phoneNumber,
    email = userProfile.email,
    familyId = userProfile.effectiveFamilyId(),
    familyName = "${userProfile.displayName.ifBlank { "Church Member" }} Family",
    membershipStatus = userProfile.status.name,
    verificationToken = com.example.core.util.ChurchIdGenerator.verificationTokenFor(userProfile.membershipId)
  )

  val observedFamily by memberRepository.familyRepository
    .observeFamilyForMember(userProfile.uid, baseMember.effectiveFamilyId())
    .collectAsState(initial = null)

  LaunchedEffect(observedMember?.uid, observedMember?.membershipId, observedMember?.profilePhotoUrl) {
    if (baseMember.membershipId.isNotBlank() && baseMember.membershipId != "Pending ID") {
      memberRepository.ensureMemberLookup(baseMember)
    }
  }

  LaunchedEffect(observedFamily?.familyId, baseMember.familyId, observedFamily?.headUid) {
    val fam = observedFamily ?: return@LaunchedEffect
    if (fam.familyId.isNotBlank() && fam.familyId != baseMember.familyId) {
      memberRepository.adoptLinkedFamilyForMember(
        member = baseMember,
        targetFamilyId = fam.familyId,
        targetFamilyName = fam.resolvedFamilyName(),
        isHead = fam.headUid == userProfile.uid
      )
    }
  }

  val effectiveMember = if (observedFamily != null) {
    val fam = observedFamily!!
    baseMember.copy(
      profilePhotoUrl = baseMember.profilePhotoUrl.ifBlank { userProfile.photoUrl },
      familyId = fam.familyId.ifBlank { baseMember.effectiveFamilyId() },
      familyName = fam.resolvedFamilyName(),
      familyRole = if (fam.headUid.isBlank() || fam.headUid == userProfile.uid) "FAMILY_HEAD" else "FAMILY_MEMBER"
    )
  } else {
    baseMember.copy(profilePhotoUrl = baseMember.profilePhotoUrl.ifBlank { userProfile.photoUrl })
  }

  if (isEditingProfile) {
    BackHandler { isEditingProfile = false }
    BelieverProfileDetailScreen(
      member = effectiveMember,
      memberRepository = memberRepository,
      onNavigateBack = { isEditingProfile = false }
    )
    return
  }

  if (showDigitalCard) {
    DigitalMembershipCardDialog(
      member = effectiveMember,
      onDismiss = { showDigitalCard = false },
      churchName = churchConfig.name
    )
  }

  if (showOfferingDialog) {
    OfferingPaymentDialog(
      uid = userProfile.uid,
      donorName = effectiveMember.fullName,
      membershipId = effectiveMember.membershipId,
      financeRepository = financeRepository,
      onDismiss = { showOfferingDialog = false },
      churchConfig = churchConfig
    )
  }

  if (showPrayerDialog) {
    PrayerRequestDialog(
      uid = userProfile.uid,
      userName = effectiveMember.fullName,
      prayerRepository = prayerRepository,
      onDismiss = { showPrayerDialog = false }
    )
  }

  if (showPastoralVisitDialog) {
    RequestPastoralVisitDialog(
      uid = userProfile.uid,
      userName = effectiveMember.fullName,
      userPhone = effectiveMember.phoneNumber,
      membershipId = effectiveMember.membershipId,
      pastoralCareRepository = pastoralCareRepository,
      onDismiss = { showPastoralVisitDialog = false }
    )
  }

  if (showNotificationCenter) {
    NotificationCenterDialog(
      churchId = churchId,
      userRole = userProfile.userRole,
      currentLanguage = currentLanguage,
      todayVerse = todayVerseResolutionForNotif.verse,
      todayVerseSource = todayVerseResolutionForNotif.source,
      announcements = announcementsForNotif,
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
        userProfile = userProfile.copy(
          photoUrl = effectiveMember.profilePhotoUrl.ifBlank { userProfile.photoUrl },
          familyId = effectiveMember.effectiveFamilyId()
        ),
        languageManager = languageManager,
        currentLanguage = currentLanguage,
        viewModel = viewModel,
        onCloseDrawer = {
          coroutineScope.launch { drawerState.close() }
        },
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
              modifier = Modifier.testTag("believer_drawer_toggle")
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
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(SoftBlueContainer),
                contentAlignment = Alignment.Center
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_church_logo),
                  contentDescription = null,
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = churchConfig.name.ifBlank { stringResource(id = R.string.church_name_default) },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = stringResource(id = R.string.role_believer) + " • " + effectiveMember.membershipId,
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
                .testTag("believer_notification_center_button")
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
                .testTag("quick_language_button")
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
        modifier = Modifier.testTag("believer_bottom_nav")
      ) {
        val navItems = listOf(
          Triple(0, stringResource(id = R.string.nav_home), Pair(Icons.Filled.Home, Icons.Outlined.Home)),
          Triple(1, stringResource(id = R.string.nav_bible), Pair(Icons.Filled.AutoStories, Icons.Outlined.AutoStories)),
          Triple(2, stringResource(id = R.string.nav_prayer), Pair(Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)),
          Triple(3, stringResource(id = R.string.nav_events), Pair(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)),
          Triple(4, stringResource(id = R.string.nav_profile), Pair(Icons.Filled.Person, Icons.Outlined.Person))
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
            ),
            modifier = Modifier.testTag("tab_${label.lowercase()}")
          )
        }
      }
    },
    containerColor = CleanBackground,
    modifier = modifier.testTag("believer_dashboard")
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (selectedTab) {
        0 -> BelieverHomeTab(
          userProfile = userProfile,
          member = effectiveMember,
          churchId = churchId,
          announcementRepository = announcementRepository,
          financeRepository = financeRepository,
          bibleContentRepository = bibleContentRepository,
          pastoralCareRepository = pastoralCareRepository,
          currentLanguage = currentLanguage,
          onOpenDigitalCard = { showDigitalCard = true },
          onOpenPrayerDialog = { showPrayerDialog = true },
          onOpenOfferingDialog = { showOfferingDialog = true },
          onOpenPastoralVisitDialog = { showPastoralVisitDialog = true },
          onNavigateToSermons = {
            initialEventsSubTab = 1
            selectedTab = 3
          },
          onNavigateToCareCells = {
            initialEventsSubTab = 2
            selectedTab = 3
          }
        )
        1 -> BelieverBibleTab(
          churchId = churchId,
          bibleContentRepository = bibleContentRepository,
          currentLanguage = currentLanguage
        )
        2 -> BelieverPrayerTab(
          uid = userProfile.uid,
          prayerRepository = prayerRepository,
          onOpenPrayerDialog = { showPrayerDialog = true },
          onRequestPastoralVisit = { showPastoralVisitDialog = true }
        )
        3 -> BelieverEventsTab(
          churchEventRepository = churchEventRepository,
          sermonRepository = sermonRepository,
          fellowshipRepository = fellowshipRepository,
          currentLanguage = currentLanguage,
          initialSubTab = initialEventsSubTab
        )
        4 -> BelieverProfileTab(
          userProfile = userProfile,
          member = effectiveMember,
          observedFamily = observedFamily,
          memberRepository = memberRepository,
          financeRepository = financeRepository,
          churchAdminRepository = churchAdminRepository,
          viewModel = viewModel,
          languageManager = languageManager,
          currentLanguage = currentLanguage,
          onOpenDigitalCard = { showDigitalCard = true },
          onEditProfile = { isEditingProfile = true },
          onOpenOfferingDialog = { showOfferingDialog = true }
        )
      }
    }
  }
}
}

@Composable
private fun BelieverHomeTab(
  userProfile: UserProfile,
  member: BelieverMember,
  churchId: String,
  announcementRepository: AnnouncementRepository,
  financeRepository: FinanceRepository,
  bibleContentRepository: BibleContentRepository,
  pastoralCareRepository: PastoralCareRepository,
  currentLanguage: String,
  onOpenDigitalCard: () -> Unit,
  onOpenPrayerDialog: () -> Unit,
  onOpenOfferingDialog: () -> Unit,
  onOpenPastoralVisitDialog: () -> Unit,
  onNavigateToSermons: () -> Unit,
  onNavigateToCareCells: () -> Unit
) {
  val context = LocalContext.current
  val announcements by announcementRepository.observeAnnouncements(churchId).collectAsState(initial = emptyList())
  val customVerses by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val dailyVerseConfig by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val activeVerseResolution = remember(customVerses, dailyVerseConfig) {
    BibleContentRepository.resolveActiveDailyVerse(customVerses, dailyVerseConfig)
  }
  val todayVerse = activeVerseResolution.verse
  val celebrations by pastoralCareRepository.observeTodayCelebrations().collectAsState(initial = emptyList())
  val todayCelebrations = celebrations.filter { it.isToday }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Greeting Hero Card (Rich Blue Gradient)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(DeepRoyalNavy, ClassicChurchBlue)
              )
            )
            .padding(22.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(id = R.string.greeting_hello),
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextIceBlue,
                  fontWeight = FontWeight.Medium
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = member.fullName.ifBlank { "Believer" },
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite
                )
              )
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.2f)
              ) {
                Text(
                  text = stringResource(id = R.string.membership_id_label, member.membershipId),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                  ),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            MemberAvatar(
              photoUrl = member.profilePhotoUrl,
              displayName = member.fullName.ifBlank { userProfile.displayName },
              size = 58.dp
            )
          }
        }
      }
    }

    // Daily Bible Verse Card (Always resolved via Pastor Pin / Auto Daily / Pastor Custom)
    item {
      val sourceBadge = when (activeVerseResolution.source) {
        DailyVerseSource.PASTOR_PINNED_TODAY -> stringResource(id = R.string.verse_badge_pinned)
        DailyVerseSource.AUTO_DAILY -> stringResource(id = R.string.verse_badge_auto)
        DailyVerseSource.PASTOR_CUSTOM -> stringResource(id = R.string.verse_badge_pastor)
      }
      val verseBody = if (currentLanguage == LanguageManager.LANG_TAMIL) {
        todayVerse.textTamil.ifBlank { todayVerse.textEnglish }
      } else {
        todayVerse.textEnglish
      }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("believer_home_daily_verse_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(SoftBlueContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoStories,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = stringResource(id = R.string.daily_verse_title),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = sourceBadge,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoftBlueContainer
              ) {
                Text(
                  text = todayVerse.bookReference,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ClassicChurchBlue
                  ),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              IconButton(
                onClick = {
                  val sendText = "${todayVerse.bookReference}\n\"$verseBody\""
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sendText)
                  }
                  context.launchActivitySafely(Intent.createChooser(intent, "Share Daily Verse"), "No share app available")
                },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("believer_home_share_verse_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share Daily Verse",
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = verseBody,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontStyle = FontStyle.Italic,
              color = TextDeepNavy,
              lineHeight = 26.sp,
              fontWeight = FontWeight.Normal
            )
          )

          val devotional = if (currentLanguage == LanguageManager.LANG_TAMIL) {
            todayVerse.devotionalTamil.ifBlank { todayVerse.devotionalEnglish }
          } else {
            todayVerse.devotionalEnglish
          }
          if (devotional.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = devotional,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 20.sp)
            )
          }
        }
      }
    }

    // Quick Actions Grid
    item {
      Text(
        text = stringResource(id = R.string.quick_actions),
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = stringResource(id = R.string.action_prayer),
          icon = Icons.Default.Favorite,
          onClick = onOpenPrayerDialog,
          modifier = Modifier.weight(1f)
        )
        QuickActionCard(
          title = stringResource(id = R.string.action_offering),
          icon = Icons.Default.VolunteerActivism,
          onClick = onOpenOfferingDialog,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = stringResource(id = R.string.action_sermons),
          icon = Icons.Default.Campaign,
          onClick = onNavigateToSermons,
          modifier = Modifier.weight(1f)
        )
        QuickActionCard(
          title = stringResource(id = R.string.action_membership_card),
          icon = Icons.Default.Badge,
          onClick = onOpenDigitalCard,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = stringResource(id = R.string.tab_area_cells),
          icon = Icons.Default.Groups,
          onClick = onNavigateToCareCells,
          modifier = Modifier.weight(1f).testTag("quick_action_care_cells")
        )
        QuickActionCard(
          title = stringResource(id = R.string.request_pastoral_visit),
          icon = Icons.Default.LocalHospital,
          onClick = onOpenPastoralVisitDialog,
          modifier = Modifier.weight(1f).testTag("quick_action_pastoral_visit")
        )
      }
    }

    // Today's Church Celebrations Banner
    if (todayCelebrations.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("today_celebrations_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.today_celebrations),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            todayCelebrations.forEach { celebration ->
              val iconEmoji = if (celebration.careType == PastoralCareType.BIRTHDAY) "🎂" else "💍"
              val typeText = if (celebration.careType == PastoralCareType.BIRTHDAY) {
                stringResource(id = R.string.birthday_label)
              } else {
                stringResource(id = R.string.anniversary_label)
              }
              Text(
                text = "$iconEmoji ${celebration.memberName} — $typeText",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = TextDeepNavy,
                  fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    // Latest Announcements
    item {
      Text(
        text = stringResource(id = R.string.announcements_title),
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    if (announcements.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No church announcements yet",
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Notices posted by your pastor will appear here.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    items(announcements.take(3), key = { it.announcementId }) { ann ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(shape = RoundedCornerShape(6.dp), color = SoftBlueContainer) {
              Text(
                text = ann.category.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Text(ann.publishDate, style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
          }

          Spacer(modifier = Modifier.height(8.dp))

          val title = if (currentLanguage == LanguageManager.LANG_TAMIL && ann.titleTamil.isNotBlank()) ann.titleTamil else ann.titleEnglish
          val body = if (currentLanguage == LanguageManager.LANG_TAMIL && ann.bodyTamil.isNotBlank()) ann.bodyTamil else ann.bodyEnglish

          Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = body, style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate, lineHeight = 20.sp))
        }
      }
    }
  }
}

@Composable
private fun QuickActionCard(
  title: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, SoftBlueBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(SoftBlueContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = ClassicChurchBlue,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        ),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun BelieverBibleTab(
  churchId: String,
  bibleContentRepository: BibleContentRepository,
  currentLanguage: String
) {
  val customVerses by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val dailyVerseConfig by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val favoriteIds by bibleContentRepository.observeFavoriteVerseIds().collectAsState(initial = emptySet())
  val resolution = remember(customVerses, dailyVerseConfig) {
    BibleContentRepository.resolveActiveDailyVerse(customVerses, dailyVerseConfig)
  }
  val featuredVerse = resolution.verse
  val libraryVerses = remember(resolution, favoriteIds) {
    resolution.rotationPool.map { v ->
      v.copy(isFavorite = v.isFavorite || favoriteIds.contains(v.verseId))
    }
  }
  val featuredIsFav = featuredVerse.isFavorite || favoriteIds.contains(featuredVerse.verseId)
  val sourceBadge = when (resolution.source) {
    DailyVerseSource.PASTOR_PINNED_TODAY -> stringResource(id = R.string.verse_badge_pinned)
    DailyVerseSource.AUTO_DAILY -> stringResource(id = R.string.verse_badge_auto)
    DailyVerseSource.PASTOR_CUSTOM -> stringResource(id = R.string.verse_badge_pastor)
  }
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = stringResource(id = R.string.nav_bible) + " & Devotionals",
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    // Highlighted Today's Featured Verse
    item {
      val featuredText = if (currentLanguage == LanguageManager.LANG_TAMIL) {
        featuredVerse.textTamil.ifBlank { featuredVerse.textEnglish }
      } else {
        featuredVerse.textEnglish
      }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("bible_tab_featured_verse_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, ClassicChurchBlue)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${stringResource(id = R.string.daily_verse_title)} • $sourceBadge",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = featuredVerse.bookReference,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Row {
              IconButton(
                onClick = { bibleContentRepository.toggleFavorite(featuredVerse.verseId) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = if (featuredIsFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "Favorite",
                  tint = if (featuredIsFav) Color(0xFFE11D48) else TextSlate
                )
              }
              IconButton(
                onClick = {
                  val sendText = "${featuredVerse.bookReference}\n\"$featuredText\""
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sendText)
                  }
                  context.launchActivitySafely(Intent.createChooser(intent, "Share Bible Verse"), "No share app available")
                },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ClassicChurchBlue)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = featuredText,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontStyle = FontStyle.Italic,
              color = TextDeepNavy,
              lineHeight = 26.sp,
              fontWeight = FontWeight.Medium
            )
          )
          val featuredDev = if (currentLanguage == LanguageManager.LANG_TAMIL) {
            featuredVerse.devotionalTamil.ifBlank { featuredVerse.devotionalEnglish }
          } else {
            featuredVerse.devotionalEnglish
          }
          if (featuredDev.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(featuredDev, style = MaterialTheme.typography.bodySmall.copy(color = TextDeepNavy, lineHeight = 20.sp))
          }
        }
      }
    }

    item {
      Text(
        text = "Daily Devotional Library (${libraryVerses.size})",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    items(libraryVerses, key = { it.verseId }) { verse ->
      val verseText = if (currentLanguage == LanguageManager.LANG_TAMIL) {
        verse.textTamil.ifBlank { verse.textEnglish }
      } else {
        verse.textEnglish
      }
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
              Text(
                text = verse.bookReference,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
            Row {
              IconButton(
                onClick = { bibleContentRepository.toggleFavorite(verse.verseId) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = if (verse.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "Favorite",
                  tint = if (verse.isFavorite) Color(0xFFE11D48) else TextSlate
                )
              }
              IconButton(
                onClick = {
                  val sendText = "${verse.bookReference}\n\"$verseText\""
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sendText)
                  }
                  context.launchActivitySafely(Intent.createChooser(intent, "Share Bible Verse"), "No share app available")
                },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ClassicChurchBlue)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = verseText,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextDeepNavy, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
          )

          val devotional = if (currentLanguage == LanguageManager.LANG_TAMIL) {
            verse.devotionalTamil.ifBlank { verse.devotionalEnglish }
          } else {
            verse.devotionalEnglish
          }
          if (devotional.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SoftBlueBorder)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Devotional Reflection:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ClassicChurchBlue))
            Spacer(modifier = Modifier.height(4.dp))
            Text(devotional, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 20.sp))
          }
        }
      }
    }
  }
}

@Composable
private fun BelieverPrayerTab(
  uid: String,
  prayerRepository: PrayerRepository,
  onOpenPrayerDialog: () -> Unit,
  onRequestPastoralVisit: () -> Unit
) {
  val myPrayers by prayerRepository.observeUserPrayerRequests(uid).collectAsState(initial = emptyList())

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(id = R.string.nav_prayer),
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = "Share burdens with church prayer team",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
            )
          }

          Button(
            onClick = onOpenPrayerDialog,
            colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Request Prayer", color = TextWhite, fontWeight = FontWeight.Bold)
          }
        }

        OutlinedButton(
          onClick = onRequestPastoralVisit,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = SoftBlueContainer),
          modifier = Modifier.fillMaxWidth().testTag("prayer_tab_request_visit")
        ) {
          Icon(
            imageVector = Icons.Default.LocalHospital,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(id = R.string.request_pastoral_visit),
            color = ClassicChurchBlue,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    if (myPrayers.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = ClassicChurchBlue, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No active prayer requests yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tap 'Request Prayer' to submit a prayer request privately.", color = TextSlate, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
          }
        }
      }
    } else {
      items(myPrayers, key = { it.requestId }) { prayer ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
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
              Text(prayer.title, fontWeight = FontWeight.Bold, color = TextDeepNavy, style = MaterialTheme.typography.titleMedium)
              Surface(shape = RoundedCornerShape(6.dp), color = SoftBlueContainer) {
                Text(
                  text = prayer.status.name.replace('_', ' '),
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(prayer.description, color = TextSlate, style = MaterialTheme.typography.bodyMedium)

            if (prayer.pastoralNote.isNotBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftBlueContainer,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Pastoral Encouragement (${prayer.pastoralResponseDate}):", fontWeight = FontWeight.Bold, color = ClassicChurchBlue, style = MaterialTheme.typography.labelSmall)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(prayer.pastoralNote, style = MaterialTheme.typography.bodySmall.copy(color = TextDeepNavy))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BelieverEventsTab(
  churchEventRepository: ChurchEventRepository,
  sermonRepository: SermonRepository,
  fellowshipRepository: FellowshipRepository,
  currentLanguage: String,
  initialSubTab: Int = 0
) {
  val eventContext = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val events by churchEventRepository.observeEvents().collectAsState(initial = emptyList())
  val sermons by sermonRepository.observeSermons().collectAsState(initial = emptyList())
  var activeTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

  Column(modifier = Modifier.fillMaxSize()) {
    TabRow(
      selectedTabIndex = activeTab,
      containerColor = SurfaceWhite,
      contentColor = ClassicChurchBlue,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          Modifier.tabIndicatorOffset(tabPositions[activeTab]),
          color = ClassicChurchBlue,
          height = 3.dp
        )
      }
    ) {
      Tab(
        selected = activeTab == 0,
        onClick = { activeTab = 0 },
        text = {
          Text(
            "Church Events",
            fontWeight = FontWeight.Bold,
            color = if (activeTab == 0) ClassicChurchBlue else TextSlate
          )
        }
      )
      Tab(
        selected = activeTab == 1,
        onClick = { activeTab = 1 },
        text = {
          Text(
            "Sermons & Audio",
            fontWeight = FontWeight.Bold,
            color = if (activeTab == 1) ClassicChurchBlue else TextSlate
          )
        }
      )
      Tab(
        selected = activeTab == 2,
        onClick = { activeTab = 2 },
        text = {
          Text(
            stringResource(id = R.string.tab_area_cells),
            fontWeight = FontWeight.Bold,
            color = if (activeTab == 2) ClassicChurchBlue else TextSlate
          )
        }
      )
    }

    when (activeTab) {
      0 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (events.isEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SoftBlueBorder)
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("No upcoming church events scheduled", fontWeight = FontWeight.Bold, color = TextDeepNavy)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Events published by your pastor will appear here for RSVP.", color = TextSlate, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
              }
            }
          }
          items(events, key = { it.eventId }) { event ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              border = BorderStroke(1.dp, SoftBlueBorder)
            ) {
              Column(modifier = Modifier.padding(20.dp)) {
                val isTa = currentLanguage == LanguageManager.LANG_TAMIL
                val title = if (isTa && event.titleTamil.isNotBlank()) event.titleTamil else event.titleEnglish
                val desc = if (isTa && event.descriptionTamil.isNotBlank()) event.descriptionTamil else event.descriptionEnglish

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = SoftBlueContainer,
                  border = BorderStroke(1.dp, SoftBlueBorder)
                ) {
                  Text(
                    text = event.category.displayLabel(isTa),
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = ClassicChurchBlue,
                      fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "${event.eventDate} • ${event.startTime}", style = MaterialTheme.typography.bodyMedium.copy(color = ClassicChurchBlue, fontWeight = FontWeight.Bold))
                Text(text = "Venue: ${event.venue}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = desc, style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate, lineHeight = 22.sp))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("${event.registeredCount} / ${event.capacity} registered", style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))

                  Button(
                    onClick = {
                      coroutineScope.launch {
                        val res = churchEventRepository.toggleRsvp(event.eventId)
                        res.onFailure { err ->
                          android.widget.Toast.makeText(
                            eventContext,
                            err.localizedMessage ?: "Unable to update RSVP",
                            android.widget.Toast.LENGTH_SHORT
                          ).show()
                        }
                      }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (event.isRegisteredByCurrentUser) SuccessGreen else ClassicChurchBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text(
                      text = if (event.isRegisteredByCurrentUser) "Registered ✓" else "RSVP Now",
                      color = TextWhite,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        val sermonContext = LocalContext.current
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (sermons.isEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SoftBlueBorder)
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("No sermons published yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Sermon notes and messages shared by your pastor will appear here.", color = TextSlate, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
              }
            }
          }
          items(sermons, key = { it.sermonId }) { sermon ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              border = BorderStroke(1.dp, SoftBlueBorder)
            ) {
              Column(modifier = Modifier.padding(20.dp)) {
                val title = if (currentLanguage == LanguageManager.LANG_TAMIL && sermon.titleTamil.isNotBlank()) sermon.titleTamil else sermon.titleEnglish
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy)
                  )
                  IconButton(
                    onClick = {
                      val shareMsg = "$title\nPreacher: ${sermon.speakerName} (${sermon.scriptureReference})\n\n${sermon.notesSummary}"
                      val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                      }
                      sermonContext.launchActivitySafely(Intent.createChooser(intent, "Share Sermon Notes"), "No share app available")
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Share,
                      contentDescription = "Share Sermon",
                      tint = ClassicChurchBlue
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Speaker: ${sermon.speakerName} • ${sermon.scriptureReference}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
                Text(sermon.dateDelivered, style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
                Text(sermon.notesSummary, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 20.sp))
              }
            }
          }
        }
      }

      2 -> {
        FellowshipScreen(
          fellowshipRepository = fellowshipRepository,
          currentLanguage = currentLanguage
        )
      }
    }
  }
}

@Composable
private fun BelieverProfileTab(
  userProfile: UserProfile,
  member: BelieverMember,
  observedFamily: Family?,
  memberRepository: MemberRepository,
  financeRepository: FinanceRepository,
  churchAdminRepository: ChurchAdminRepository,
  viewModel: AuthViewModel,
  languageManager: LanguageManager,
  currentLanguage: String,
  onOpenDigitalCard: () -> Unit,
  onEditProfile: () -> Unit,
  onOpenOfferingDialog: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val familyId = observedFamily?.familyId?.ifBlank { member.effectiveFamilyId() }
    ?: member.effectiveFamilyId().ifBlank { userProfile.effectiveFamilyId() }
  val familyName = observedFamily?.resolvedFamilyName() ?: member.effectiveFamilyName()
  val isHouseholdHead = observedFamily == null ||
    observedFamily.headUid.isBlank() ||
    observedFamily.headUid == userProfile.uid
  val householdRosterEntries = remember(observedFamily, member.familyMembers) {
    val headId = observedFamily?.headUid.orEmpty()
    val rawList = observedFamily?.dependentMembers ?: member.familyMembers
    rawList.filterNot { it.linkedUid.isNotBlank() && it.linkedUid == headId }
  }
  val householdMembersCount = observedFamily?.totalHouseholdCount() ?: member.totalFamilyMembersCount()

  val subscriptions by financeRepository.observeUserSubscriptions(userProfile.uid, familyId).collectAsState(initial = emptyList())
  val myOfferings by financeRepository.observeUserOfferings(userProfile.uid).collectAsState(initial = emptyList())
  val currentMonthLabel = financeRepository.currentMonthYearLabel()
  val mySub = subscriptions.firstOrNull { it.familyId == familyId && it.monthYear == currentMonthLabel }
    ?: subscriptions.firstOrNull { it.familyId == familyId }
    ?: subscriptions.firstOrNull { it.uid == userProfile.uid }
  val churchConfig by churchAdminRepository.observeChurchConfig(userProfile.churchId.ifBlank { "church_main" })
    .collectAsState(initial = ChurchConfig())

  var showConnectFamilyDialog by remember { mutableStateOf(false) }
  var editingRoleEntry by remember { mutableStateOf<FamilyMemberEntry?>(null) }
  var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI_ONLINE) }
  var subscriptionReference by remember { mutableStateOf("") }
  var isPayingSubscription by remember { mutableStateOf(false) }
  var subscriptionError by remember { mutableStateOf<String?>(null) }

  if (showConnectFamilyDialog) {
    ConnectFamilyMemberDialog(
      churchId = userProfile.churchId.ifBlank { "church_main" },
      memberRepository = memberRepository,
      existingLinkedUids = ((observedFamily?.linkedMemberUids ?: emptyList()) + userProfile.uid).distinct(),
      allowHeadAssignment = isHouseholdHead,
      onDismiss = { showConnectFamilyDialog = false },
      onConnectBeliever = { targetMember, role, makeHead ->
        coroutineScope.launch {
          val connectResult = memberRepository.familyRepository.connectBelieverToFamily(
            familyId = familyId,
            targetMember = targetMember,
            relationshipRole = role,
            makeHead = makeHead,
            defaultHeadMember = member
          )
          showConnectFamilyDialog = false
          connectResult.onSuccess {
            Toast.makeText(context, "${targetMember.fullName} connected as $role!", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Failed to connect believer", Toast.LENGTH_LONG).show()
          }
        }
      },
      onAddManualDependent = { newEntry ->
        val updatedMember = member.copy(
          familyId = familyId,
          familyName = familyName,
          familyMembers = member.familyMembers + newEntry
        )
        coroutineScope.launch {
          val saveResult = memberRepository.saveMemberProfile(updatedMember)
          showConnectFamilyDialog = false
          saveResult.onSuccess {
            Toast.makeText(context, "${newEntry.name} added to family!", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Failed to add family member", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  if (editingRoleEntry != null) {
    EditFamilyMemberRoleDialog(
      entry = editingRoleEntry!!,
      allowPromoteToHead = isHouseholdHead,
      onDismiss = { editingRoleEntry = null },
      onSaveRole = { newRole, promoteToHead ->
        val entry = editingRoleEntry!!
        coroutineScope.launch {
          val res = memberRepository.familyRepository.updateMemberRoleInFamily(
            familyId = familyId,
            entryIdOrUid = entry.id,
            newRole = newRole,
            promoteToHead = promoteToHead
          )
          editingRoleEntry = null
          res.onSuccess {
            Toast.makeText(context, "${entry.name}'s role updated to $newRole", Toast.LENGTH_SHORT).show()
          }.onFailure { err ->
            Toast.makeText(context, err.localizedMessage ?: "Failed to update role", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = stringResource(id = R.string.nav_profile),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            MemberAvatar(
              photoUrl = member.profilePhotoUrl,
              displayName = member.fullName.ifBlank { userProfile.displayName },
              size = 64.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = member.fullName.ifBlank { userProfile.displayName.ifBlank { "Church Member" } },
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SoftBlueContainer
              ) {
                Text(
                  text = stringResource(id = R.string.membership_id_label, member.membershipId),
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.Bold
                  ),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          if (member.phoneNumber.isNotBlank()) {
            Text("Phone: ${member.phoneNumber}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
          if (member.email.isNotBlank()) {
            Text("Email: ${member.email}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
          if (member.dateOfBirth.isNotBlank() || member.gender.isNotBlank()) {
            val bioParts = listOfNotNull(
              member.dateOfBirth.takeIf { it.isNotBlank() }?.let { "DOB: $it" },
              member.gender.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            Text(bioParts, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
          if (member.address.isNotBlank()) {
            Text("Address: ${member.address}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
          if (member.baptismStatus.isNotBlank()) {
            val baptismText = buildString {
              append("Baptism: ${member.baptismStatus.replace('_', ' ')}")
              if (member.baptismDate.isNotBlank()) append(" (${member.baptismDate})")
              if (member.baptismPlace.isNotBlank()) append(" • ${member.baptismPlace}")
            }
            Text(baptismText, style = MaterialTheme.typography.bodySmall.copy(color = ClassicChurchBlue, fontWeight = FontWeight.SemiBold))
          }
          val familyLine = listOfNotNull(
            member.fatherName.takeIf { it.isNotBlank() }?.let { "Father: $it" },
            member.motherName.takeIf { it.isNotBlank() }?.let { "Mother: $it" },
            member.spouseName.takeIf { it.isNotBlank() }?.let { "Spouse: $it" }
          ).joinToString(" • ")
          if (familyLine.isNotBlank()) {
            Text(familyLine, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
          if (member.ministryGroup.isNotBlank()) {
            Text("Ministry / Cell: ${member.ministryGroup}", style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
          }
        }
      }
    }

    // My Church Family Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("believer_family_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(SoftBlueContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.FamilyRestroom,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "My Church Family",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = familyName,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SoftBlueContainer
            ) {
              Text(
                text = "$householdMembersCount Members",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CleanBackground,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Family ID: $familyId • 1 Monthly Subscription covers all family members",
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
          }

          // Household Head Row
          val headDisplayName = observedFamily?.headName?.ifBlank { member.fullName } ?: member.fullName
          val headIsMe = observedFamily == null || observedFamily.headUid.isBlank() || observedFamily.headUid == userProfile.uid
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = SoftBlueContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, SoftBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MemberAvatar(
                photoUrl = if (headIsMe) member.profilePhotoUrl else "",
                displayName = headDisplayName,
                size = 36.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (headIsMe) "$headDisplayName (You)" else headDisplayName,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = if (headIsMe) "Household Head • Can connect & assign roles" else "Household Head",
                  style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue)
                )
              }
              Surface(shape = RoundedCornerShape(6.dp), color = ClassicChurchBlue) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "HEAD",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }

          if (householdRosterEntries.isEmpty()) {
            Text(
              text = "Connect registered believer accounts (Father, Mother, Spouse, Children) or add dependents under one family record.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
            )
          } else {
            householdRosterEntries.forEach { entry ->
              val isSelfEntry = entry.linkedUid.isNotBlank() && entry.linkedUid == userProfile.uid
              val entryPhoto = if (isSelfEntry) member.profilePhotoUrl.ifBlank { entry.profilePhotoUrl } else entry.profilePhotoUrl
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = CleanBackground,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("family_member_row_${entry.id}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  MemberAvatar(
                    photoUrl = entryPhoto,
                    displayName = entry.name,
                    size = 36.dp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = if (isSelfEntry) "${entry.name} (You)" else entry.name,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDeepNavy
                      )
                    )
                    val info = listOfNotNull(
                      entry.relationship.takeIf { it.isNotBlank() },
                      entry.gender.takeIf { it.isNotBlank() },
                      entry.membershipId.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (info.isNotBlank()) {
                      Text(info, style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
                    }
                  }
                  if (isHouseholdHead) {
                    IconButton(
                      onClick = { editingRoleEntry = entry },
                      modifier = Modifier.size(48.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Role for ${entry.name}",
                        tint = ClassicChurchBlue,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                    IconButton(
                      onClick = {
                        coroutineScope.launch {
                          val res = memberRepository.familyRepository.removeDependentMember(familyId, entry.id)
                          res.onSuccess {
                            Toast.makeText(context, "${entry.name} removed from family", Toast.LENGTH_SHORT).show()
                          }.onFailure { err ->
                            Toast.makeText(context, err.localizedMessage ?: "Failed to remove member", Toast.LENGTH_SHORT).show()
                          }
                        }
                      },
                      modifier = Modifier.size(48.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ${entry.name}",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  } else {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = SoftBlueContainer
                    ) {
                      Text(
                        text = entry.relationship.ifBlank { "Member" },
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = ClassicChurchBlue,
                          fontWeight = FontWeight.Bold,
                          fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (isHouseholdHead) {
              OutlinedButton(
                onClick = { showConnectFamilyDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClassicChurchBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = SurfaceWhite,
                  contentColor = ClassicChurchBlue
                ),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("profile_add_family_member_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Link,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Connect / Add Member", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
            OutlinedButton(
              onClick = onEditProfile,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, SoftBlueBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextDeepNavy
              ),
              modifier = Modifier
                .let { if (!isHouseholdHead) it.weight(1f) else it }
                .heightIn(min = 48.dp)
            ) {
              Text("Manage Profile", color = TextDeepNavy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Action Card 1: Digital Membership Card (ID)
    item {
      Card(
        onClick = onOpenDigitalCard,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Digital Membership Card (ID)",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Official digital church pass with verified QR code",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
              )
            }
          }
        }
      }
    }

    // Action Card 2: Edit Church Profile & Baptism Details
    item {
      Card(
        onClick = onEditProfile,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Edit Profile & Baptism Records",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Update photo, address, baptism details & family records",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
              )
            }
          }
        }
      }
    }

    // Action Card 3: Give Tithes & Offerings
    item {
      Card(
        onClick = onOpenOfferingDialog,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.VolunteerActivism,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Give Tithes & Voluntary Offerings",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Support church mission, building fund & general ministry",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
              )
            }
          }
        }
      }
    }

    // Recent Offerings & Verification Status
    if (myOfferings.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("believer_offerings_history_card"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "My Offering Submissions",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            myOfferings.take(3).forEach { tx ->
              val (statusBg, statusFg, statusText) = when (tx.status) {
                PaymentTransactionStatus.SUCCESS -> Triple(SuccessGreenContainer, SuccessGreen, "VERIFIED")
                PaymentTransactionStatus.PENDING -> Triple(SoftAmberContainer, AccentAmber, "PENDING VERIFICATION")
                PaymentTransactionStatus.FAILED -> Triple(ErrorRedContainer, ErrorRed, "REJECTED")
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = CleanBackground,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${tx.category.name.replace('_', ' ')} • ₹${tx.amount.toInt()}",
                      fontWeight = FontWeight.Bold,
                      color = TextDeepNavy,
                      style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                      text = "${tx.transactionDate} • Ref: ${tx.transactionReference.ifBlank { tx.receiptNumber }}",
                      color = TextSlate,
                      style = MaterialTheme.typography.labelSmall
                    )
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(
                      text = statusText,
                      color = statusFg,
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Family Monthly Subscription Section (One Monthly Subscription Per Family)
    item {
      val displayAmount = (mySub?.amountDue ?: churchConfig.monthlyDuesAmount).coerceAtLeast(100.0)
      val displayMonth = mySub?.monthYear?.ifBlank { currentMonthLabel } ?: currentMonthLabel
      val isPaid = mySub?.status == SubscriptionStatus.PAID
      val isPendingVerification = mySub?.status == SubscriptionStatus.PENDING_VERIFICATION
      val (badgeBg, badgeFg, badgeLabel) = when {
        isPaid -> Triple(SuccessGreenContainer, SuccessGreen, "PAID")
        isPendingVerification -> Triple(SoftAmberContainer, AccentAmber, "PENDING VERIFICATION")
        else -> Triple(ErrorRedContainer, ErrorRed, mySub?.status?.name ?: SubscriptionStatus.PENDING.name)
      }

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("family_subscription_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Family Monthly Subscription",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = "1 Subscription Per Family",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                )
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = badgeBg
            ) {
              Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = badgeFg,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Text(
            text = "Household: $familyName ($householdMembersCount member${if (householdMembersCount == 1) "" else "s"})",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
          Text(
            text = "Month: $displayMonth • Family Dues: ₹${displayAmount.toInt()}",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextDeepNavy,
              fontWeight = FontWeight.SemiBold
            )
          )

          if (isPaid) {
            val paidSub = mySub!!
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = SuccessGreenContainer,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Verified Receipt: ${paidSub.receiptNumber} • Paid on ${paidSub.paidDate}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                  )
                )
                val paidByLabel = paidSub.paidByName.ifBlank { member.fullName }
                val methodLabel = paidSub.paymentMethod.ifBlank { "UPI_ONLINE" }.replace('_', ' ')
                Text(
                  text = "Paid by: $paidByLabel ($methodLabel)",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextDeepNavy)
                )
              }
            }
          } else if (isPendingVerification) {
            val pendingSub = mySub!!
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = SoftAmberContainer,
              border = BorderStroke(1.dp, AccentAmber),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "Payment Proof Submitted — Waiting for Pastor / Admin Verification",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold
                  )
                )
                val paidByLabel = pendingSub.paidByName.ifBlank { member.fullName }
                val methodLabel = pendingSub.paymentMethod.ifBlank { "UPI_ONLINE" }.replace('_', ' ')
                val refLabel = pendingSub.transactionReference.ifBlank { pendingSub.receiptNumber }
                Text(
                  text = "Submitted by: $paidByLabel • $methodLabel • Ref: $refLabel",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextDeepNavy)
                )
              }
            }
          } else {
            Text(
              text = "Select Payment Method",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                PaymentMethod.UPI_ONLINE to "UPI",
                PaymentMethod.CARD_NETBANKING to "Bank / NEFT",
                PaymentMethod.CASH_OFFLINE to "Cash"
              ).forEach { (method, label) ->
                val selected = selectedPaymentMethod == method
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (selected) ClassicChurchBlue else SoftBlueContainer,
                  border = BorderStroke(1.dp, if (selected) ClassicChurchBlue else SoftBlueBorder),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedPaymentMethod = method; subscriptionError = null }
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                      color = if (selected) TextWhite else ClassicChurchBlue,
                      fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                  )
                }
              }
            }

            when (selectedPaymentMethod) {
              PaymentMethod.UPI_ONLINE -> {
                if (churchConfig.upiId.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftBlueContainer,
                    border = BorderStroke(1.dp, SoftBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(
                        text = "Church UPI ID: ${churchConfig.upiId}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy)
                      )
                      OutlinedButton(
                        onClick = {
                          context.launchUpiPayment(
                            upiId = churchConfig.upiId,
                            payeeName = churchConfig.name.ifBlank { "GRACE AG" },
                            amountInr = displayAmount,
                            transactionNote = "Family Dues $displayMonth - $familyName"
                          )
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClassicChurchBlue),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ClassicChurchBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open UPI App to Pay ₹${displayAmount.toInt()}", color = ClassicChurchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                      Text(
                        text = "After paying in your UPI app, enter the UTR / Transaction Reference ID below for Pastor verification.",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                      )
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftAmberContainer,
                    border = BorderStroke(1.dp, AccentAmber),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Church UPI ID is not added yet by Admin. Pay Cash at church office, or enter a valid UPI transaction reference if transferred directly.",
                      color = AccentAmber,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                }
              }
              PaymentMethod.CARD_NETBANKING -> {
                if (churchConfig.bankAccountDetails.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftBlueContainer,
                    border = BorderStroke(1.dp, SoftBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Bank Details:\n${churchConfig.bankAccountDetails}\n\nEnter your NEFT/IMPS Reference ID below after transferring.",
                      color = TextDeepNavy,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftAmberContainer,
                    border = BorderStroke(1.dp, AccentAmber),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Online Bank / Card gateway is not added yet. Select Cash to request verification at church office, or enter a Bank Reference ID if transferred.",
                      color = AccentAmber,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                }
              }
              PaymentMethod.CASH_OFFLINE -> {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = CleanBackground,
                  border = BorderStroke(1.dp, SoftBlueBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "Pay ₹${displayAmount.toInt()} in cash at the church office. Submitting will mark your family bill as Pending Verification until Pastor/Admin confirms receipt.",
                    color = TextSlate,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }
            }

            if (selectedPaymentMethod != PaymentMethod.CASH_OFFLINE) {
              OutlinedTextField(
                value = subscriptionReference,
                onValueChange = { subscriptionReference = it; subscriptionError = null },
                label = { Text("UPI / Bank Transaction Reference ID *", color = TextSlate) },
                placeholder = { Text("e.g. 432198765432", color = TextSlate.copy(alpha = 0.6f)) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("subscription_reference_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = ClassicChurchBlue,
                  unfocusedBorderColor = SoftBlueBorder,
                  focusedTextColor = TextDeepNavy,
                  unfocusedTextColor = TextDeepNavy,
                  focusedLabelColor = ClassicChurchBlue,
                  unfocusedLabelColor = TextSlate,
                  focusedContainerColor = SurfaceWhite,
                  unfocusedContainerColor = SurfaceWhite,
                  cursorColor = ClassicChurchBlue
                )
              )
            }

            if (!subscriptionError.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = ErrorRedContainer,
                border = BorderStroke(1.dp, ErrorRed),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = subscriptionError!!,
                  color = ErrorRed,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(10.dp)
                )
              }
            }

            Button(
              onClick = {
                subscriptionError = null
                if (selectedPaymentMethod != PaymentMethod.CASH_OFFLINE && subscriptionReference.trim().length < 6) {
                  subscriptionError = "Transfer ₹${displayAmount.toInt()} first and enter your UTR / Transaction Reference ID (min 6 characters)."
                  return@Button
                }
                isPayingSubscription = true
                coroutineScope.launch {
                  val res = financeRepository.submitFamilySubscriptionPayment(
                    existingSubscriptionId = mySub?.subscriptionId,
                    familyId = familyId,
                    familyName = familyName,
                    familyMemberCount = householdMembersCount,
                    payerUid = userProfile.uid,
                    payerName = member.fullName.ifBlank { userProfile.displayName.ifBlank { "Member" } },
                    membershipId = member.membershipId,
                    amountDue = displayAmount,
                    paymentMethod = selectedPaymentMethod,
                    transactionReference = subscriptionReference,
                    churchId = userProfile.churchId.ifBlank { "church_main" }
                  )
                  isPayingSubscription = false
                  res.onSuccess {
                    subscriptionReference = ""
                    Toast.makeText(context, "Submitted for Pastor verification!", Toast.LENGTH_LONG).show()
                  }.onFailure { err ->
                    subscriptionError = err.localizedMessage ?: "Unable to submit payment proof."
                  }
                }
              },
              enabled = !isPayingSubscription,
              colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("pay_family_subscription_button")
            ) {
              val ctaText = when {
                isPayingSubscription -> "Submitting..."
                selectedPaymentMethod == PaymentMethod.CASH_OFFLINE -> "Request Cash Verification (₹${displayAmount.toInt()})"
                else -> "Submit Payment Proof (₹${displayAmount.toInt()})"
              }
              Text(
                text = ctaText,
                color = TextWhite,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Language Toggle Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(id = R.string.select_language),
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
          }
          Button(
            onClick = {
              val next = if (currentLanguage == LanguageManager.LANG_ENGLISH) {
                LanguageManager.LANG_TAMIL
              } else {
                LanguageManager.LANG_ENGLISH
              }
              languageManager.setLanguage(next)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = if (currentLanguage == LanguageManager.LANG_ENGLISH) "தமிழ்" else "English",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextWhite
              )
            )
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = { viewModel.signOut(context) },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("believer_sign_out_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, ErrorRed),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = ErrorRed
        )
      ) {
        Text(stringResource(id = R.string.sign_out), color = ErrorRed, fontWeight = FontWeight.Bold)
      }
    }
  }
}
