package com.example.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

/**
 * Reusable spring press scale for cards and interactive tiles.
 * Pair with `MutableInteractionSource` passed into clickable / Card(onClick).
 */
@Composable
fun rememberTactilePressScale(
  interactionSource: MutableInteractionSource,
  pressedScale: Float = 0.965f
): Float {
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "tactile_press_scale"
  )
  return scale
}

/**
 * Smooth, test-safe staggered entrance reveal for cards and sections.
 * Keeps content mounted in the semantics tree while gliding gently into place.
 */
@Composable
fun AnimatedEntranceItem(
  index: Int,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val progress = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    val staggerMs = (index.coerceIn(0, 6) * 45).toLong()
    if (staggerMs > 0L) delay(staggerMs)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
      )
    )
  }

  val p = progress.value
  Box(
    modifier = modifier.graphicsLayer {
      alpha = 0.35f + (0.65f * p)
      translationY = (1f - p) * 22f
      val s = 0.985f + (0.015f * p)
      scaleX = s
      scaleY = s
    }
  ) {
    content()
  }
}

/**
 * Smooth Material 3 crossfade + subtle vertical motion for dashboard tab transitions.
 */
@Composable
fun <T> SmoothTabContent(
  targetState: T,
  modifier: Modifier = Modifier,
  label: String = "smooth_tab_content",
  content: @Composable (T) -> Unit
) {
  AnimatedContent(
    targetState = targetState,
    modifier = modifier,
    transitionSpec = {
      (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
        slideInVertically(
          animationSpec = tween(240, easing = FastOutSlowInEasing),
          initialOffsetY = { fullHeight -> (fullHeight / 32).coerceIn(10, 28) }
        )) togetherWith fadeOut(animationSpec = tween(140)) using SizeTransform(clip = false)
    },
    label = label
  ) { state ->
    content(state)
  }
}

/**
 * Full-bleed church hero banner with protective royal-navy scrim, gentle zoom settle,
 * optional frosted badge pill, and optional trailing content (e.g. avatar or stat pill).
 *
 * Replace the corresponding `res/drawable/img_*.jpg` file anytime to update page artwork.
 */
@Composable
fun ChurchPageHeroBanner(
  @DrawableRes imageRes: Int,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  badgeText: String? = null,
  badgeIcon: ImageVector? = null,
  height: Dp = 156.dp,
  contentDescription: String? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  bottomContent: (@Composable () -> Unit)? = null
) {
  val imageScale = remember { Animatable(1.06f) }
  LaunchedEffect(imageRes) {
    imageScale.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
    )
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = DeepRoyalNavy),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
    ) {
      Image(
        painter = painterResource(id = imageRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = imageScale.value
            scaleY = imageScale.value
          }
      )

      // Multi-stop protective gradient scrim for legible white text over any photo
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0.0f to DeepRoyalNavy.copy(alpha = 0.30f),
                0.45f to DeepRoyalNavy.copy(alpha = 0.68f),
                1.0f to DeepRoyalNavy.copy(alpha = 0.93f)
              )
            )
          )
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        if (!badgeText.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (badgeIcon != null) {
                Icon(
                  imageVector = badgeIcon,
                  contentDescription = null,
                  tint = AccentAmber,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
              }
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextWhite,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.4.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        } else {
          Spacer(modifier = Modifier.height(2.dp))
        }

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite,
                  lineHeight = 26.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = subtitle,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TextIceBlue,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                  ),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            if (trailingContent != null) {
              Spacer(modifier = Modifier.width(12.dp))
              trailingContent()
            }
          }
          if (bottomContent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            bottomContent()
          }
        }
      }
    }
  }
}
