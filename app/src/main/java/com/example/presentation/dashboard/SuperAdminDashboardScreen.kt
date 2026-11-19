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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.AccountStatus
import com.example.core.model.ChurchConfig
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.example.data.admin.ChurchAdminRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.member.MemberRepository
import com.example.presentation.auth.AuthUiState
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.common.ChurchSettingsDrawerContent
import com.example.presentation.common.MemberAvatar
import com.example.presentation.common.WeeklyBirthdayReminderCard
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
  userProfile: UserProfile,
  viewModel: AuthViewModel,
  churchAdminRepository: ChurchAdminRepository,
  memberRepository: MemberRepository,
  fellowshipRepository: FellowshipRepository,
  churchEventRepository: ChurchEventRepository,
  languageManager: LanguageManager,
  currentLanguage: String,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  var selectedTab by remember { mutableIntStateOf(0) }
  val actorName = userProfile.displayName.ifBlank { "Super Admin" }
  val churchId = userProfile.churchId.ifBlank { "church_main" }
  val churchConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))

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
              modifier = Modifier.testTag("admin_drawer_toggle")
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
                  text = stringResource(id = R.string.super_admin_title),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = "Tenant: ${userProfile.churchId.ifBlank { "church_main" }}",
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
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = SoftBlueContainer,
              border = BorderStroke(1.dp, SoftBlueBorder),
              modifier = Modifier
                .padding(end = 8.dp)
                .clickable {
                  coroutineScope.launch { drawerState.open() }
                }
                .testTag("admin_quick_language_button")
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
          modifier = Modifier.testTag("admin_bottom_nav")
        ) {
          val navItems = listOf(
            Triple(0, stringResource(id = R.string.nav_overview), Pair(Icons.Filled.Assessment, Icons.Outlined.Assessment)),
            Triple(1, stringResource(id = R.string.nav_accounts), Pair(Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)),
            Triple(2, stringResource(id = R.string.nav_churches), Pair(Icons.Filled.Church, Icons.Outlined.Church)),
            Triple(3, stringResource(id = R.string.nav_reports), Pair(Icons.Filled.Security, Icons.Outlined.Security)),
            Triple(4, stringResource(id = R.string.nav_settings), Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
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
      modifier = modifier.testTag("super_admin_dashboard")
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        when (selectedTab) {
          0 -> SuperAdminOverviewTab(
            churchAdminRepository = churchAdminRepository,
            memberRepository = memberRepository,
            fellowshipRepository = fellowshipRepository,
            churchEventRepository = churchEventRepository,
            churchId = churchId,
            currentUid = userProfile.uid,
            actorName = actorName,
            currentLanguage = currentLanguage,
            onNavigateToAccounts = { selectedTab = 1 },
            onNavigateToChurchConfig = { selectedTab = 2 },
            onNavigateToAuditLogs = { selectedTab = 3 }
          )
          1 -> AdminAccountsTab(
            churchAdminRepository = churchAdminRepository,
            memberRepository = memberRepository,
            actorName = actorName
          )
          2 -> AdminChurchConfigTab(
            churchAdminRepository = churchAdminRepository,
            churchId = churchId,
            actorName = actorName
          )
          3 -> AdminAuditLogsTab(
            churchAdminRepository = churchAdminRepository
          )
          4 -> AdminSettingsTab(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
private fun SuperAdminOverviewTab(
  churchAdminRepository: ChurchAdminRepository,
  memberRepository: MemberRepository,
  fellowshipRepository: FellowshipRepository,
  churchEventRepository: ChurchEventRepository,
  churchId: String,
  currentUid: String,
  actorName: String,
  currentLanguage: String,
  onNavigateToAccounts: () -> Unit,
  onNavigateToChurchConfig: () -> Unit,
  onNavigateToAuditLogs: () -> Unit
) {
  val churchConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))
  val accounts by churchAdminRepository.observeManagedUsers(churchId).collectAsState(initial = emptyList())
  val members by memberRepository.observeAllMembers().collectAsState(initial = emptyList())
  val groups by fellowshipRepository.observeFellowshipGroups().collectAsState(initial = emptyList())
  val events by churchEventRepository.observeEvents().collectAsState(initial = emptyList())
  val logs by churchAdminRepository.observeAuditLogs(churchId).collectAsState(initial = emptyList())
  val churchCelebrations by memberRepository.celebrationRepository
    .observeChurchCelebrations(churchId)
    .collectAsState(initial = emptyList())

  val pastorCount = accounts.count { it.userRole == UserRole.PASTOR && it.status == AccountStatus.APPROVED }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminStatCard(
          title = "Active Church",
          value = "1",
          subtitle = churchConfig.name,
          modifier = Modifier.weight(1f),
          onClick = onNavigateToChurchConfig
        )
        AdminStatCard(
          title = "Approved Pastors",
          value = "$pastorCount",
          subtitle = "${accounts.size} total accounts",
          modifier = Modifier.weight(1f),
          onClick = onNavigateToAccounts
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminStatCard(
          title = "Registered Believers",
          value = "${members.size}",
          subtitle = "${groups.size} care cells & wings",
          modifier = Modifier.weight(1f),
          onClick = onNavigateToAccounts
        )
        AdminStatCard(
          title = "Events & Audit",
          value = "${events.size}",
          subtitle = "${logs.size} audit records",
          modifier = Modifier.weight(1f),
          isPositive = true,
          onClick = onNavigateToAuditLogs
        )
      }
    }

    item {
      WeeklyBirthdayReminderCard(
        celebrations = churchCelebrations,
        churchId = churchId,
        currentUid = currentUid,
        actorName = actorName,
        currentLanguage = currentLanguage,
        celebrationRepository = memberRepository.celebrationRepository
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
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Quick Pastor & Church Setup",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Create Pastor accounts directly with Email & Temporary Password, review new Believer registrations, and configure church settings.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextSlate,
              lineHeight = 19.sp
            )
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onNavigateToAccounts,
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
          ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Pastor & Manage Accounts", color = TextWhite, fontWeight = FontWeight.Bold)
          }
        }
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
            text = "GRACE AG System Health",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "• Auth & Session: Email/Password, Google Sign-In & Phone OTP with persistent login\n" +
              "• Cloud Firestore: Multi-tenant schema (${churchConfig.churchId}) with offline disk cache\n" +
              "• Security Rules: Zero-trust role & schema validation active\n" +
              "• Monthly Dues Setting: ₹${churchConfig.monthlyDuesAmount.toInt()} • Localization: English & Tamil",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextSlate,
              lineHeight = 22.sp
            )
          )
        }
      }
    }
  }
}

