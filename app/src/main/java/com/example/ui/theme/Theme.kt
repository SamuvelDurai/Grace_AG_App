package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Strictly High-Contrast White & Blue Light Palette so text is always crisp and visible
private val AppColorScheme = lightColorScheme(
  primary = ClassicChurchBlue,
  onPrimary = SurfaceWhite,
  primaryContainer = SoftBlueContainer,
  onPrimaryContainer = DeepRoyalNavy,
  secondary = VibrantBlue,
  onSecondary = SurfaceWhite,
  secondaryContainer = SoftBlueContainer,
  onSecondaryContainer = DeepRoyalNavy,
  tertiary = AccentAmber,
  onTertiary = SurfaceWhite,
  tertiaryContainer = SoftAmberContainer,
  onTertiaryContainer = TextDeepNavy,
  background = CleanBackground,
  onBackground = TextDeepNavy,
  surface = SurfaceWhite,
  onSurface = TextDeepNavy,
  surfaceVariant = SurfaceSubtle,
  onSurfaceVariant = TextSlate,
  surfaceContainerLowest = SurfaceWhite,
  surfaceContainerLow = CleanBackground,
  surfaceContainer = SurfaceWhite,
  surfaceContainerHigh = SurfaceWhite,
  surfaceContainerHighest = SoftBlueContainer,
  outline = SoftBlueBorder,
  outlineVariant = Color(0xFFE2E8F0),
  error = ErrorRed,
  onError = SurfaceWhite,
  errorContainer = ErrorRedContainer,
  onErrorContainer = ErrorRed
)

@Composable
fun ChurchConnectTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = AppColorScheme,
    typography = Typography,
    content = content
  )
}
