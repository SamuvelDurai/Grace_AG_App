package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.core.theme.AppThemeMode
import com.example.core.theme.ThemeManager

val LocalThemeManager = staticCompositionLocalOf<ThemeManager?> { null }

private fun buildLightColorScheme(p: ChurchColorPalette) = lightColorScheme(
  primary = p.primary,
  onPrimary = TextWhite,
  primaryContainer = p.primaryContainer,
  onPrimaryContainer = DeepRoyalNavy,
  secondary = p.vibrantBlue,
  onSecondary = TextWhite,
  secondaryContainer = p.primaryContainer,
  onSecondaryContainer = DeepRoyalNavy,
  tertiary = p.accentAmber,
  onTertiary = TextWhite,
  tertiaryContainer = p.amberContainer,
  onTertiaryContainer = p.textPrimary,
  background = p.background,
  onBackground = p.textPrimary,
  surface = p.surface,
  onSurface = p.textPrimary,
  surfaceVariant = p.surfaceSubtle,
  onSurfaceVariant = p.textSecondary,
  surfaceContainerLowest = p.surface,
  surfaceContainerLow = p.background,
  surfaceContainer = p.surface,
  surfaceContainerHigh = p.surface,
  surfaceContainerHighest = p.primaryContainer,
  outline = p.border,
  outlineVariant = Color(0xFFE2E8F0),
  error = p.errorRed,
  onError = TextWhite,
  errorContainer = p.errorContainer,
  onErrorContainer = p.errorRed
)

private fun buildDarkColorScheme(p: ChurchColorPalette) = darkColorScheme(
  primary = p.primary,
  onPrimary = TextWhite,
  primaryContainer = p.primaryContainer,
  onPrimaryContainer = p.textPrimary,
  secondary = p.vibrantBlue,
  onSecondary = DeepRoyalNavy,
  secondaryContainer = p.primaryContainer,
  onSecondaryContainer = p.textPrimary,
  tertiary = p.accentAmber,
  onTertiary = DeepRoyalNavy,
  tertiaryContainer = p.amberContainer,
  onTertiaryContainer = p.textPrimary,
  background = p.background,
  onBackground = p.textPrimary,
  surface = p.surface,
  onSurface = p.textPrimary,
  surfaceVariant = p.surfaceSubtle,
  onSurfaceVariant = p.textSecondary,
  surfaceContainerLowest = p.background,
  surfaceContainerLow = Color(0xFF0C1728),
  surfaceContainer = p.surface,
  surfaceContainerHigh = p.surfaceSubtle,
  surfaceContainerHighest = p.primaryContainer,
  outline = p.border,
  outlineVariant = Color(0xFF1F3352),
  error = p.errorRed,
  onError = TextWhite,
  errorContainer = p.errorContainer,
  onErrorContainer = p.errorRed
)

@Composable
fun ChurchConnectTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }

  val target = if (isDark) DarkChurchPalette else LightChurchPalette
  val spec = tween<Color>(durationMillis = 260)

  val background by animateColorAsState(target.background, spec, label = "theme_bg")
  val surface by animateColorAsState(target.surface, spec, label = "theme_surface")
  val surfaceSubtle by animateColorAsState(target.surfaceSubtle, spec, label = "theme_surface_subtle")
  val primary by animateColorAsState(target.primary, spec, label = "theme_primary")
  val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "theme_primary_container")
  val border by animateColorAsState(target.border, spec, label = "theme_border")
  val textPrimary by animateColorAsState(target.textPrimary, spec, label = "theme_text_primary")
  val textSecondary by animateColorAsState(target.textSecondary, spec, label = "theme_text_secondary")
  val textMuted by animateColorAsState(target.textMuted, spec, label = "theme_text_muted")

  val animatedPalette = target.copy(
    background = background,
    surface = surface,
    surfaceSubtle = surfaceSubtle,
    primary = primary,
    primaryContainer = primaryContainer,
    border = border,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textMuted = textMuted
  )

  val colorScheme = if (isDark) {
    buildDarkColorScheme(animatedPalette)
  } else {
    buildLightColorScheme(animatedPalette)
  }

  CompositionLocalProvider(LocalChurchPalette provides animatedPalette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