@Composable
private fun AdminAccountsTab(
  churchAdminRepository: ChurchAdminRepository,
  memberRepository: MemberRepository,
  actorName: String
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val accounts by churchAdminRepository.observeManagedUsers().collectAsState(initial = emptyList())
  val members by memberRepository.observeAllMembers().collectAsState(initial = emptyList())
  var selectedFilter by remember { mutableStateOf("ALL") }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isStatusError by remember { mutableStateOf(false) }
  var showCreatePastorDialog by remember { mutableStateOf(false) }

  if (showCreatePastorDialog) {
    CreatePastorAccountDialog(
      churchAdminRepository = churchAdminRepository,
      actorName = actorName,
      onSuccess = { created ->
        showCreatePastorDialog = false
        isStatusError = false
        statusMessage = "Pastor account created for ${created.displayName} (${created.email}). They can now sign in with Email/Password, Google, or Mobile OTP."
      },
      onDismiss = { showCreatePastorDialog = false }
    )
  }

  val filteredAccounts = remember(accounts, selectedFilter) {
    when (selectedFilter) {
      "PASTOR" -> accounts.filter { it.userRole == UserRole.PASTOR }
      "BELIEVER" -> accounts.filter { it.userRole == UserRole.BELIEVER }
      "REVIEW" -> accounts.filter { it.status != AccountStatus.APPROVED }
      else -> accounts
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Pastoral & Member Accounts",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = "Manage roles and approval access across ${accounts.size} accounts",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = { showCreatePastorDialog = true },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("create_pastor_account_button")
      ) {
        Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = null,
          tint = TextWhite,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.create_pastor_account),
          color = TextWhite,
          fontWeight = FontWeight.Bold
        )
      }
    }

    if (statusMessage != null) {
      item {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isStatusError) ErrorRedContainer else SuccessGreenContainer,
          border = BorderStroke(1.dp, if (isStatusError) ErrorRed else SuccessGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = statusMessage!!,
            color = if (isStatusError) ErrorRed else SuccessGreen,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val filters = listOf(
          "ALL" to "All (${accounts.size})",
          "PASTOR" to "Pastors (${accounts.count { it.userRole == UserRole.PASTOR }})",
          "BELIEVER" to "Believers (${accounts.count { it.userRole == UserRole.BELIEVER }})",
          "REVIEW" to "Needs Review (${accounts.count { it.status != AccountStatus.APPROVED }})"
        )
        items(filters, key = { it.first }) { (key, label) ->
          val selected = selectedFilter == key
          FilterChip(
            selected = selected,
            onClick = { selectedFilter = key },
            label = {
              Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextWhite else TextDeepNavy
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
    }

    if (filteredAccounts.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No accounts in this view", fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Registered church users matching this filter will appear here.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(filteredAccounts, key = { it.uid }) { account ->
      val isApproved = account.status == AccountStatus.APPROVED
      val isPastor = account.userRole == UserRole.PASTOR
      val isSuperAdmin = account.userRole == UserRole.SUPER_ADMIN

      val matchedMemberPhoto = members.firstOrNull { it.uid == account.uid }?.profilePhotoUrl.orEmpty()
      val resolvedPhotoUrl = account.photoUrl.ifBlank { matchedMemberPhoto }

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_account_card_${account.uid}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            MemberAvatar(
              photoUrl = resolvedPhotoUrl,
              displayName = account.displayName.ifBlank { "Church User" },
              size = 44.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = account.displayName.ifBlank { "Church User" },
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy,
                style = MaterialTheme.typography.titleMedium
              )
              if (account.phoneNumber.isNotBlank()) {
                Text(account.phoneNumber, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
              }
              if (account.email.isNotBlank()) {
                Text(account.email, style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
              }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(shape = RoundedCornerShape(6.dp), color = SoftBlueContainer) {
                Text(
                  text = account.userRole.name,
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isApproved) SuccessGreenContainer else ErrorRedContainer
              ) {
                Text(
                  text = account.status.name,
                  color = if (isApproved) SuccessGreen else ErrorRed,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }

          if (!isSuperAdmin) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  val nextRole = if (isPastor) UserRole.BELIEVER else UserRole.PASTOR
                  coroutineScope.launch {
                    val result = churchAdminRepository.updateUserRoleAndStatus(
                      uid = account.uid,
                      role = nextRole,
                      status = account.status,
                      actorName = actorName
                    )
                    statusMessage = result.fold(
                      onSuccess = {
                        isStatusError = false
                        "${account.displayName} role updated to ${nextRole.name}"
                      },
                      onFailure = {
                        isStatusError = true
                        "Role update failed: ${it.message ?: "Permission check"}"
                      }
                    )
                  }
                },
                border = BorderStroke(1.dp, ClassicChurchBlue),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = SurfaceWhite,
                  contentColor = ClassicChurchBlue
                ),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
              ) {
                Text(
                  text = if (isPastor) "Set Believer" else "Promote Pastor",
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  style = MaterialTheme.typography.labelSmall
                )
              }

              Button(
                onClick = {
                  val nextStatus = if (isApproved) AccountStatus.SUSPENDED else AccountStatus.APPROVED
                  coroutineScope.launch {
                    val result = churchAdminRepository.updateUserRoleAndStatus(
                      uid = account.uid,
                      role = account.userRole,
                      status = nextStatus,
                      actorName = actorName
                    )
                    statusMessage = result.fold(
                      onSuccess = {
                        isStatusError = false
                        "${account.displayName} marked ${nextStatus.name}"
                      },
                      onFailure = {
                        isStatusError = true
                        "Status update failed: ${it.message ?: "Permission check"}"
                      }
                    )
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isApproved) ErrorRed else ClassicChurchBlue,
                  contentColor = TextWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
              ) {
                Text(
                  text = if (isApproved) "Suspend" else "Approve",
                  color = TextWhite,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  style = MaterialTheme.typography.labelSmall
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminChurchConfigTab(
  churchAdminRepository: ChurchAdminRepository,
  churchId: String,
  actorName: String
) {
  val coroutineScope = rememberCoroutineScope()
  val savedConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))

  var churchName by remember { mutableStateOf(savedConfig.name) }
  var churchAddress by remember { mutableStateOf(savedConfig.address) }
  var contactPhone by remember { mutableStateOf(savedConfig.phone) }
  var contactEmail by remember { mutableStateOf(savedConfig.email) }
  var serviceTimings by remember { mutableStateOf(savedConfig.serviceTimings) }
  var monthlyDuesAmount by remember { mutableStateOf(savedConfig.monthlyDuesAmount.toInt().toString()) }
  var upiId by remember { mutableStateOf(savedConfig.upiId) }
  var bankAccountDetails by remember { mutableStateOf(savedConfig.bankAccountDetails) }
  var latestVersionCode by remember { mutableStateOf(savedConfig.latestVersionCode.toString()) }
  var latestVersionName by remember { mutableStateOf(savedConfig.latestVersionName) }
  var updateDownloadUrl by remember { mutableStateOf(savedConfig.updateDownloadUrl) }
  var updateReleaseNotes by remember { mutableStateOf(savedConfig.updateReleaseNotes) }
  var forceUpdate by remember { mutableStateOf(savedConfig.forceUpdate) }
  var saveMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }

  LaunchedEffect(savedConfig) {
    churchName = savedConfig.name
    churchAddress = savedConfig.address
    contactPhone = savedConfig.phone
    contactEmail = savedConfig.email
    serviceTimings = savedConfig.serviceTimings
    monthlyDuesAmount = savedConfig.monthlyDuesAmount.toInt().toString()
    upiId = savedConfig.upiId
    bankAccountDetails = savedConfig.bankAccountDetails
    latestVersionCode = savedConfig.latestVersionCode.toString()
    latestVersionName = savedConfig.latestVersionName
    updateDownloadUrl = savedConfig.updateDownloadUrl
    updateReleaseNotes = savedConfig.updateReleaseNotes
    forceUpdate = savedConfig.forceUpdate
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Church Profile & Dues Configuration",
        style = MaterialTheme.typography.titleLarge.copy(
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
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (saveMessage != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isError) ErrorRedContainer else SuccessGreenContainer,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = saveMessage!!,
                color = if (isError) ErrorRed else SuccessGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall
              )
            }
          }

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

          OutlinedTextField(
            value = churchName,
            onValueChange = { churchName = it; saveMessage = null },
            label = { Text("Church Name", color = TextSlate) },
            modifier = Modifier.fillMaxWidth().testTag("church_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = churchAddress,
            onValueChange = { churchAddress = it; saveMessage = null },
            label = { Text("Address", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it; saveMessage = null },
            label = { Text("Contact Phone", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = contactEmail,
            onValueChange = { contactEmail = it; saveMessage = null },
            label = { Text("Office Email", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = serviceTimings,
            onValueChange = { serviceTimings = it; saveMessage = null },
            label = { Text("Weekly Service Timings", color = TextSlate) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = monthlyDuesAmount,
            onValueChange = { monthlyDuesAmount = it.filter { ch -> ch.isDigit() }; saveMessage = null },
            label = { Text("Standard Monthly Member Subscription (₹)", color = TextSlate) },
            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = ClassicChurchBlue) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it; saveMessage = null },
            label = { Text("Church UPI ID (Optional — for GPay/PhonePe)", color = TextSlate) },
            placeholder = { Text("e.g. graceag@sbi", color = TextSlate.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth().testTag("church_upi_input"),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          OutlinedTextField(
            value = bankAccountDetails,
            onValueChange = { bankAccountDetails = it; saveMessage = null },
            label = { Text("Bank Account / NEFT Details (Optional)", color = TextSlate) },
            placeholder = { Text("A/C Name, Account No, IFSC", color = TextSlate.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth().testTag("church_bank_input"),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = SoftBlueContainer,
            border = BorderStroke(1.dp, SoftBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "In-App APK Release Updater ($0 Direct)",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                )
              )
              Text(
                text = "Current Installed Build: v${BuildConfig.VERSION_NAME} (Version Code ${BuildConfig.VERSION_CODE}). Set a higher Version Code + GitHub Releases APK URL to show an update pop-up to all members.",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextDeepNavy,
                  lineHeight = 18.sp
                )
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = latestVersionCode,
                  onValueChange = { latestVersionCode = it.filter { ch -> ch.isDigit() }; saveMessage = null },
                  label = { Text("Latest Code", color = TextSlate) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.weight(1f).testTag("church_latest_version_code_input"),
                  shape = RoundedCornerShape(12.dp),
                  colors = fieldColors
                )
                OutlinedTextField(
                  value = latestVersionName,
                  onValueChange = { latestVersionName = it; saveMessage = null },
                  label = { Text("Version Name", color = TextSlate) },
                  placeholder = { Text("1.1", color = TextSlate.copy(alpha = 0.6f)) },
                  modifier = Modifier.weight(1f).testTag("church_latest_version_name_input"),
                  shape = RoundedCornerShape(12.dp),
                  colors = fieldColors
                )
              }
              OutlinedTextField(
                value = updateDownloadUrl,
                onValueChange = { updateDownloadUrl = it; saveMessage = null },
                label = { Text("Direct APK URL (e.g. GitHub Releases)", color = TextSlate) },
                placeholder = { Text("https://github.com/.../grace-ag-v1.1.apk", color = TextSlate.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth().testTag("church_update_url_input"),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
              )
              OutlinedTextField(
                value = updateReleaseNotes,
                onValueChange = { updateReleaseNotes = it; saveMessage = null },
                label = { Text("What's New / Release Notes", color = TextSlate) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("church_update_notes_input"),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Force Mandatory Update",
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextDeepNavy
                    )
                  )
                  Text(
                    text = "Hide the 'Later' button until member updates.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                  )
                }
                Switch(
                  checked = forceUpdate,
                  onCheckedChange = { forceUpdate = it; saveMessage = null },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceWhite,
                    checkedTrackColor = ClassicChurchBlue
                  ),
                  modifier = Modifier.testTag("church_force_update_switch")
                )
              }
            }
          }

          Button(
            onClick = {
              isSaving = true
              coroutineScope.launch {
                val dues = monthlyDuesAmount.toDoubleOrNull() ?: savedConfig.monthlyDuesAmount
                val parsedVerCode = (latestVersionCode.toIntOrNull() ?: savedConfig.latestVersionCode).coerceAtLeast(1)
                val updated = savedConfig.copy(
                  name = churchName,
                  address = churchAddress,
                  phone = contactPhone,
                  email = contactEmail,
                  serviceTimings = serviceTimings,
                  monthlyDuesAmount = dues,
                  upiId = upiId,
                  bankAccountDetails = bankAccountDetails,
                  latestVersionCode = parsedVerCode,
                  latestVersionName = latestVersionName.ifBlank { savedConfig.latestVersionName },
                  updateDownloadUrl = updateDownloadUrl,
                  updateReleaseNotes = updateReleaseNotes,
                  forceUpdate = forceUpdate
                )
                val result = churchAdminRepository.saveChurchConfig(updated, actorName)
                isSaving = false
                result.fold(
                  onSuccess = {
                    isError = false
                    saveMessage = "Church configurations & app release metadata saved."
                  },
                  onFailure = { err ->
                    isError = true
                    saveMessage = err.message ?: "Unable to save configurations."
                  }
                )
              }
            },
            enabled = !isSaving && churchName.isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("save_church_config_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = if (isSaving) "Saving..." else "Save Configurations",
              color = TextWhite,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminAuditLogsTab(
  churchAdminRepository: ChurchAdminRepository
) {
  val logs by churchAdminRepository.observeAuditLogs().collectAsState(initial = emptyList())

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Security Audit Trails & Reconciliations",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
      Text(
        text = "${logs.size} recorded governance events",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
      )
    }

    if (logs.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No audit records yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Role updates and church configuration changes will be recorded here.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(logs, key = { it.logId }) { log ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = log.actorName.ifBlank { "Church System" },
              fontWeight = FontWeight.Bold,
              color = ClassicChurchBlue,
              style = MaterialTheme.typography.labelMedium
            )
            Surface(shape = RoundedCornerShape(4.dp), color = SuccessGreenContainer) {
              Text(
                text = log.status.ifBlank { "SUCCESS" },
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(log.action, color = TextDeepNavy, style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(6.dp))
          Text(log.timestampText, color = TextSlate, style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun AdminStatCard(
  title: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  isPositive: Boolean = false,
  onClick: () -> Unit = {}
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, SoftBlueBorder)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          color = TextSlate,
          fontWeight = FontWeight.Bold
        )
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = if (isPositive) SuccessGreen else ClassicChurchBlue
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextSlate,
          fontWeight = FontWeight.Medium
        )
      )
    }
  }
}

@Composable
private fun CreatePastorAccountDialog(
  churchAdminRepository: ChurchAdminRepository,
  actorName: String,
  onSuccess: (UserProfile) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isCreating by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

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

  AlertDialog(
    onDismissRequest = { if (!isCreating) onDismiss() },
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = stringResource(id = R.string.create_pastor_account),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Provision an approved Pastor account. Share these login details with the Pastor so they can sign in immediately.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
        )

        if (!errorMsg.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ErrorRedContainer,
            border = BorderStroke(1.dp, ErrorRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMsg!!,
              color = ErrorRed,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it; errorMsg = null },
          label = { Text("Pastor Full Name *", color = TextSlate) },
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pastor_name_input")
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it; errorMsg = null },
          label = { Text("Pastor Email *", color = TextSlate) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pastor_email_input")
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it; errorMsg = null },
          label = { Text("Mobile Number (+91) *", color = TextSlate) },
          placeholder = { Text("9876543210", color = TextMuted) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pastor_phone_input")
        )

        OutlinedTextField(
          value = password,
          onValueChange = { password = it; errorMsg = null },
          label = { Text("Temporary Password (min 6 chars) *", color = TextSlate) },
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                tint = TextSlate
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          colors = fieldColors,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pastor_password_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          isCreating = true
          errorMsg = null
          coroutineScope.launch {
            val result = churchAdminRepository.createPastorAccount(
              context = context,
              fullName = fullName,
              email = email,
              phoneNumber = phone,
              password = password,
              actorName = actorName
            )
            isCreating = false
            result.onSuccess { created ->
              onSuccess(created)
            }.onFailure { err ->
              errorMsg = err.localizedMessage ?: "Failed to create Pastor account."
            }
          }
        },
        enabled = !isCreating && fullName.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && password.length >= 6,
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("confirm_create_pastor_button")
      ) {
        if (isCreating) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextWhite, strokeWidth = 2.dp)
        } else {
          Text("Create Account", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        enabled = !isCreating,
        border = BorderStroke(1.dp, SoftBlueBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}

@Composable
private fun AdminSettingsTab(viewModel: AuthViewModel) {
  val context = LocalContext.current
  val authState by viewModel.uiState.collectAsState()
  val adminProfile = (authState as? AuthUiState.Authenticated)?.profile

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Super Admin Controls",
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        color = TextDeepNavy
      )
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      border = BorderStroke(1.dp, SoftBlueBorder)
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Administrator Session & Cloud Security",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
        if (adminProfile != null) {
          Text(
            text = "Signed in as ${adminProfile.displayName.ifBlank { "Super Admin" }} (${adminProfile.email.ifBlank { adminProfile.phoneNumber }})",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
        }
        Text(
          text = "• Your session stays signed in automatically until you tap Sign Out.\n" +
            "• Only Super Admin can create Pastor accounts, assign Church roles, and update church dues.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 20.sp)
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    OutlinedButton(
      onClick = { viewModel.signOut(context) },
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.5.dp, ErrorRed),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = SurfaceWhite,
        contentColor = ErrorRed
      )
    ) {
      Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
    }
  }
}
