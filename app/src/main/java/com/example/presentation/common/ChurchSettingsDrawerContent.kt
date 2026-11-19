package com.example.presentation.common

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.ChurchConfig
import com.example.core.model.UserProfile
import com.example.core.notifications.DailyVerseAlarmScheduler
import com.example.core.notifications.NotificationHelper
import com.example.presentation.auth.AuthViewModel
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun ChurchSettingsDrawerContent(
  userProfile: UserProfile,
  languageManager: LanguageManager,
  currentLanguage: String,
  viewModel: AuthViewModel,
  onCloseDrawer: () -> Unit,
  modifier: Modifier = Modifier,
  churchConfig: ChurchConfig = ChurchConfig()
) {
  val context = LocalContext.current
  val churchId = userProfile.churchId.ifBlank { "church_main" }
  val prefs = remember(context) {
    context.applicationContext.getSharedPreferences(NotificationHelper.PREFS_NAME, android.content.Context.MODE_PRIVATE)
  }

  var notificationsEnabled by remember {
    mutableStateOf(prefs.getBoolean(NotificationHelper.PREF_NOTICES_ALERTS, true))
  }
  var verseReminderEnabled by remember {
    mutableStateOf(prefs.getBoolean(NotificationHelper.PREF_DAILY_VERSE, true))
  }
  var hasPermission by remember {
    mutableStateOf(NotificationHelper.hasNotificationPermission(context))
  }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted || NotificationHelper.hasNotificationPermission(context)
    if (granted) {
      DailyVerseAlarmScheduler.syncSchedule(context)
      NotificationHelper.syncFcmSubscriptions(
        context = context,
        churchId = churchId,
        role = userProfile.userRole,
        noticesEnabled = notificationsEnabled,
        dailyVerseEnabled = verseReminderEnabled
      )
      Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Allow notifications in system settings to receive alerts", Toast.LENGTH_LONG).show()
    }
  }

  ModalDrawerSheet(
    drawerContainerColor = CleanBackground,
    modifier = modifier
      .width(320.dp)
      .fillMaxHeight()
      .testTag("settings_drawer_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 24.dp)
    ) {
      // Header with Church Branding & User Info
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(DeepRoyalNavy, ClassicChurchBlue)
            )
          )
          .padding(20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(SurfaceWhite),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_church_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
              )
            }

            IconButton(onClick = onCloseDrawer, modifier = Modifier.size(48.dp)) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Drawer",
                tint = TextWhite
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            if (userProfile.photoUrl.isNotBlank()) {
              MemberAvatar(
                photoUrl = userProfile.photoUrl,
                displayName = userProfile.displayName,
                size = 44.dp
              )
              Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              val resolvedChurchName = churchConfig.name.ifBlank { stringResource(id = R.string.church_name_default) }
              Text(
                text = resolvedChurchName,
                modifier = Modifier.testTag("drawer_header_church_name"),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite
                )
              )
              Text(
                text = userProfile.displayName.ifBlank { "Church Member" },
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextIceBlue,
                  fontWeight = FontWeight.SemiBold
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color.White.copy(alpha = 0.25f)
            ) {
              Text(
                text = userProfile.userRole.name,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextWhite,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            if (userProfile.membershipId.isNotBlank()) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = userProfile.membershipId,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextIceBlue,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // LANGUAGE SWITCHER SECTION
        Card(
          modifier = Modifier.fillMaxWidth().testTag("language_switcher_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                .background(SoftBlueContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = stringResource(id = R.string.select_language),
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = "Resource Localisation",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = ClassicChurchBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 1: English
            val isEnglish = currentLanguage == LanguageManager.LANG_ENGLISH
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isEnglish) SoftBlueContainer else SurfaceWhite,
              border = BorderStroke(
                1.5.dp,
                if (isEnglish) ClassicChurchBlue else SoftBlueBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (!isEnglish) {
                    languageManager.setLanguage(LanguageManager.LANG_ENGLISH)
                  }
                }
                .testTag("lang_option_english")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "🇬🇧",
                    fontSize = 20.sp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = stringResource(id = R.string.lang_english),
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isEnglish) FontWeight.Bold else FontWeight.Medium,
                        color = TextDeepNavy
                      )
                    )
                    Text(
                      text = "English (Default)",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSlate,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                Icon(
                  imageVector = if (isEnglish) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (isEnglish) ClassicChurchBlue else SoftBlueBorder,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option 2: Tamil
            val isTamil = currentLanguage == LanguageManager.LANG_TAMIL
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isTamil) SoftBlueContainer else SurfaceWhite,
              border = BorderStroke(
                1.5.dp,
                if (isTamil) ClassicChurchBlue else SoftBlueBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (!isTamil) {
                    languageManager.setLanguage(LanguageManager.LANG_TAMIL)
                  }
                }
                .testTag("lang_option_tamil")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "🇮🇳",
                    fontSize = 20.sp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = stringResource(id = R.string.lang_tamil),
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isTamil) FontWeight.Bold else FontWeight.Medium,
                        color = TextDeepNavy
                      )
                    )
                    Text(
                      text = "தமிழ் (Tamil Nadu)",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSlate,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                Icon(
                  imageVector = if (isTamil) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (isTamil) ClassicChurchBlue else SoftBlueBorder,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

        // PREFERENCES & NOTIFICATIONS
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.push_alerts_title),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(id = R.string.daily_bible_verse_setting),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextDeepNavy)
                )
                Text(
                  text = stringResource(id = R.string.daily_verse_time_hint),
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                )
              }
              Switch(
                checked = verseReminderEnabled,
                onCheckedChange = { enabled ->
                  verseReminderEnabled = enabled
                  prefs.edit().putBoolean(NotificationHelper.PREF_DAILY_VERSE, enabled).apply()
                  if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasNotificationPermission(context)
                  ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  DailyVerseAlarmScheduler.syncSchedule(context)
                  NotificationHelper.syncFcmSubscriptions(
                    context = context,
                    churchId = churchId,
                    role = userProfile.userRole,
                    noticesEnabled = notificationsEnabled,
                    dailyVerseEnabled = enabled
                  )
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = SurfaceWhite,
                  checkedTrackColor = ClassicChurchBlue
                ),
                modifier = Modifier.testTag("drawer_daily_verse_switch")
              )
            }

            HorizontalDivider(color = SoftBlueBorder, modifier = Modifier.padding(vertical = 8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(id = R.string.notices_alerts_setting),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextDeepNavy)
                )
                Text(
                  text = stringResource(id = R.string.notices_alerts_hint),
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                )
              }
              Switch(
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                  notificationsEnabled = enabled
                  prefs.edit().putBoolean(NotificationHelper.PREF_NOTICES_ALERTS, enabled).apply()
                  if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasNotificationPermission(context)
                  ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  NotificationHelper.syncFcmSubscriptions(
                    context = context,
                    churchId = churchId,
                    role = userProfile.userRole,
                    noticesEnabled = enabled,
                    dailyVerseEnabled = verseReminderEnabled
                  )
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = SurfaceWhite,
                  checkedTrackColor = ClassicChurchBlue
                ),
                modifier = Modifier.testTag("drawer_notices_switch")
              )
            }

            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedButton(
                onClick = {
                  notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SoftBlueBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("drawer_permission_notification_button")
              ) {
                Text(
                  text = "Allow Notifications",
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }
        }

        // CHURCH OFFICE CONTACT
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Church,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.church_info_title),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val officeName = churchConfig.name.ifBlank { stringResource(id = R.string.church_name_default) }
            val officeAddress = churchConfig.address.ifBlank { "12, Cross Street, Chennai, TN" }
            val officeTimings = churchConfig.serviceTimings.ifBlank { stringResource(id = R.string.church_service_timing) }
            val officePhone = churchConfig.phone.trim()
            val officeEmail = churchConfig.email.trim()

            Text(
              text = officeName,
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.testTag("drawer_office_church_name")
            )
            Text(
              text = officeAddress,
              color = TextSlate,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.testTag("drawer_office_address")
            )
            Text(
              text = officeTimings,
              color = ClassicChurchBlue,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag("drawer_office_timings")
            )
            Text(
              text = if (officePhone.isNotBlank()) "Helpline: $officePhone" else stringResource(id = R.string.church_helpline),
              color = TextSlate,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.testTag("drawer_office_phone")
            )
            if (officeEmail.isNotBlank()) {
              Text(
                text = officeEmail,
                color = TextSlate,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("drawer_office_email")
              )
            }
          }
        }

        OutlinedButton(
          onClick = {
            onCloseDrawer()
            viewModel.signOut(context)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
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
}
