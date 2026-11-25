package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ChurchColorPalette(
  val isDark: Boolean,
  val primary: Color,
  val vibrantBlue: Color,
  val lightSkyBlue: Color,
  val primaryContainer: Color,
  val border: Color,
  val background: Color,
  val surface: Color,
  val surfaceSubtle: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
  val accentAmber: Color,
  val amberContainer: Color,
  val accentGold: Color,
  val successGreen: Color,
  val successContainer: Color,
  val errorRed: Color,
  val errorContainer: Color
)

// Refined High-Contrast Light Sanctuary Palette
val LightChurchPalette = ChurchColorPalette(
  isDark = false,
  primary = Color(0xFF154582),
  vibrantBlue = Color(0xFF1E6FD9),
  lightSkyBlue = Color(0xFF42A5F5),
  primaryContainer = Color(0xFFE7F1FD),
  border = Color(0xFFC8DCF5),
  background = Color(0xFFF4F7FC),
  surface = Color(0xFFFFFFFF),
  surfaceSubtle = Color(0xFFECF3FC),
  textPrimary = Color(0xFF0B1A30),
  textSecondary = Color(0xFF394B65),
  textMuted = Color(0xFF5E718B),
  accentAmber = Color(0xFFC96B05),
  amberContainer = Color(0xFFFEF3D6),
  accentGold = Color(0xFFEAB308),
  successGreen = Color(0xFF15803D),
  successContainer = Color(0xFFDCFCE7),
  errorRed = Color(0xFFB91C1C),
  errorContainer = Color(0xFFFEE2E2)
)

// Deep Midnight Sanctuary Dark Palette (WCAG AA/AAA verified pairings)
val DarkChurchPalette = ChurchColorPalette(
  isDark = true,
  primary = Color(0xFF387DF0),
  vibrantBlue = Color(0xFF60A5FA),
  lightSkyBlue = Color(0xFF93C5FD),
  primaryContainer = Color(0xFF172B47),
  border = Color(0xFF284266),
  background = Color(0xFF08111E),
  surface = Color(0xFF111E32),
  surfaceSubtle = Color(0xFF16263E),
  textPrimary = Color(0xFFF1F5FB),
  textSecondary = Color(0xFFB8C7DE),
  textMuted = Color(0xFF879AB7),
  accentAmber = Color(0xFFF59E0B),
  amberContainer = Color(0xFF2C2212),
  accentGold = Color(0xFFFBBF24),
  successGreen = Color(0xFF1E9E52),
  successContainer = Color(0xFF0F2D1E),
  errorRed = Color(0xFFEF4444),
  errorContainer = Color(0xFF34161A)
)

val LocalChurchPalette = compositionLocalOf { LightChurchPalette }

// Constant dark-backdrop & on-dark-surface tokens (Hero banners, Splash, Drawer header, Primary button labels)
val DeepRoyalNavy = Color(0xFF091E3A)
val DarkNavyBackground = Color(0xFF071426)
val DarkCardSurface = Color(0xFF0E2545)
val TextWhite = Color(0xFFFFFFFF)
val TextIceBlue = Color(0xFFD6E6FA)

// Reactive Theme-Aware Color Tokens consumed across all Composables
val ClassicChurchBlue: Color
  @Composable get() = LocalChurchPalette.current.primary

val VibrantBlue: Color
  @Composable get() = LocalChurchPalette.current.vibrantBlue

val LightSkyBlue: Color
  @Composable get() = LocalChurchPalette.current.lightSkyBlue

val SoftBlueContainer: Color
  @Composable get() = LocalChurchPalette.current.primaryContainer

val SoftBlueBorder: Color
  @Composable get() = LocalChurchPalette.current.border

val CleanBackground: Color
  @Composable get() = LocalChurchPalette.current.background

val SurfaceWhite: Color
  @Composable get() = LocalChurchPalette.current.surface

val SurfaceCard: Color
  @Composable get() = LocalChurchPalette.current.surface

val SurfaceSubtle: Color
  @Composable get() = LocalChurchPalette.current.surfaceSubtle

val TextDeepNavy: Color
  @Composable get() = LocalChurchPalette.current.textPrimary

val TextSlate: Color
  @Composable get() = LocalChurchPalette.current.textSecondary

val TextMuted: Color
  @Composable get() = LocalChurchPalette.current.textMuted

val AccentAmber: Color
  @Composable get() = LocalChurchPalette.current.accentAmber

val SoftAmberContainer: Color
  @Composable get() = LocalChurchPalette.current.amberContainer

val AccentGold: Color
  @Composable get() = LocalChurchPalette.current.accentGold

val SuccessGreen: Color
  @Composable get() = LocalChurchPalette.current.successGreen

val SuccessGreenContainer: Color
  @Composable get() = LocalChurchPalette.current.successContainer

val ErrorRed: Color
  @Composable get() = LocalChurchPalette.current.errorRed

val ErrorRedContainer: Color
  @Composable get() = LocalChurchPalette.current.errorContainer
