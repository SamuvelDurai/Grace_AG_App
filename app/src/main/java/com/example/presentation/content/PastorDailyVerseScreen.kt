package com.example.presentation.content

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.BibleVerse
import com.example.core.model.DailyVerseConfig
import com.example.data.content.BibleContentRepository
import com.example.data.content.DailyVerseSource
import com.example.ui.theme.ClassicChurchBlue
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
fun PastorDailyVerseManager(
  bibleContentRepository: BibleContentRepository,
  churchId: String,
  pastorName: String,
  currentLanguage: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val customVerses by bibleContentRepository.observeDailyVerses(churchId).collectAsState(initial = emptyList())
  val dailyConfig by bibleContentRepository.observeDailyVerseConfig(churchId)
    .collectAsState(initial = DailyVerseConfig(churchId = churchId))
  val isTamil = currentLanguage == LanguageManager.LANG_TAMIL

  val resolution = remember(customVerses, dailyConfig) {
    BibleContentRepository.resolveActiveDailyVerse(customVerses, dailyConfig)
  }

  var showPublishDialog by remember { mutableStateOf(false) }
  var deletingVerse by remember { mutableStateOf<BibleVerse?>(null) }
  var isTogglingAuto by remember { mutableStateOf(false) }

  if (showPublishDialog) {
    PublishDailyVerseDialog(
      bibleContentRepository = bibleContentRepository,
      churchId = churchId,
      pastorName = pastorName,
      onDismiss = { showPublishDialog = false }
    )
  }

  if (deletingVerse != null) {
    val target = deletingVerse!!
    AlertDialog(
      onDismissRequest = { deletingVerse = null },
      containerColor = SurfaceWhite,
      title = {
        Text(
          text = "Delete Bible Verse?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDeepNavy)
        )
      },
      text = {
        Text(
          text = "Remove \"${target.bookReference}\" from your church's published verse collection?",
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = bibleContentRepository.deleteVerse(target.verseId)
              deletingVerse = null
              res.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to delete verse", Toast.LENGTH_LONG).show()
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
          onClick = { deletingVerse = null },
          border = BorderStroke(1.dp, SoftBlueBorder),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
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
            text = stringResource(id = R.string.daily_verse_manager_title),
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = "${customVerses.size} Pastor verses • ${BibleContentRepository.builtInDailyVerseCatalog.size} built-in bilingual verses",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = { showPublishDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("pastor_publish_verse_button")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Verse", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Auto Verse Daily Control Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
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
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ClassicChurchBlue,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.auto_verse_daily_title),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
            Switch(
              checked = dailyConfig.autoDailyEnabled,
              enabled = !isTogglingAuto,
              onCheckedChange = { enabled ->
                isTogglingAuto = true
                coroutineScope.launch {
                  val res = bibleContentRepository.setAutoDailyVerseEnabled(
                    enabled = enabled,
                    pastorName = pastorName,
                    churchId = churchId
                  )
                  isTogglingAuto = false
                  res.onSuccess {
                    val msg = if (enabled) {
                      "Auto Verse Daily enabled for all members"
                    } else {
                      "Auto Verse Daily paused — showing your latest published verse"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                  }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Failed to update setting", Toast.LENGTH_LONG).show()
                  }
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceWhite,
                checkedTrackColor = ClassicChurchBlue
              ),
              modifier = Modifier.testTag("pastor_auto_verse_switch")
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(id = R.string.auto_verse_daily_desc),
            style = MaterialTheme.typography.bodySmall.copy(color = TextDeepNavy, lineHeight = 19.sp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (dailyConfig.autoDailyEnabled) SuccessGreenContainer else SurfaceWhite
          ) {
            Text(
              text = if (dailyConfig.autoDailyEnabled) {
                "AUTO ROTATION ACTIVE • ${resolution.rotationPool.size} Verses in Daily Cycle"
              } else {
                "MANUAL MODE • Believers see Pastor's latest verse until changed"
              },
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (dailyConfig.autoDailyEnabled) SuccessGreen else ClassicChurchBlue
              ),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }
    }

    // Today's Live Verse Preview (what all members currently see)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pastor_today_verse_preview_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Live Today Across App",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = ClassicChurchBlue
                )
              )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
              val sourceLabel = when (resolution.source) {
                DailyVerseSource.PASTOR_PINNED_TODAY -> "Pinned Today"
                DailyVerseSource.PASTOR_CUSTOM -> "Pastor Verse"
                DailyVerseSource.AUTO_DAILY -> "Auto Daily"
              }
              Text(
                text = "${resolution.verse.bookReference} • $sourceLabel",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (isTamil) resolution.verse.textTamil else resolution.verse.textEnglish,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontStyle = FontStyle.Italic,
              color = TextDeepNavy,
              lineHeight = 22.sp
            )
          )
          val secondaryText = if (isTamil) resolution.verse.textEnglish else resolution.verse.textTamil
          if (secondaryText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = secondaryText,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 19.sp)
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Pastor Published Verses (${customVerses.size})",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy
        )
      )
    }

    if (customVerses.isEmpty()) {
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
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No custom verses added yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Tap Add Verse to share a specific scripture for today or add it to your church's daily rotation.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(customVerses, key = { it.verseId }) { verse ->
      Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
                Text(
                  text = verse.bookReference,
                  style = MaterialTheme.typography.labelMedium.copy(
                    color = ClassicChurchBlue,
                    fontWeight = FontWeight.Bold
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
              if (verse.pinDateKey == BibleContentRepository.todayDateKey()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = SuccessGreenContainer) {
                  Text(
                    text = "PINNED TODAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = SuccessGreen,
                      fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }
            }
            IconButton(
              onClick = { deletingVerse = verse },
              modifier = Modifier
                .size(48.dp)
                .testTag("delete_verse_${verse.verseId}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Bible Verse",
                tint = ErrorRed,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = verse.textEnglish,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextDeepNavy)
          )
          if (verse.textTamil.isNotBlank() && verse.textTamil != verse.textEnglish) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = verse.textTamil,
              style = MaterialTheme.typography.bodySmall.copy(color = ClassicChurchBlue)
            )
          }
          val dev = if (isTamil) verse.devotionalTamil.ifBlank { verse.devotionalEnglish } else verse.devotionalEnglish
          if (dev.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SoftBlueBorder)
            Spacer(modifier = Modifier.height(6.dp))
            Text(dev, style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
          }
        }
      }
    }
  }
}

