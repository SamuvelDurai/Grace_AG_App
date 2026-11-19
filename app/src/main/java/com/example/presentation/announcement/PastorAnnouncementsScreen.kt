package com.example.presentation.announcement

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Announcement
import com.example.data.announcement.AnnouncementRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PastorAnnouncementsScreen(
  announcementRepository: AnnouncementRepository,
  pastorName: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val announcements by announcementRepository.observeAnnouncements().collectAsState(initial = emptyList())
  var showPublishDialog by remember { mutableStateOf(false) }
  var deletingNotice by remember { mutableStateOf<Announcement?>(null) }

  if (showPublishDialog) {
    PublishAnnouncementDialog(
      announcementRepository = announcementRepository,
      pastorName = pastorName,
      onDismiss = { showPublishDialog = false }
    )
  }

  if (deletingNotice != null) {
    val target = deletingNotice!!
    AlertDialog(
      onDismissRequest = { deletingNotice = null },
      containerColor = SurfaceWhite,
      title = {
        Text(
          text = "Delete Church Notice?",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextDeepNavy
          )
        )
      },
      text = {
        Text(
          text = "Remove \"${target.titleEnglish}\" from believer feeds?",
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = announcementRepository.deleteAnnouncement(target.announcementId)
              deletingNotice = null
              res.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to delete notice", Toast.LENGTH_LONG).show()
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
          onClick = { deletingNotice = null },
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
            text = "Church Notices & Announcements",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = "${announcements.size} published notices",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = { showPublishDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("publish_announcement_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = TextWhite,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Publish", color = TextWhite, fontWeight = FontWeight.Bold)
        }
      }
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
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No announcements published yet", fontWeight = FontWeight.Bold, color = TextDeepNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Tap Publish to send an English & Tamil notice to your congregation.",
              color = TextSlate,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    items(announcements, key = { it.announcementId }) { ann ->
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
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SoftBlueContainer
            ) {
              Text(
                text = ann.category.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(ann.publishDate, style = MaterialTheme.typography.bodySmall.copy(color = TextSlate))
              IconButton(
                onClick = { deletingNotice = ann },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("delete_announcement_${ann.announcementId}")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete Notice",
                  tint = ErrorRed,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = ann.titleEnglish,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          if (ann.titleTamil.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = ann.titleTamil,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ClassicChurchBlue
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = ann.bodyEnglish,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSlate, lineHeight = 20.sp)
          )
          if (ann.bodyTamil.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = ann.bodyTamil,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSlate, lineHeight = 18.sp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Posted by: ${ann.authorName}",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSlate, fontWeight = FontWeight.Medium)
          )
        }
      }
    }
  }
}
