package com.example.presentation.ministry

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PrayerRequest
import com.example.core.model.PrayerStatus
import com.example.data.content.BibleContentRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.member.MemberRepository
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.content.PastorDailyVerseManager
import com.example.presentation.event.PastorEventsScreen
import com.example.presentation.fellowship.FellowshipScreen
import com.example.presentation.pastoralcare.PastoralCareAlertsScreen
import com.example.presentation.prayer.PastoralPrayerResponseDialog
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

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.core.model.Sermon
import com.example.presentation.sermon.PublishSermonDialog
import kotlinx.coroutines.launch

@Composable
fun PastorMinistryScreen(
  prayerRepository: PrayerRepository,
  churchEventRepository: ChurchEventRepository,
  sermonRepository: SermonRepository,
  fellowshipRepository: FellowshipRepository,
  pastoralCareRepository: PastoralCareRepository,
  memberRepository: MemberRepository,
  bibleContentRepository: BibleContentRepository,
  churchId: String,
  currentLanguage: String,
  viewModel: AuthViewModel,
  initialTab: Int = 0,
  pastorName: String = "Senior Pastor",
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val prayers by prayerRepository.observeAllPrayerRequests().collectAsState(initial = emptyList())
  val sermons by sermonRepository.observeSermons().collectAsState(initial = emptyList())

  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
  var respondingPrayer by remember { mutableStateOf<PrayerRequest?>(null) }
  var showPublishSermonDialog by remember { mutableStateOf(false) }
  var deletingSermon by remember { mutableStateOf<Sermon?>(null) }

  if (respondingPrayer != null) {
    PastoralPrayerResponseDialog(
      prayerRequest = respondingPrayer!!,
      prayerRepository = prayerRepository,
      onDismiss = { respondingPrayer = null }
    )
  }

  if (showPublishSermonDialog) {
    PublishSermonDialog(
      sermonRepository = sermonRepository,
      defaultSpeakerName = pastorName,
      onDismiss = { showPublishSermonDialog = false }
    )
  }

  if (deletingSermon != null) {
    val target = deletingSermon!!
    AlertDialog(
      onDismissRequest = { deletingSermon = null },
      containerColor = SurfaceWhite,
      title = {
        Text(
          text = "Delete Sermon?",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
      },
      text = {
        Text(
          text = "Remove \"${target.titleEnglish}\" from the sermon library?",
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = sermonRepository.deleteSermon(target.sermonId)
              deletingSermon = null
              res.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to delete sermon", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = TextWhite),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Text("Delete", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { deletingSermon = null },
          border = BorderStroke(1.dp, SoftBlueBorder),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CleanBackground)
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Text(
        text = "Ministry Management & Pastoral Care",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    ScrollableTabRow(
      selectedTabIndex = selectedTab,
      containerColor = SurfaceWhite,
      contentColor = ClassicChurchBlue,
      edgePadding = 16.dp,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = ClassicChurchBlue,
          height = 3.dp
        )
      }
    ) {
      val tabs = listOf(
        "Prayers (${prayers.size})",
        "Care Alerts",
        "Care Cells",
        "Events & Services",
        "Sermon Library",
        "Daily Verse",
        "Settings"
      )
      tabs.forEachIndexed { idx, label ->
        Tab(
          selected = selectedTab == idx,
          onClick = { selectedTab = idx },
          text = {
            Text(
              text = label,
              fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
              color = if (selectedTab == idx) ClassicChurchBlue else TextSlate
            )
          }
        )
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      when (selectedTab) {
        0 -> {
          // Prayer Requests
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            if (prayers.isEmpty()) {
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                  border = BorderStroke(1.dp, SoftBlueBorder)
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No prayer requests yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Member prayer requests submitted in the app will appear here.", color = TextSlate, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }
            items(prayers, key = { it.requestId }) { prayer ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SoftBlueBorder)
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Text(
                        text = prayer.requesterName,
                        style = MaterialTheme.typography.titleMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = TextDeepNavy
                        )
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SoftBlueContainer
                      ) {
                        Text(
                          text = prayer.privacy.name.replace('_', ' '),
                          style = MaterialTheme.typography.labelSmall.copy(
                            color = ClassicChurchBlue,
                            fontWeight = FontWeight.Bold
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (prayer.status == PrayerStatus.PRAYED_FOR) SuccessGreenContainer else SoftBlueContainer
                    ) {
                      Text(
                        text = prayer.status.name.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = if (prayer.status == PrayerStatus.PRAYED_FOR) SuccessGreen else ClassicChurchBlue,
                          fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = prayer.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = ClassicChurchBlue
                    )
                  )
                  Text(
                    text = prayer.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate, lineHeight = 20.sp)
                  )

                  if (prayer.pastoralNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = SoftBlueContainer,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(10.dp)) {
                        Text("Pastoral Note (${prayer.pastoralResponseDate}):", fontWeight = FontWeight.Bold, color = ClassicChurchBlue, style = MaterialTheme.typography.labelSmall)
                        Text(prayer.pastoralNote, style = MaterialTheme.typography.bodySmall.copy(color = TextDeepNavy))
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))
                  Button(
                    onClick = { respondingPrayer = prayer },
                    colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                  ) {
                    Text("Respond & Update Status", color = TextWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                  }
                }
              }
            }
          }
        }

        1 -> {
          PastoralCareAlertsScreen(
            pastoralCareRepository = pastoralCareRepository,
            currentLanguage = currentLanguage
          )
        }

        2 -> {
          FellowshipScreen(
            fellowshipRepository = fellowshipRepository,
            currentLanguage = currentLanguage,
            isPastorMode = true,
            memberRepository = memberRepository
          )
        }

        3 -> {
          PastorEventsScreen(
            churchEventRepository = churchEventRepository,
            memberRepository = memberRepository,
            currentLanguage = currentLanguage
          )
        }

        4 -> {
          // Sermons Library Management
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Sermon Library",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextDeepNavy
                    )
                  )
                  Text(
                    text = "${sermons.size} published messages",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = { showPublishSermonDialog = true },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = ClassicChurchBlue,
                    contentColor = TextWhite
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("add_sermon_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Sermon", color = TextWhite, fontWeight = FontWeight.Bold)
                }
              }
            }

            if (sermons.isEmpty()) {
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                  border = BorderStroke(1.dp, SoftBlueBorder)
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No sermons published yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap Add Sermon to publish a message for members.", color = TextSlate, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }

            items(sermons, key = { it.sermonId }) { sermon ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SoftBlueBorder)
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(sermon.titleEnglish, fontWeight = FontWeight.Bold, color = TextDeepNavy, style = MaterialTheme.typography.titleMedium)
                      if (sermon.titleTamil.isNotBlank()) {
                        Text(sermon.titleTamil, fontWeight = FontWeight.SemiBold, color = ClassicChurchBlue, style = MaterialTheme.typography.bodyMedium)
                      }
                    }
                    IconButton(
                      onClick = { deletingSermon = sermon },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_sermon_${sermon.sermonId}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Sermon",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("Preacher: ${sermon.speakerName} • ${sermon.scriptureReference}", color = TextSlate, style = MaterialTheme.typography.bodySmall)
                  Text(sermon.dateDelivered, color = ClassicChurchBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  if (sermon.notesSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(sermon.notesSummary, color = TextSlate, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }
          }
        }

        5 -> {
          PastorDailyVerseManager(
            bibleContentRepository = bibleContentRepository,
            churchId = churchId,
            pastorName = pastorName,
            currentLanguage = currentLanguage
          )
        }

        6 -> {
          // Pastor Account & Session Controls
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, SoftBlueBorder)
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Text(
                    text = "Pastoral Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextDeepNavy
                    )
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Signed in as $pastorName (Approved Pastor). Manage Care Cells, Youth Groups, Church Events, Sermons, and Member Directory in real time.",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TextSlate,
                      lineHeight = 20.sp
                    )
                  )
                }
              }
            }

            item {
              OutlinedButton(
                onClick = { viewModel.signOut(context) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, ErrorRed),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = ErrorRed)
              ) {
                Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
