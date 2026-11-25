package com.example.presentation.auth

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.core.localization.LanguageManager
import com.example.presentation.common.AnimatedEntranceItem
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun OnboardingScreen(
  currentLanguage: String,
  onLanguageSelected: (String) -> Unit,
  onGetStarted: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("onboarding_screen"),
    color = CleanBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Hero Sanctuary Welcome Card
      AnimatedEntranceItem(index = 0) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = DeepRoyalNavy),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(250.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_sanctuary_hero),
              contentDescription = stringResource(id = R.string.app_name),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colorStops = arrayOf(
                      0.0f to DeepRoyalNavy.copy(alpha = 0.25f),
                      0.55f to DeepRoyalNavy.copy(alpha = 0.68f),
                      1.0f to DeepRoyalNavy.copy(alpha = 0.94f)
                    )
                  )
                )
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(92.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                  contentAlignment = Alignment.Center
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.ic_church_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                      .size(66.dp)
                      .clip(CircleShape)
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = stringResource(id = R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite,
                  fontSize = 23.sp
                ),
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = stringResource(id = R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextIceBlue,
                  lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Language Selection Card
      AnimatedEntranceItem(index = 1) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(SoftBlueContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  tint = ClassicChurchBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(id = R.string.select_language),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              LanguageChoiceChip(
                title = "English",
                selected = currentLanguage == LanguageManager.LANG_ENGLISH,
                onClick = { onLanguageSelected(LanguageManager.LANG_ENGLISH) },
                modifier = Modifier.weight(1f)
              )

              LanguageChoiceChip(
                title = "தமிழ் (Tamil)",
                selected = currentLanguage == LanguageManager.LANG_TAMIL,
                onClick = { onLanguageSelected(LanguageManager.LANG_TAMIL) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Button
      AnimatedEntranceItem(index = 2) {
        Button(
          onClick = onGetStarted,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("get_started_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ClassicChurchBlue,
            contentColor = TextWhite
          ),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
          Text(
            text = stringResource(id = R.string.get_started),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextWhite,
              letterSpacing = 0.5.sp
            )
          )
        }
      }
    }
  }
}

@Composable
private fun LanguageChoiceChip(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (selected) {
    Button(
      onClick = onClick,
      modifier = modifier.height(50.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = ClassicChurchBlue,
        contentColor = TextWhite
      ),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = TextWhite,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextWhite
          )
        )
      }
    }
  } else {
    OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(50.dp),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, SoftBlueBorder),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = SurfaceWhite,
        contentColor = TextDeepNavy
      )
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.SemiBold,
          color = TextDeepNavy
        )
      )
    }
  }
}
