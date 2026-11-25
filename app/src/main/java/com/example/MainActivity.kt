package com.example

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.localization.LanguageManager
import com.example.core.theme.AppThemeMode
import com.example.core.theme.ThemeManager
import com.example.data.admin.ChurchAdminRepository
import com.example.data.announcement.AnnouncementRepository
import com.example.data.auth.AuthRepository
import com.example.data.content.BibleContentRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.finance.FinanceRepository
import com.example.data.member.MemberRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.example.data.user.UserRepository
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.navigation.AppNavigation
import com.example.ui.theme.ChurchConnectTheme
import com.example.ui.theme.LocalThemeManager

class MainActivity : ComponentActivity() {

  private val languageManager by lazy { LanguageManager.getInstance(applicationContext) }
  private val themeManager by lazy { ThemeManager.getInstance(applicationContext) }
  private val sessionManager by lazy { com.example.data.auth.SessionManager(applicationContext) }
  private val authRepository by lazy { AuthRepository() }
  private val userRepository by lazy { UserRepository(applicationContext) }
  private val memberRepository by lazy { MemberRepository(applicationContext) }
  private val bibleContentRepository by lazy { BibleContentRepository(applicationContext) }
  private val announcementRepository by lazy { AnnouncementRepository(applicationContext) }
  private val prayerRepository by lazy { PrayerRepository(applicationContext) }
  private val financeRepository by lazy { FinanceRepository(applicationContext) }
  private val churchEventRepository by lazy { ChurchEventRepository(applicationContext) }
  private val sermonRepository by lazy { SermonRepository(applicationContext) }
  private val fellowshipRepository by lazy { com.example.data.fellowship.FellowshipRepository(applicationContext) }
  private val pastoralCareRepository by lazy { com.example.data.pastoralcare.PastoralCareRepository(applicationContext) }
  private val churchAdminRepository by lazy { ChurchAdminRepository(applicationContext) }

  private val authViewModel: AuthViewModel by viewModels {
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(authRepository, userRepository, memberRepository, sessionManager) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val currentLang by languageManager.currentLanguage.collectAsState()
      val currentThemeMode by themeManager.themeMode.collectAsState()
      val isDarkTheme = when (currentThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      SideEffect {
        enableEdgeToEdge(
          statusBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
          } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
          },
          navigationBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
          } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
          }
        )
      }

      val localizedContext = remember(currentLang) {
        languageManager.applyLocale(this@MainActivity)
      }
      val activityLocalizedContext = remember(localizedContext) {
        object : ContextWrapper(this@MainActivity) {
          override fun getResources(): Resources = localizedContext.resources
          override fun getAssets(): AssetManager = localizedContext.assets
          override fun getTheme(): Resources.Theme = localizedContext.theme
        }
      }

      CompositionLocalProvider(
        LocalContext provides activityLocalizedContext,
        LocalThemeManager provides themeManager,
        LocalActivityResultRegistryOwner provides this@MainActivity,
        LocalOnBackPressedDispatcherOwner provides this@MainActivity
      ) {
        ChurchConnectTheme(themeMode = currentThemeMode) {
          AppNavigation(
            viewModel = authViewModel,
            memberRepository = memberRepository,
            bibleContentRepository = bibleContentRepository,
            announcementRepository = announcementRepository,
            prayerRepository = prayerRepository,
            financeRepository = financeRepository,
            churchEventRepository = churchEventRepository,
            sermonRepository = sermonRepository,
            fellowshipRepository = fellowshipRepository,
            pastoralCareRepository = pastoralCareRepository,
            churchAdminRepository = churchAdminRepository,
            languageManager = languageManager,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
