package com.example.presentation.common

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.Announcement
import com.example.core.model.BibleVerse
import com.example.core.model.UserRole
import com.example.core.notifications.DailyVerseAlarmScheduler
import com.example.core.notifications.NotificationHelper
import com.example.data.content.DailyVerseSource
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun NotificationCenterDialog(
  churchId: String,
  userRole: UserRole,
  currentLanguage: String,
  todayVerse: BibleVerse,
  todayVerseSource: DailyVerseSource,
  announcements: List<Announcement>,
  highlightNote: String? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL
  val prefs = remember(context) {
    context.applicationContext.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
  }

  var verseReminderEnabled by remember {
    mutableStateOf(prefs.getBoolean(NotificationHelper.PREF_DAILY_VERSE, true))
  }
  var noticesEnabled by remember {
    mutableStateOf(prefs.getBoolean(NotificationHelper.PREF_NOTICES_ALERTS, true))
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
        role = userRole,
        noticesEnabled = noticesEnabled,
        dailyVerseEnabled = verseReminderEnabled
      )
      Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Enable notifications in system settings to receive alerts", Toast.LENGTH_LONG).show()
    }
  }

  val sourceBadgeText = when (todayVerseSource) {
    DailyVerseSource.PASTOR_PINNED_TODAY -> stringResource(id = R.string.verse_badge_pinned)
    DailyVerseSource.AUTO_DAILY -> stringResource(id = R.string.verse_badge_auto)
    DailyVerseSource.PASTOR_CUSTOM -> stringResource(id = R.string.verse_badge_pastor)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SoftBlueContainer
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier
              .padding(8.dp)
              .size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = stringResource(id = R.string.notification_center_title),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = stringResource(id = R.string.notification_center_subtitle),
            style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Today's Daily Verse Preview
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = SoftBlueContainer,
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_center_daily_verse_card")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AutoStories,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = todayVerse.bookReference,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ClassicChurchBlue
                  )
                )
              }
              Surface(shape = RoundedCornerShape(6.dp), color = SurfaceWhite) {
                Text(
                  text = sourceBadgeText,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.Bold
                  ),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (isTamil) todayVerse.textTamil.ifBlank { todayVerse.textEnglish } else todayVerse.textEnglish,
              style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                color = TextDeepNavy,
                lineHeight = 19.sp
              )
            )
          }
        }

        if (!highlightNote.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SuccessGreenContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = highlightNote,
              style = MaterialTheme.typography.labelMedium.copy(
                color = SuccessGreen,
                fontWeight = FontWeight.SemiBold
              ),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // Recent Church Notices
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(id = R.string.announcements_title),
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
          }
          if (announcements.isEmpty()) {
            Text(
              text = "No recent church bulletins yet.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
            )
          } else {
            announcements.take(3).forEach { ann ->
              val title = if (isTamil && ann.titleTamil.isNotBlank()) ann.titleTamil else ann.titleEnglish
              val body = if (isTamil && ann.bodyTamil.isNotBlank()) ann.bodyTamil else ann.bodyEnglish
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, SoftBlueBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = title,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDeepNavy
                      ),
                      modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = ann.publishDate,
                      style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                    )
                  }
                  if (body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = body,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                      style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(color = SoftBlueBorder)

        // Alert Preferences
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(id = R.string.daily_bible_verse_setting),
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextDeepNavy
              )
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
                role = userRole,
                noticesEnabled = noticesEnabled,
                dailyVerseEnabled = enabled
              )
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = SurfaceWhite,
              checkedTrackColor = ClassicChurchBlue
            ),
            modifier = Modifier.testTag("notif_center_daily_verse_switch")
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(id = R.string.notices_alerts_setting),
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextDeepNavy
              )
            )
            Text(
              text = stringResource(id = R.string.notices_alerts_hint),
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
            )
          }
          Switch(
            checked = noticesEnabled,
            onCheckedChange = { enabled ->
              noticesEnabled = enabled
              prefs.edit().putBoolean(NotificationHelper.PREF_NOTICES_ALERTS, enabled).apply()
              if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationHelper.hasNotificationPermission(context)
              ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
              NotificationHelper.syncFcmSubscriptions(
                context = context,
                churchId = churchId,
                role = userRole,
                noticesEnabled = enabled,
                dailyVerseEnabled = verseReminderEnabled
              )
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = SurfaceWhite,
              checkedTrackColor = ClassicChurchBlue
            ),
            modifier = Modifier.testTag("notif_center_notices_switch")
          )
        }

        OutlinedButton(
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              !NotificationHelper.hasNotificationPermission(context)
            ) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              val sent = NotificationHelper.showDailyVerseReminderNow(context)
              Toast.makeText(
                context,
                if (sent) "Sample Daily Verse notification sent" else "Enable system notifications for GRACE AG",
                Toast.LENGTH_SHORT
              ).show()
            }
          },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("notif_center_test_button")
        ) {
          Text(
            text = if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              "Allow Notifications"
            } else {
              "Send Sample Verse Alert"
            },
            color = ClassicChurchBlue,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("notif_center_close_button")
      ) {
        Text("Done", color = TextWhite, fontWeight = FontWeight.Bold)
      }
    }
  )
}
