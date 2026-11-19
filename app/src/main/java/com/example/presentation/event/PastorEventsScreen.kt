package com.example.presentation.event

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.LanguageManager
import com.example.core.model.ChurchEvent
import com.example.core.model.EventCategory
import com.example.data.event.ChurchEventRepository
import com.example.data.member.MemberRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PastorEventsScreen(
  churchEventRepository: ChurchEventRepository,
  memberRepository: MemberRepository,
  currentLanguage: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val events by churchEventRepository.observeEvents().collectAsState(initial = emptyList())
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  var selectedTab by remember { mutableIntStateOf(0) }
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingEvent by remember { mutableStateOf<ChurchEvent?>(null) }
  var managingAttendeesEvent by remember { mutableStateOf<ChurchEvent?>(null) }
  var deletingEvent by remember { mutableStateOf<ChurchEvent?>(null) }

  if (showCreateDialog || editingEvent != null) {
    CreateOrEditEventDialog(
      existingEvent = editingEvent,
      isTamil = isTamil,
      churchEventRepository = churchEventRepository,
      onDismiss = {
        showCreateDialog = false
        editingEvent = null
      }
    )
  }

  if (managingAttendeesEvent != null) {
    ManageEventAttendeesDialog(
      event = managingAttendeesEvent!!,
      isTamil = isTamil,
      churchEventRepository = churchEventRepository,
      memberRepository = memberRepository,
      onDismiss = { managingAttendeesEvent = null }
    )
  }

  if (deletingEvent != null) {
    val target = deletingEvent!!
    AlertDialog(
      onDismissRequest = { deletingEvent = null },
      containerColor = SurfaceWhite,
      title = {
        Text(
          text = if (isTamil) "நிகழ்வை நீக்கவா?" else "Delete Church Event?",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ErrorRed
          )
        )
      },
      text = {
        Text(
          text = if (isTamil) {
            "\"${target.titleTamil.ifBlank { target.titleEnglish }}\" நிகழ்வை நீக்க விரும்புகிறீர்களா?"
          } else {
            "Are you sure you want to remove \"${target.titleEnglish}\"? Members will no longer see it on the event calendar."
          },
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = churchEventRepository.deleteEvent(target.eventId)
              deletingEvent = null
              res.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to delete event", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = ErrorRed,
            contentColor = TextWhite
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(if (isTamil) "நீக்கு" else "Delete", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { deletingEvent = null },
          border = BorderStroke(1.dp, SoftBlueBorder),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = TextDeepNavy
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(if (isTamil) "ரத்து" else "Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }

  val filteredEvents = when (selectedTab) {
    0 -> events
    1 -> events.filter { it.category == EventCategory.SUNDAY_SERVICE }
    2 -> events.filter { it.category == EventCategory.FASTING_PRAYER }
    3 -> events.filter { it.category == EventCategory.YOUTH_EVENT }
    4 -> events.filter { it.category == EventCategory.OUTREACH_MISSION }
    5 -> events.filter {
      it.category == EventCategory.SPECIAL_FESTIVAL || it.category == EventCategory.OTHER_EVENT
    }
    else -> events
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(CleanBackground)
      .testTag("pastor_events_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.5.dp, SoftBlueBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SoftBlueContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = if (isTamil) "சபை நிகழ்வுகள் & ஆராதனை" else "Church Events & Services",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = if (isTamil) {
                  "நிகழ்வுகளை உருவாக்கி பங்கேற்பாளர்களை நிர்வகிக்கவும்"
                } else {
                  "Create services, manage RSVPs & assign believers"
                },
                style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = { showCreateDialog = true },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("create_event_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = TextWhite,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isTamil) "புதிய நிகழ்வு" else "New Event",
              color = TextWhite,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              maxLines = 1
            )
          }
        }
      }
    }

    // Category Tabs
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        val tabs = listOf(
          if (isTamil) "அனைத்தும்" else "All Events",
          if (isTamil) "ஆராதனை" else "Services",
          if (isTamil) "உபவாச ஜெபம்" else "Fasting Prayer",
          if (isTamil) "வாலிபர்" else "Youth",
          if (isTamil) "சுவிசேஷம்" else "Outreach",
          if (isTamil) "சிறப்பு" else "Special & Other"
        )
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = SurfaceWhite,
          contentColor = ClassicChurchBlue,
          edgePadding = 12.dp,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = ClassicChurchBlue,
              height = 3.dp
            )
          }
        ) {
          tabs.forEachIndexed { idx, label ->
            Tab(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              text = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == idx) ClassicChurchBlue else TextSlate
                  )
                )
              }
            )
          }
        }
      }
    }

    if (filteredEvents.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isTamil) "இந்தப் பிரிவில் நிகழ்வுகள் இல்லை" else "No church events in this category yet.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSlate,
                fontWeight = FontWeight.Medium
              )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { showCreateDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = ClassicChurchBlue,
                contentColor = TextWhite
              ),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(
                text = if (isTamil) "+ புதிய நிகழ்வை உருவாக்கு" else "+ Create Event",
                color = TextWhite,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    items(filteredEvents, key = { it.eventId }) { event ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pastor_event_card_${event.eventId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.5.dp, SoftBlueBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SoftBlueContainer,
              border = BorderStroke(1.dp, SoftBlueBorder)
            ) {
              Text(
                text = event.category.displayLabel(isTamil),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SuccessGreenContainer
            ) {
              Text(
                text = "${event.registeredCount} / ${event.capacity} RSVP",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SuccessGreen,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = if (isTamil && event.titleTamil.isNotBlank()) event.titleTamil else event.titleEnglish,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          if (!isTamil && event.titleTamil.isNotBlank() && event.titleTamil != event.titleEnglish) {
            Text(
              text = event.titleTamil,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = ClassicChurchBlue
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${event.eventDate} • ${event.startTime} - ${event.endTime}",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = ClassicChurchBlue
              )
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = TextSlate,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = event.venue,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (isTamil && event.descriptionTamil.isNotBlank()) event.descriptionTamil else event.descriptionEnglish,
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextSlate,
              lineHeight = 19.sp
            )
          )

          if (event.organizer.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (isTamil) "ஏற்பாடு: ${event.organizer}" else "Organizer: ${event.organizer}",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextDeepNavy,
                fontWeight = FontWeight.SemiBold
              )
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = SoftBlueBorder)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { managingAttendeesEvent = event },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SoftBlueContainer,
                contentColor = ClassicChurchBlue
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1.4f)
                .height(48.dp)
                .testTag("manage_event_attendees_${event.eventId}")
            ) {
              Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isTamil) {
                  "பங்கேற்பாளர்கள் (${event.attendeeIds.size})"
                } else {
                  "Attendees (${event.attendeeIds.size})"
                },
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            OutlinedButton(
              onClick = { editingEvent = event },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, SoftBlueBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextDeepNavy
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("edit_event_${event.eventId}")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isTamil) "திருத்து" else "Edit",
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
              )
            }

            IconButton(
              onClick = { deletingEvent = event },
              modifier = Modifier
                .size(48.dp)
                .testTag("delete_event_${event.eventId}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete event",
                tint = ErrorRed
              )
            }
          }
        }
      }
    }
  }
}
