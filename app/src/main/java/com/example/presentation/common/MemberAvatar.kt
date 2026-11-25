package com.example.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.ProfilePhotoHelper
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun MemberAvatar(
  photoUrl: String,
  displayName: String,
  size: Dp = 46.dp,
  showBorder: Boolean = true,
  backgroundColor: Color = SoftBlueContainer,
  contentColor: Color = ClassicChurchBlue,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val cleanUrl = remember(photoUrl) { photoUrl.trim() }
  val decodedBitmap = remember(cleanUrl) {
    ProfilePhotoHelper.decodeDataUriBitmap(cleanUrl)
  }
  val isRemoteHttpUrl = remember(cleanUrl) {
    cleanUrl.startsWith("https://", ignoreCase = true) ||
      cleanUrl.startsWith("http://", ignoreCase = true)
  }
  var remoteFailed by remember(cleanUrl) { mutableStateOf(false) }

  val baseModifier = modifier
    .size(size)
    .clip(CircleShape)
    .background(backgroundColor)
    .let { mod ->
      if (showBorder) mod.border(BorderStroke(1.5.dp, SoftBlueBorder), CircleShape) else mod
    }
    .let { mod ->
      if (onClick != null) mod.clickable(onClick = onClick) else mod
    }

  Box(
    modifier = baseModifier,
    contentAlignment = Alignment.Center
  ) {
    // Always render fallback initials/icon underneath so a failed/loading remote image never shows an empty circle
    if (displayName.isNotBlank()) {
      val initials = remember(displayName) {
        displayName.trim()
          .split(Regex("\\s+"))
          .take(2)
          .mapNotNull { it.firstOrNull()?.uppercaseChar() }
          .joinToString("")
          .ifBlank { displayName.trim().take(1).uppercase() }
      }
      Text(
        text = initials,
        color = contentColor,
        fontWeight = FontWeight.Bold,
        fontSize = (size.value * 0.36f).coerceIn(11f, 34f).sp
      )
    } else {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Member avatar",
        tint = contentColor,
        modifier = Modifier.size(size * 0.55f)
      )
    }

    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = displayName.ifBlank { "Member photo" },
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
        )
      }
      isRemoteHttpUrl && !remoteFailed -> {
        val context = LocalContext.current
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(cleanUrl)
            .crossfade(true)
            .build(),
          contentDescription = displayName.ifBlank { "Member photo" },
          contentScale = ContentScale.Crop,
          onError = { remoteFailed = true },
          modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
        )
      }
    }
  }
}

@Composable
fun MemberPhotoPreviewDialog(
  photoUrl: String,
  displayName: String,
  subtitle: String = "",
  isUploading: Boolean = false,
  onChangePhoto: (() -> Unit)? = null,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("member_photo_preview_dialog"),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      border = BorderStroke(1.dp, SoftBlueBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = displayName.ifBlank { "Church Member" },
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDeepNavy
              )
            )
            if (subtitle.isNotBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = ClassicChurchBlue)
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close photo preview",
              tint = TextSlate
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(contentAlignment = Alignment.Center) {
          MemberAvatar(
            photoUrl = photoUrl,
            displayName = displayName,
            size = 210.dp,
            showBorder = true
          )
          if (isUploading) {
            CircularProgressIndicator(
              color = ClassicChurchBlue,
              strokeWidth = 4.dp,
              modifier = Modifier.size(210.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!ProfilePhotoHelper.isCrossDevicePhotoUrl(photoUrl)) {
          Text(
            text = "No profile photo uploaded yet.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSlate),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        if (onChangePhoto != null) {
          Button(
            onClick = onChangePhoto,
            enabled = !isUploading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("preview_upload_photo_button")
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = null,
              tint = TextWhite,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isUploading) "Saving Photo..." else if (ProfilePhotoHelper.isCrossDevicePhotoUrl(photoUrl)) "Update Member Photo" else "Upload Member Photo",
              color = TextWhite,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
