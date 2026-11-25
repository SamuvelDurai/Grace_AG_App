package com.example.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onAnimationFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scale = remember { Animatable(0.75f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    scale.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )
    alpha.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 500)
    )
    delay(900)
    onAnimationFinished()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DeepRoyalNavy)
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_sanctuary_hero),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .scale(1.06f - (0.04f * scale.value))
        .alpha(0.32f)
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              DeepRoyalNavy.copy(alpha = 0.72f),
              ClassicChurchBlue.copy(alpha = 0.80f),
              DeepRoyalNavy.copy(alpha = 0.94f)
            )
          )
        )
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.scale(scale.value)
    ) {
      Box(
        modifier = Modifier
          .size(118.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(98.dp)
            .clip(CircleShape)
            .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_church_logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
              .size(86.dp)
              .clip(CircleShape)
          )
        }
      }

      Spacer(modifier = Modifier.height(26.dp))

      Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TextWhite,
          letterSpacing = 1.4.sp
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(id = R.string.app_tagline),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextIceBlue,
          fontWeight = FontWeight.Medium
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(0.35f + (0.65f * alpha.value))
      )
    }
  }
}
