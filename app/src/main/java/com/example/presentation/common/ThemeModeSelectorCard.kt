package com.example.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.theme.AppThemeMode
import com.example.core.theme.ThemeManager
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.LocalThemeManager
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun ThemeModeSelectorCard(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val themeManager = LocalThemeManager.current ?: remember(context) {
    ThemeManager.getInstance(context)
  }
  val selectedMode by themeManager.themeMode.collectAsState()

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("theme_selector_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, SoftBlueBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SoftBlueContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(id = R.string.theme_settings_title),
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = stringResource(id = R.string.theme_settings_subtitle),
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSlate,
              fontSize = 11.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftBlueContainer,
        border = BorderStroke(1.dp, SoftBlueBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          ThemeModePill(
            label = stringResource(id = R.string.theme_system),
            icon = Icons.Default.BrightnessAuto,
            selected = selectedMode == AppThemeMode.SYSTEM,
            onClick = { themeManager.setThemeMode(AppThemeMode.SYSTEM) },
            modifier = Modifier
              .weight(1f)
              .testTag("theme_option_system")
          )
          ThemeModePill(
            label = stringResource(id = R.string.theme_light),
            icon = Icons.Default.LightMode,
            selected = selectedMode == AppThemeMode.LIGHT,
            onClick = { themeManager.setThemeMode(AppThemeMode.LIGHT) },
            modifier = Modifier
              .weight(1f)
              .testTag("theme_option_light")
          )
          ThemeModePill(
            label = stringResource(id = R.string.theme_dark),
            icon = Icons.Default.DarkMode,
            selected = selectedMode == AppThemeMode.DARK,
            onClick = { themeManager.setThemeMode(AppThemeMode.DARK) },
            modifier = Modifier
              .weight(1f)
              .testTag("theme_option_dark")
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeModePill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressScale = rememberTactilePressScale(interactionSource, pressedScale = 0.95f)
  val containerColor by animateColorAsState(
    targetValue = if (selected) ClassicChurchBlue else Color.Transparent,
    animationSpec = tween(220),
    label = "theme_pill_bg"
  )
  val contentColor by animateColorAsState(
    targetValue = if (selected) TextWhite else TextDeepNavy,
    animationSpec = tween(220),
    label = "theme_pill_fg"
  )

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    shadowElevation = if (selected) 2.dp else 0.dp,
    modifier = modifier
      .heightIn(min = 48.dp)
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
      }
      .clip(RoundedCornerShape(10.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
          fontSize = 11.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
