package com.example.presentation.pastoralcare

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.PastoralCareAlert
import com.example.core.model.PastoralCareStatus
import com.example.core.model.PastoralCareType
import com.example.core.util.launchActivitySafely
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun PastoralCareAlertsScreen(
  pastoralCareRepository: PastoralCareRepository,
  currentLanguage: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val alerts by pastoralCareRepository.observeAlerts().collectAsState(initial = emptyList())
  var selectedTab by remember { mutableIntStateOf(0) }
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  val filteredAlerts = when (selectedTab) {
    0 -> alerts
    1 -> alerts.filter { it.isToday }
    2 -> alerts.filter { it.careType == PastoralCareType.HOSPITAL_VISIT || it.careType == PastoralCareType.HOME_BLESSING || it.careType == PastoralCareType.BEREAVEMENT_CARE }
    else -> alerts
  }

  val todayBirthdaysCount = alerts.count { it.isToday && it.careType == PastoralCareType.BIRTHDAY }
  val todayAnniversaryCount = alerts.count { it.isToday && it.careType == PastoralCareType.WEDDING_ANNIVERSARY }
  val todayHospitalCount = alerts.count { it.isToday && it.careType == PastoralCareType.HOSPITAL_VISIT }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(CleanBackground)
      .testTag("pastoral_care_alerts_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Hero Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.5.dp, SoftBlueBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(id = R.string.pastoral_care_title),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = stringResource(id = R.string.pastoral_care_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Metric Counters
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            MetricCard(
              title = if (isTamil) "பிறந்தநாள்" else "Birthdays",
              count = todayBirthdaysCount.toString(),
              emoji = "🎂",
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = if (isTamil) "திருமணம்" else "Anniversaries",
              count = todayAnniversaryCount.toString(),
              emoji = "💍",
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = if (isTamil) "சந்திப்புகள்" else "Visits",
              count = todayHospitalCount.toString(),
              emoji = "🏥",
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Tabs
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        val tabTitles = listOf(
          if (isTamil) "அனைத்தும்" else "All Alerts",
          if (isTamil) "இன்று" else "Today",
          if (isTamil) "சந்திப்பு தேவைகள்" else "Visits & Needs"
        )
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = SurfaceWhite,
          contentColor = ClassicChurchBlue,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = ClassicChurchBlue,
              height = 3.dp
            )
          }
        ) {
          tabTitles.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == index) ClassicChurchBlue else TextSlate
                  )
                )
              }
            )
          }
        }
      }
    }

    // Alerts List
    if (filteredAlerts.isEmpty()) {
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
            Text(
              text = if (isTamil) "நல்வாழ்வு அறிவிப்புகள் இல்லை" else "No pastoral care alerts yet",
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isTamil) "உறுப்பினர் சந்திப்பு கோரிக்கைகள் இங்கே தோன்றும்." else "Hospital visits, home blessings, and member care alerts will appear here.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(filteredAlerts, key = { it.alertId }) { alert ->
      PastoralAlertCard(
        alert = alert,
        isTamil = isTamil,
        onUpdateStatus = { newStatus ->
          coroutineScope.launch {
            val result = pastoralCareRepository.updateAlertStatus(alert.alertId, newStatus)
            result.onFailure { err ->
              Toast.makeText(context, err.localizedMessage ?: "Failed to update alert", Toast.LENGTH_SHORT).show()
            }
          }
        },
        onCallMember = {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${alert.memberPhone.replace(" ", "")}")
          }
          context.launchActivitySafely(dialIntent, "Dialer unavailable")
        },
        onWhatsAppBlessing = {
          val blessingText = alert.generateBlessingMessage(isTamil)
          val cleanPhone = alert.memberPhone.replace("+", "").replace(" ", "")
          val encodedMsg = Uri.encode(blessingText)
          val waIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
          }
          context.launchActivitySafely(waIntent, "WhatsApp not installed")
        }
      )
    }
  }
}

@Composable
private fun MetricCard(
  title: String,
  count: String,
  emoji: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = SoftBlueContainer,
    border = BorderStroke(1.dp, SoftBlueBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = count,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextSlate,
          fontSize = 11.sp
        ),
        maxLines = 1
      )
    }
  }
}

