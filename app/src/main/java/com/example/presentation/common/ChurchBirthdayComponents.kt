package com.example.presentation.common

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.CelebrationType
import com.example.core.model.ChurchCelebration
import com.example.core.util.launchActivitySafely
import com.example.data.pastoralcare.CelebrationRepository
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.SoftAmberContainer
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

@Composable
fun ChurchFamilyBirthdayWishesCard(
  celebrations: List<ChurchCelebration>,
  currentUid: String,
  currentUserName: String,
  currentLanguage: String,
  celebrationRepository: CelebrationRepository,
  modifier: Modifier = Modifier
) {
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val todayList = remember(celebrations) { celebrations.filter { it.isToday() } }
  val upcomingWeek = remember(celebrations) {
    celebrations.filter { val d = it.daysUntil(); d in 1..7 }
  }

  if (todayList.isEmpty() && upcomingWeek.isEmpty()) return

  var wishTarget by remember { mutableStateOf<ChurchCelebration?>(null) }

  if (wishTarget != null) {
    SendChurchFamilyWishDialog(
      celebration = wishTarget!!,
      isTamil = isTamil,
      onDismiss = { wishTarget = null },
      onSend = { msg ->
        val target = wishTarget!!
        scope.launch {
          val res = celebrationRepository.sendChurchFamilyWish(
            celebrationId = target.celebrationId,
            senderUid = currentUid,
            senderName = currentUserName,
            message = msg
          )
          if (res.isSuccess) {
            Toast.makeText(
              context,
              if (isTamil) "${target.memberName} அவர்களுக்கு வாழ்த்து அனுப்பப்பட்டது!" else "Birthday wish sent to ${target.memberName}!",
              Toast.LENGTH_SHORT
            ).show()
            wishTarget = null
          } else {
            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not send wish", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
      .testTag("church_family_birthday_wishes_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(1.5.dp, if (todayList.isNotEmpty()) AccentAmber else SoftBlueBorder)
  ) {
    Column {
      if (todayList.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_church_celebration),
            contentDescription = stringResource(id = R.string.church_family_birthdays_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    DeepRoyalNavy.copy(alpha = 0.88f),
                    DeepRoyalNavy.copy(alpha = 0.52f)
                  )
                )
              )
          )
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(AccentAmber.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Cake,
                  contentDescription = null,
                  tint = AccentAmber,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = stringResource(id = R.string.church_family_birthdays_title),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                  )
                )
                Text(
                  text = stringResource(id = R.string.church_family_birthdays_today_subtitle),
                  style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.88f))
                )
              }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = AccentAmber) {
              Text(
                text = "${todayList.size} Today",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextWhite,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (todayList.isEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftAmberContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(id = R.string.church_family_birthdays_title),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
              Text(
                text = stringResource(id = R.string.church_family_birthdays_week_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
              )
            }
          }
        }
      }

      todayList.forEach { celebration ->
        val alreadyWished = celebration.wishedByUids.contains(currentUid)
        val isAnniversary = celebration.celebrationType == CelebrationType.WEDDING_ANNIVERSARY
        val bannerLine = if (isAnniversary) {
          if (isTamil) {
            "GRACE AG சபைக் குடும்பத்தின் திருமண நாள் வாழ்த்துக்கள் — ${celebration.memberName}!"
          } else {
            "Wedding Anniversary wishes from GRACE AG Church Family to ${celebration.memberName}!"
          }
        } else {
          if (isTamil) {
            "GRACE AG சபைக் குடும்பத்தின் பிறந்தநாள் வாழ்த்துக்கள் — ${celebration.memberName}!"
          } else {
            "Birthday wishes from GRACE AG Church Family to ${celebration.memberName}!"
          }
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = SoftAmberContainer.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MemberAvatar(
                photoUrl = celebration.profilePhotoUrl,
                displayName = celebration.memberName,
                size = 46.dp
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isAnniversary) "💍 ${celebration.memberName}" else "🎂 ${celebration.memberName}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  )
                )
                Text(
                  text = bannerLine,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TextDeepNavy,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                  )
                )
              }
            }

            if (celebration.recentWishes.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                celebration.recentWishes.take(2).forEach { wish ->
                  Text(
                    text = "“${wish.message}” — ${wish.senderName}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = TextSlate,
                      fontStyle = FontStyle.Italic
                    )
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(shape = RoundedCornerShape(10.dp), color = SurfaceWhite) {
                Text(
                  text = "🙏 ${celebration.wishCount} ${if (celebration.wishCount == 1) "Wish" else "Wishes"}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.Bold
                  ),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }

              Button(
                onClick = { wishTarget = celebration },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (alreadyWished) SuccessGreen else ClassicChurchBlue,
                  contentColor = TextWhite
                ),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("send_birthday_wish_${celebration.celebrationId}")
              ) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (alreadyWished) {
                    if (isTamil) "வாழ்த்தப்பட்டது ✓" else "Wished ✓"
                  } else {
                    if (isTamil) "வாழ்த்து அனுப்புக" else "Send Wish"
                  },
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }
        }
      }

      if (upcomingWeek.isNotEmpty()) {
        if (todayList.isNotEmpty()) {
          HorizontalDivider(color = SoftBlueBorder)
        }
        Text(
          text = if (isTamil) "இந்த வாரம் வரும் பிறந்தநாள்கள்" else "Coming Up This Week",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ClassicChurchBlue
          )
        )
        upcomingWeek.take(4).forEach { item ->
          val days = item.daysUntil()
          val dayText = when (days) {
            1 -> if (isTamil) "நாளை" else "Tomorrow"
            else -> if (isTamil) "$days நாட்களில்" else "In $days days"
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              MemberAvatar(
                photoUrl = item.profilePhotoUrl,
                displayName = item.memberName,
                size = 32.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${if (item.celebrationType == CelebrationType.WEDDING_ANNIVERSARY) "💍" else "🎂"} ${item.memberName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TextDeepNavy
                )
              )
            }
            Text(
              text = "${item.displayDate} • $dayText",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSlate,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }
      }
    }
    }
  }
}

