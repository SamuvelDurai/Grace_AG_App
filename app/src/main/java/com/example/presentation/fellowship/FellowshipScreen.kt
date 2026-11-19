package com.example.presentation.fellowship

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EscalatorWarning
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wc
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.FellowshipCategory
import com.example.core.model.FellowshipGroup
import com.example.core.util.launchActivitySafely
import com.example.data.fellowship.FellowshipRepository
import com.example.data.member.MemberRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun FellowshipScreen(
  fellowshipRepository: FellowshipRepository,
  currentLanguage: String,
  modifier: Modifier = Modifier,
  isPastorMode: Boolean = false,
  memberRepository: MemberRepository? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val groups by fellowshipRepository.observeFellowshipGroups().collectAsState(initial = emptyList())
  var selectedTab by remember { mutableIntStateOf(0) }
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  var showCreateDialog by remember { mutableStateOf(false) }
  var editingGroup by remember { mutableStateOf<FellowshipGroup?>(null) }
  var managingMembersGroup by remember { mutableStateOf<FellowshipGroup?>(null) }
  var deletingGroup by remember { mutableStateOf<FellowshipGroup?>(null) }

  if (showCreateDialog || editingGroup != null) {
    CreateOrEditFellowshipGroupDialog(
      existingGroup = editingGroup,
      isTamil = isTamil,
      fellowshipRepository = fellowshipRepository,
      onDismiss = {
        showCreateDialog = false
        editingGroup = null
      }
    )
  }

  if (managingMembersGroup != null && memberRepository != null) {
    ManageGroupMembersDialog(
      group = managingMembersGroup!!,
      isTamil = isTamil,
      fellowshipRepository = fellowshipRepository,
      memberRepository = memberRepository,
      onDismiss = { managingMembersGroup = null }
    )
  }

  if (deletingGroup != null) {
    val target = deletingGroup!!
    AlertDialog(
      onDismissRequest = { deletingGroup = null },
      containerColor = SurfaceWhite,
      title = {
        Text(
          text = if (isTamil) "குழுவை நீக்கவா?" else "Delete Fellowship Group?",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ErrorRed
          )
        )
      },
      text = {
        Text(
          text = if (isTamil) {
            "\"${target.nameTamil}\" குழுவை நீக்க விரும்புகிறீர்களா?"
          } else {
            "Are you sure you want to remove \"${target.nameEnglish}\"? Congregation directory profiles will not be deleted."
          },
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = fellowshipRepository.deleteGroup(target.groupId)
              deletingGroup = null
              res.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to delete group", Toast.LENGTH_LONG).show()
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
          onClick = { deletingGroup = null },
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

  val filteredGroups = when (selectedTab) {
    0 -> groups
    1 -> groups.filter { it.category == FellowshipCategory.AREA_CARE_CELL }
    2 -> groups.filter { it.category == FellowshipCategory.YOUTH_MINISTRY }
    3 -> groups.filter { it.category == FellowshipCategory.SUNDAY_SCHOOL }
    4 -> groups.filter { it.category == FellowshipCategory.WOMENS_FELLOWSHIP }
    5 -> groups.filter { it.category == FellowshipCategory.MENS_FELLOWSHIP }
    6 -> groups.filter {
      it.category == FellowshipCategory.CHOIR_WORSHIP || it.category == FellowshipCategory.OTHER_GROUP
    }
    else -> groups
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(CleanBackground)
      .testTag("fellowship_screen"),
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
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                  imageVector = Icons.Default.Groups,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = stringResource(id = R.string.care_cells_title),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = stringResource(id = R.string.care_cells_subtitle),
                  style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
                )
              }
            }

            if (isPastorMode) {
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
                  .testTag("create_fellowship_group_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  tint = TextWhite,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isTamil) "புதிய குழு" else "New Group",
                  color = TextWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1
                )
              }
            }
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
        val tabTitles = listOf(
          stringResource(id = R.string.tab_all_cells),
          stringResource(id = R.string.tab_area_cells),
          stringResource(id = R.string.tab_youth),
          stringResource(id = R.string.tab_sunday_school),
          stringResource(id = R.string.tab_womens),
          if (isTamil) "ஆண்கள்" else "Men's",
          if (isTamil) "பாடகர் & மற்றவை" else "Choir & Other"
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
          tabTitles.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.labelSmall.copy(
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

    if (filteredGroups.isEmpty()) {
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
              text = if (isTamil) "இந்த பிரிவில் குழுக்கள் இல்லை" else "No groups in this category yet.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSlate,
                fontWeight = FontWeight.Medium
              )
            )
            if (isPastorMode) {
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
                  text = if (isTamil) "+ புதிய குழுவை உருவாக்கு" else "+ Create First Group",
                  color = TextWhite,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }

    // Care Cells List
    items(filteredGroups, key = { it.groupId }) { group ->
      FellowshipGroupCard(
        group = group,
        isTamil = isTamil,
        isPastorMode = isPastorMode,
        canManageMembers = isPastorMode && memberRepository != null,
        onCallLeader = {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${group.leaderPhone.replace(" ", "")}")
          }
          context.launchActivitySafely(dialIntent, "Dialer unavailable")
        },
        onWhatsAppLeader = {
          val cleanPhone = group.leaderPhone.replace("+", "").replace(" ", "")
          val greetingMsg = if (isTamil) {
            "வணக்கம் போதகர்/தலைவர் அவர்களே, ${group.nameTamil} குடும்ப ஐக்கியம் குறித்து தகவல்கள் அறிய விரும்புகிறேன்."
          } else {
            "Praise the Lord leader, I would like to attend the ${group.nameEnglish} gathering this week."
          }
          val waIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(greetingMsg)}")
          }
          context.launchActivitySafely(waIntent, "WhatsApp not installed")
        },
        onGetDirections = {
          val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(group.meetingLocation)}")
          val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
          context.launchActivitySafely(mapIntent, "Maps app unavailable")
        },
        onManageMembers = { managingMembersGroup = group },
        onEditGroup = { editingGroup = group },
        onDeleteGroup = { deletingGroup = group }
      )
    }
  }
}