@Composable
private fun PastoralAlertCard(
  alert: PastoralCareAlert,
  isTamil: Boolean,
  onUpdateStatus: (PastoralCareStatus) -> Unit,
  onCallMember: () -> Unit,
  onWhatsAppBlessing: () -> Unit
) {
  val typeLabel = when (alert.careType) {
    PastoralCareType.BIRTHDAY -> if (isTamil) "பிறந்தநாள்" else "Birthday"
    PastoralCareType.WEDDING_ANNIVERSARY -> if (isTamil) "திருமண நாள்" else "Wedding Anniversary"
    PastoralCareType.HOSPITAL_VISIT -> if (isTamil) "மருத்துவமனை சந்திப்பு" else "Hospital Visit"
    PastoralCareType.HOME_BLESSING -> if (isTamil) "இல்ல ஆசீர்வாதம்" else "Home Blessing"
    PastoralCareType.BEREAVEMENT_CARE -> if (isTamil) "துயர ஆறுதல்" else "Bereavement Care"
  }

  val typeIcon = when (alert.careType) {
    PastoralCareType.BIRTHDAY -> Icons.Default.Cake
    PastoralCareType.WEDDING_ANNIVERSARY -> Icons.Default.Favorite
    PastoralCareType.HOSPITAL_VISIT -> Icons.Default.LocalHospital
    PastoralCareType.HOME_BLESSING -> Icons.Default.Home
    PastoralCareType.BEREAVEMENT_CARE -> Icons.Default.VolunteerActivism
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pastoral_alert_${alert.alertId}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    border = BorderStroke(1.5.dp, SoftBlueBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
              imageVector = typeIcon,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = alert.memberName,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = "${alert.membershipId} • ${alert.memberPhone}",
              style = MaterialTheme.typography.labelSmall.copy(
                color = ClassicChurchBlue,
                fontWeight = FontWeight.SemiBold
              )
            )
          }
        }

        // Status Badge
        val (statusText, statusBg, statusColor) = when (alert.status) {
          PastoralCareStatus.PENDING -> Triple("Pending", SoftBlueContainer, ClassicChurchBlue)
          PastoralCareStatus.CALLED -> Triple("Called", SoftBlueContainer, ClassicChurchBlue)
          PastoralCareStatus.VISITED -> Triple("Visited", SuccessGreenContainer, SuccessGreen)
          PastoralCareStatus.PRAYED -> Triple("Prayed", SuccessGreenContainer, SuccessGreen)
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusBg,
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(
              color = statusColor,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Event Details
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = CleanBackground,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = typeLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ClassicChurchBlue
              )
            )
            if (alert.milestoneYear != null) {
              Text(
                text = " • ${alert.milestoneYear}th Milestone",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TextDeepNavy
                )
              )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
              text = alert.eventDate,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (alert.isToday) SuccessGreen else TextSlate
              )
            )
          }

          if (alert.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = alert.notes,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
            )
          }

          if (alert.locationDetails.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "📍 ${alert.locationDetails}",
              style = MaterialTheme.typography.labelSmall.copy(color = TextDeepNavy)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick Actions Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Call button
        OutlinedButton(
          onClick = onCallMember,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ClassicChurchBlue
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("call_member_${alert.alertId}")
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isTamil) "அழைப்பு" else "Call",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ClassicChurchBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // WhatsApp Blessing Button
        Button(
          onClick = onWhatsAppBlessing,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.weight(1.5f).height(48.dp).testTag("whatsapp_blessing_${alert.alertId}")
        ) {
          Text(
            text = if (isTamil) "💬 வேத வாழ்த்து" else "💬 Send Blessing",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextWhite
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Mark Prayed / Visited Toggle
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (alert.status == PastoralCareStatus.PRAYED || alert.status == PastoralCareStatus.VISITED) SuccessGreenContainer else SoftBlueContainer,
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier
            .clickable {
              val nextStatus = if (alert.careType == PastoralCareType.HOSPITAL_VISIT || alert.careType == PastoralCareType.HOME_BLESSING) {
                if (alert.status == PastoralCareStatus.VISITED) PastoralCareStatus.PENDING else PastoralCareStatus.VISITED
              } else {
                if (alert.status == PastoralCareStatus.PRAYED) PastoralCareStatus.PENDING else PastoralCareStatus.PRAYED
              }
              onUpdateStatus(nextStatus)
            }
            .height(48.dp)
            .width(48.dp)
            .testTag("toggle_status_${alert.alertId}")
        ) {
          Box(
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Mark Prayed",
              tint = if (alert.status == PastoralCareStatus.PRAYED || alert.status == PastoralCareStatus.VISITED) SuccessGreen else ClassicChurchBlue,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}