@Composable
fun WeeklyBirthdayReminderCard(
  celebrations: List<ChurchCelebration>,
  churchId: String,
  currentUid: String,
  actorName: String,
  currentLanguage: String,
  celebrationRepository: CelebrationRepository,
  modifier: Modifier = Modifier
) {
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val thisWeekList = remember(celebrations) { celebrations.filter { it.isWithinNextWeek() } }
  var showAddDialog by remember { mutableStateOf(false) }
  var wishTarget by remember { mutableStateOf<ChurchCelebration?>(null) }

  if (showAddDialog) {
    AddChurchCelebrationDialog(
      isTamil = isTamil,
      onDismiss = { showAddDialog = false },
      onSave = { name, dateStr, type ->
        scope.launch {
          val res = celebrationRepository.saveCelebration(
            memberName = name,
            rawDate = dateStr,
            type = type,
            churchId = churchId
          )
          if (res.isSuccess) {
            Toast.makeText(context, "Birthday/Celebration saved for $name", Toast.LENGTH_SHORT).show()
            showAddDialog = false
          } else {
            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Invalid date", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  if (wishTarget != null) {
    SendChurchFamilyWishDialog(
      celebration = wishTarget!!,
      isTamil = isTamil,
      onDismiss = { wishTarget = null },
      onSend = { msg ->
        val target = wishTarget!!
        scope.launch {
          val res = celebrationRepository.sendChurchFamilyWish(
            celebrationId = target.celebrationId,
            senderUid = currentUid,
            senderName = actorName,
            message = msg
          )
          if (res.isSuccess) {
            Toast.makeText(context, "Church blessing posted for ${target.memberName}!", Toast.LENGTH_SHORT).show()
            wishTarget = null
          } else {
            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not post wish", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
      .testTag("weekly_birthday_reminder_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.5.dp, AccentAmber)
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(SoftAmberContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = AccentAmber,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = stringResource(id = R.string.weekly_birthday_reminder_title),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = stringResource(id = R.string.weekly_birthday_reminder_subtitle),
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
            )
          }
        }
        OutlinedButton(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, ClassicChurchBlue),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("add_birthday_reminder_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isTamil) "சேர்" else "Add",
            color = ClassicChurchBlue,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }

      if (thisWeekList.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SoftBlueContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (isTamil) {
              "அடுத்த 7 நாட்களில் எந்தப் பிறந்தநாளும் பதிவு செய்யப்படவில்லை. புதிய பிறந்தநாளைச் சேர்க்க '+ சேர்' என்பதைத் தட்டவும்."
            } else {
              "No member birthdays in the next 7 days. Tap '+ Add' to add a member birthday or anniversary, or members' profiles sync automatically when they save their DOB."
            },
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 19.sp),
            modifier = Modifier.padding(12.dp)
          )
        }
      } else {
        thisWeekList.forEach { item ->
          val days = item.daysUntil()
          val countdownBadge = when (days) {
            0 -> if (isTamil) "இன்று" else "TODAY"
            1 -> if (isTamil) "நாளை" else "Tomorrow"
            else -> if (isTamil) "$days நாட்களில்" else "In $days days"
          }
          val isToday = days == 0
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isToday) SoftAmberContainer.copy(alpha = 0.6f) else SoftBlueContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, if (isToday) AccentAmber else SoftBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MemberAvatar(
                photoUrl = item.profilePhotoUrl,
                displayName = item.memberName,
                size = 42.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${if (item.celebrationType == CelebrationType.WEDDING_ANNIVERSARY) "💍" else "🎂"} ${item.memberName}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDeepNavy
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isToday) AccentAmber else ClassicChurchBlue
                  ) {
                    Text(
                      text = countdownBadge,
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text(
                    text = "${item.displayDate} • 🙏 ${item.wishCount}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                  )
                }
              }
              IconButton(
                onClick = { wishTarget = item },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = "Post Church Wish",
                  tint = SuccessGreen,
                  modifier = Modifier.size(20.dp)
                )
              }
              IconButton(
                onClick = {
                  val shareText = if (isTamil) {
                    "🎂 அன்பான ${item.memberName}, GRACE AG சபைக் குடும்பத்தின் இனிய பிறந்தநாள் நல்வாழ்த்துக்கள்! 'கர்த்தர் உன்னை ஆசீர்வதித்து, உன்னைக் காக்கக்கடவர்' — எண்ணாகமம் 6:24. 🙏"
                  } else {
                    "🎂 Happy Birthday ${item.memberName}! Warm birthday wishes from GRACE AG Church Family! 'The LORD bless you and keep you' — Numbers 6:24. 🙏"
                  }
                  val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                  }
                  context.launchActivitySafely(Intent.createChooser(shareIntent, "Share Birthday Wish"), "No share app found")
                },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share Birthday Greeting",
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(20.dp)
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
private fun SendChurchFamilyWishDialog(
  celebration: ChurchCelebration,
  isTamil: Boolean,
  onDismiss: () -> Unit,
  onSend: (String) -> Unit
) {
  val presets = remember(celebration.memberName, isTamil) {
    if (isTamil) {
      listOf(
        "இனிய பிறந்தநாள் நல்வாழ்த்துக்கள் ${celebration.memberName}! கர்த்தர் உங்களை ஆசீர்வதிப்பாராக! 🙏",
        "கர்த்தருடைய கிருபையும் சமாதானமும் இந்த புதிய ஆண்டில் பெருகட்டும்! 🎂"
      )
    } else {
      listOf(
        "Happy Birthday ${celebration.memberName}! May God bless you abundantly this year! 🙏",
        "Wishing you joy, health, and God's richest grace from the GRACE AG Church Family! 🎂"
      )
    }
  }
  var message by remember { mutableStateOf(presets.first()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = if (isTamil) "🎂 ${celebration.memberName} அவர்களுக்கு வாழ்த்து" else "🎂 Wish ${celebration.memberName}",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy)
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = if (isTamil) {
            "GRACE AG சபைக் குடும்பத்தின் சார்பாக உங்கள் வாழ்த்தை அனுப்புங்கள்:"
          } else {
            "Send your blessing from the GRACE AG Church Family:"
          },
          style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
        )
        presets.forEach { preset ->
          Surface(
            onClick = { message = preset },
            shape = RoundedCornerShape(10.dp),
            color = if (message == preset) SoftBlueContainer else SurfaceWhite,
            border = BorderStroke(1.dp, SoftBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = preset,
              style = MaterialTheme.typography.labelSmall.copy(color = TextDeepNavy),
              modifier = Modifier.padding(10.dp)
            )
          }
        }
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text(if (isTamil) "வாழ்த்துச் செய்தி" else "Your Birthday Wish") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("birthday_wish_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (message.isNotBlank()) onSend(message) },
        enabled = message.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("confirm_send_birthday_wish_button")
      ) {
        Text(if (isTamil) "வாழ்த்து அனுப்பு" else "Send Wish", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(if (isTamil) "ரத்து" else "Cancel", color = TextSlate)
      }
    }
  )
}

@Composable
private fun AddChurchCelebrationDialog(
  isTamil: Boolean,
  onDismiss: () -> Unit,
  onSave: (String, String, CelebrationType) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var rawDate by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(CelebrationType.BIRTHDAY) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = if (isTamil) "பிறந்தநாள் / திருமண நாள் சேர்" else "Add Birthday or Anniversary",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy)
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = selectedType == CelebrationType.BIRTHDAY,
            onClick = { selectedType = CelebrationType.BIRTHDAY },
            label = { Text("🎂 Birthday") }
          )
          FilterChip(
            selected = selectedType == CelebrationType.WEDDING_ANNIVERSARY,
            onClick = { selectedType = CelebrationType.WEDDING_ANNIVERSARY },
            label = { Text("💍 Anniversary") }
          )
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(if (isTamil) "உறுப்பினர் பெயர்" else "Member / Couple Name") },
          placeholder = { Text("e.g. Bro. Samuel Durai") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_celebration_name_input")
        )
        OutlinedTextField(
          value = rawDate,
          onValueChange = { rawDate = it },
          label = { Text(if (isTamil) "தேதி (எ.கா: 19 Nov அல்லது 19/11/1995)" else "Date (e.g. 19 Nov or 19/11/1995)") },
          placeholder = { Text("19/11/1995") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_celebration_date_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, rawDate, selectedType) },
        enabled = name.isNotBlank() && rawDate.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("save_celebration_button")
      ) {
        Text(if (isTamil) "சேமி" else "Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(if (isTamil) "ரத்து" else "Cancel", color = TextSlate)
      }
    }
  )
}