@Composable
private fun FellowshipGroupCard(
  group: FellowshipGroup,
  isTamil: Boolean,
  isPastorMode: Boolean,
  canManageMembers: Boolean,
  onCallLeader: () -> Unit,
  onWhatsAppLeader: () -> Unit,
  onGetDirections: () -> Unit,
  onManageMembers: () -> Unit,
  onEditGroup: () -> Unit,
  onDeleteGroup: () -> Unit
) {
  val groupName = if (isTamil) group.nameTamil else group.nameEnglish
  val studyTopic = if (isTamil) group.currentStudyTopicTamil else group.currentStudyTopicEnglish

  val categoryIcon = when (group.category) {
    FellowshipCategory.AREA_CARE_CELL -> Icons.Default.FamilyRestroom
    FellowshipCategory.YOUTH_MINISTRY -> Icons.Default.Groups
    FellowshipCategory.SUNDAY_SCHOOL -> Icons.Default.EscalatorWarning
    FellowshipCategory.WOMENS_FELLOWSHIP -> Icons.Default.VolunteerActivism
    FellowshipCategory.MENS_FELLOWSHIP -> Icons.Default.Wc
    FellowshipCategory.CHOIR_WORSHIP -> Icons.Default.MusicNote
    FellowshipCategory.OTHER_GROUP -> Icons.Default.Groups
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("fellowship_group_${group.groupId}"),
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
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(SoftBlueContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = groupName,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = "${group.category.displayLabel(isTamil)} • ${group.areaZone}",
              style = MaterialTheme.typography.labelSmall.copy(
                color = ClassicChurchBlue,
                fontWeight = FontWeight.SemiBold
              )
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SoftBlueContainer,
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Text(
            text = stringResource(id = R.string.active_members_count, group.memberCount),
            style = MaterialTheme.typography.labelSmall.copy(
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Meeting Schedule & Venue
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CleanBackground,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = group.meetingSchedule,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextDeepNavy
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = TextSlate,
              modifier = Modifier.size(16.dp).padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = group.meetingLocation,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Bible Study Series
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AutoStories,
              contentDescription = null,
              tint = ClassicChurchBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = stringResource(id = R.string.study_topic) + ":",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                )
              )
              Text(
                text = studyTopic,
                style = MaterialTheme.typography.bodySmall.copy(
                  color = TextDeepNavy,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Cell Leader info
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(id = R.string.cell_leader) + ": " + group.leaderName,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          if (group.leaderPhone.isNotBlank()) {
            Text(
              text = group.leaderPhone,
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onCallLeader,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ClassicChurchBlue
          ),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("call_cell_leader_${group.groupId}")
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(id = R.string.call_leader),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = ClassicChurchBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = onWhatsAppLeader,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          ),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("whatsapp_cell_leader_${group.groupId}")
        ) {
          Text(
            text = "💬 WhatsApp",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextWhite,
              fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = onGetDirections,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, SoftBlueBorder),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = ClassicChurchBlue
          ),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("map_directions_${group.groupId}")
        ) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(id = R.string.get_directions),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = ClassicChurchBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (isPastorMode) {
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = SoftBlueBorder)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (canManageMembers) {
            Button(
              onClick = onManageMembers,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SoftBlueContainer,
                contentColor = ClassicChurchBlue
              ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1.4f)
                .height(48.dp)
                .testTag("manage_group_members_${group.groupId}")
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
                  "உறுப்பினர்கள் (${group.memberIds.size})"
                } else {
                  "Members (${group.memberIds.size})"
                },
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          OutlinedButton(
            onClick = onEditGroup,
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
              .testTag("edit_fellowship_group_${group.groupId}")
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
            onClick = onDeleteGroup,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_fellowship_group_${group.groupId}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete group",
              tint = ErrorRed
            )
          }
        }
      }
    }
  }
}