@Composable
fun PublishDailyVerseDialog(
  bibleContentRepository: BibleContentRepository,
  churchId: String,
  pastorName: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var reference by remember { mutableStateOf("") }
  var textEn by remember { mutableStateOf("") }
  var textTa by remember { mutableStateOf("") }
  var devEn by remember { mutableStateOf("") }
  var devTa by remember { mutableStateOf("") }
  var pinForToday by remember { mutableStateOf(true) }
  var isSubmitting by remember { mutableStateOf(false) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
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

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite,
    title = {
      Text(
        text = "Create Daily Bible Verse",
        style = MaterialTheme.typography.titleMedium.copy(
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
        OutlinedTextField(
          value = reference,
          onValueChange = { reference = it },
          label = { Text("Book & Verse (e.g. Psalm 23:1)", color = TextSlate) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verse_reference_input"),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = textEn,
          onValueChange = { textEn = it },
          label = { Text("Scripture Text (English)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verse_text_en_input"),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = textTa,
          onValueChange = { textTa = it },
          label = { Text("Scripture Text (தமிழ் - Optional)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verse_text_ta_input"),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = devEn,
          onValueChange = { devEn = it },
          label = { Text("Pastoral Reflection (English - Optional)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        OutlinedTextField(
          value = devTa,
          onValueChange = { devTa = it },
          label = { Text("Pastoral Reflection (தமிழ் - Optional)", color = TextSlate) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = fieldColors
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = pinForToday,
            onCheckedChange = { pinForToday = it },
            colors = CheckboxDefaults.colors(checkedColor = ClassicChurchBlue)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pin as Today's Featured Verse",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            Text(
              text = "Overrides Auto Daily for today only; tomorrow auto-rotation continues.",
              style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          isSubmitting = true
          coroutineScope.launch {
            val res = bibleContentRepository.publishVerse(
              reference = reference,
              textEn = textEn,
              textTa = textTa,
              devEn = devEn,
              devTa = devTa,
              pastorName = pastorName,
              pinForToday = pinForToday,
              churchId = churchId
            )
            isSubmitting = false
            res.onSuccess {
              Toast.makeText(context, "Daily Bible verse published to congregation", Toast.LENGTH_SHORT).show()
              onDismiss()
            }.onFailure { err ->
              Toast.makeText(context, err.localizedMessage ?: "Failed to publish verse", Toast.LENGTH_LONG).show()
            }
          }
        },
        enabled = !isSubmitting && reference.isNotBlank() && (textEn.isNotBlank() || textTa.isNotBlank()),
        colors = ButtonDefaults.buttonColors(containerColor = ClassicChurchBlue, contentColor = TextWhite),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("publish_verse_confirm_button")
      ) {
        Text(if (isSubmitting) "Publishing..." else "Publish", color = TextWhite, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SoftBlueBorder),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancel", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
