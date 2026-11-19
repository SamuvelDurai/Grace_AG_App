package com.example.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.util.ProfilePhotoHelper
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer

@Composable
fun MemberAvatar(
  photoUrl: String,
  displayName: String,
  size: Dp = 46.dp,
  showBorder: Boolean = true,
  backgroundColor: Color = SoftBlueContainer,
  contentColor: Color = ClassicChurchBlue,
  modifier: Modifier = Modifier
) {
  val decodedBitmap = remember(photoUrl) {
    ProfilePhotoHelper.decodeDataUriBitmap(photoUrl)
  }

  val baseModifier = modifier
    .size(size)
    .clip(CircleShape)
    .background(backgroundColor)
    .let { mod ->
      if (showBorder) mod.border(BorderStroke(1.5.dp, SoftBlueBorder), CircleShape) else mod
    }

  Box(
    modifier = baseModifier,
    contentAlignment = Alignment.Center
  ) {
    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = displayName.ifBlank { "Member photo" },
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
      }
      photoUrl.isNotBlank() -> {
        AsyncImage(
          model = photoUrl,
          contentDescription = displayName.ifBlank { "Member photo" },
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
      }
      displayName.isNotBlank() -> {
        val initials = displayName.trim()
          .split(Regex("\\s+"))
          .take(2)
          .mapNotNull { it.firstOrNull()?.uppercaseChar() }
          .joinToString("")
          .ifBlank { displayName.trim().take(1).uppercase() }
        Text(
          text = initials,
          color = contentColor,
          fontWeight = FontWeight.Bold,
          fontSize = (size.value * 0.36f).coerceIn(11f, 28f).sp
        )
      }
      else -> {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = "Member avatar",
          tint = contentColor,
          modifier = Modifier.size(size * 0.55f)
        )
      }
    }
  }
}
