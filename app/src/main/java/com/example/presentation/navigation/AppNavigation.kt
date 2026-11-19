package com.example.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.core.localization.LanguageManager
import com.example.core.model.AccountStatus
import com.example.core.model.UserRole
import com.example.core.notifications.NotificationSyncObserver
import com.example.presentation.common.AppUpdateChecker
import com.example.data.admin.ChurchAdminRepository
import com.example.data.announcement.AnnouncementRepository
import com.example.data.content.BibleContentRepository
import com.example.data.event.ChurchEventRepository
import com.example.data.fellowship.FellowshipRepository
import com.example.data.finance.FinanceRepository
import com.example.data.member.MemberRepository
import com.example.data.pastoralcare.PastoralCareRepository
import com.example.data.prayer.PrayerRepository
import com.example.data.sermon.SermonRepository
import com.example.presentation.auth.AuthUiState
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.auth.NewBelieverRegistrationScreen
import com.example.presentation.auth.OnboardingScreen
import com.example.presentation.auth.OtpVerificationScreen
import com.example.presentation.auth.PhoneLoginScreen
import com.example.presentation.auth.SplashScreen
import com.example.presentation.dashboard.BelieverDashboardScreen
import com.example.presentation.dashboard.PastorDashboardScreen
import com.example.presentation.dashboard.SuperAdminDashboardScreen
import com.example.presentation.status.AccountSuspendedScreen
import com.example.presentation.status.PendingApprovalScreen
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground

enum class ScreenState {
  SPLASH,
  ONBOARDING,
  PHONE_LOGIN,
  OTP_VERIFY,
  REGISTRATION,
  MAIN_DASHBOARD
}

@Composable
fun AppNavigation(
  viewModel: AuthViewModel,
  memberRepository: MemberRepository,
  bibleContentRepository: BibleContentRepository,
  announcementRepository: AnnouncementRepository,
  prayerRepository: PrayerRepository,
  financeRepository: FinanceRepository,
  churchEventRepository: ChurchEventRepository,
  sermonRepository: SermonRepository,
  fellowshipRepository: FellowshipRepository,
  pastoralCareRepository: PastoralCareRepository,
  churchAdminRepository: ChurchAdminRepository,
  languageManager: LanguageManager,
  modifier: Modifier = Modifier
) {
  val authState by viewModel.uiState.collectAsState()
  val currentLanguage by languageManager.currentLanguage.collectAsState()
  var screenState by remember { mutableStateOf(ScreenState.SPLASH) }
  var splashCompleted by remember { mutableStateOf(false) }

  // React cleanly to persistent auth restoration, login/signup completion, and explicit Sign Out
  LaunchedEffect(splashCompleted, authState, screenState) {
    when {
      screenState == ScreenState.SPLASH && splashCompleted -> {
        screenState = when (authState) {
          is AuthUiState.Authenticated -> {
            val profile = (authState as AuthUiState.Authenticated).profile
            if (profile.userRole == UserRole.BELIEVER && !profile.profileCompleted) {
              ScreenState.REGISTRATION
            } else {
              ScreenState.MAIN_DASHBOARD
            }
          }
          is AuthUiState.Unauthenticated, is AuthUiState.Error -> ScreenState.ONBOARDING
          else -> screenState
        }
      }
      (screenState == ScreenState.ONBOARDING ||
        screenState == ScreenState.PHONE_LOGIN ||
        screenState == ScreenState.OTP_VERIFY) &&
        authState is AuthUiState.Authenticated -> {
        val profile = (authState as AuthUiState.Authenticated).profile
        screenState = if (profile.userRole == UserRole.BELIEVER && !profile.profileCompleted) {
          ScreenState.REGISTRATION
        } else {
          ScreenState.MAIN_DASHBOARD
        }
      }
      screenState == ScreenState.MAIN_DASHBOARD &&
        authState is AuthUiState.Authenticated -> {
        val profile = (authState as AuthUiState.Authenticated).profile
        if (profile.userRole == UserRole.BELIEVER && !profile.profileCompleted) {
          screenState = ScreenState.REGISTRATION
        }
      }
      (screenState == ScreenState.MAIN_DASHBOARD || screenState == ScreenState.REGISTRATION) &&
        authState is AuthUiState.Unauthenticated -> {
        screenState = ScreenState.PHONE_LOGIN
      }
    }
  }

  when (screenState) {
    ScreenState.SPLASH -> {
      SplashScreen(
        onAnimationFinished = {
          splashCompleted = true
        }
      )
    }

    ScreenState.ONBOARDING -> {
      OnboardingScreen(
        currentLanguage = currentLanguage,
        onLanguageSelected = { languageManager.setLanguage(it) },
        onGetStarted = { screenState = ScreenState.PHONE_LOGIN }
      )
    }

    ScreenState.PHONE_LOGIN -> {
      PhoneLoginScreen(
        viewModel = viewModel,
        onNavigateToOtp = { screenState = ScreenState.OTP_VERIFY }
      )
    }

    ScreenState.OTP_VERIFY -> {
      BackHandler { screenState = ScreenState.PHONE_LOGIN }
      OtpVerificationScreen(
        viewModel = viewModel,
        onVerificationSuccess = {
          val current = (viewModel.uiState.value as? AuthUiState.Authenticated)?.profile
          if (current != null && current.userRole == UserRole.BELIEVER && !current.profileCompleted) {
            screenState = ScreenState.REGISTRATION
          } else if (current != null) {
            screenState = ScreenState.MAIN_DASHBOARD
          }
        },
        onNavigateBack = { screenState = ScreenState.PHONE_LOGIN }
      )
    }

    ScreenState.REGISTRATION -> {
      BackHandler(enabled = true) {
        val current = (viewModel.uiState.value as? AuthUiState.Authenticated)?.profile
        if (current != null && current.profileCompleted) {
          screenState = ScreenState.MAIN_DASHBOARD
        }
      }
      NewBelieverRegistrationScreen(
        viewModel = viewModel,
        onRegistrationSubmitted = { screenState = ScreenState.MAIN_DASHBOARD }
      )
    }

    ScreenState.MAIN_DASHBOARD -> {
      when (val state = authState) {
        is AuthUiState.Authenticated -> {
          val profile = state.profile
          when (profile.status) {
            AccountStatus.PENDING -> {
              PendingApprovalScreen(
                userProfile = profile,
                viewModel = viewModel
              )
            }
            AccountStatus.SUSPENDED, AccountStatus.REJECTED -> {
              AccountSuspendedScreen(viewModel = viewModel)
            }
            AccountStatus.APPROVED -> {
              NotificationSyncObserver(
                userProfile = profile,
                currentLanguage = currentLanguage,
                announcementRepository = announcementRepository,
                financeRepository = financeRepository,
                bibleContentRepository = bibleContentRepository
              )
              AppUpdateChecker(
                churchAdminRepository = churchAdminRepository,
                churchId = profile.churchId.ifBlank { "church_main" }
              )
              when (profile.userRole) {
                UserRole.BELIEVER -> {
                  BelieverDashboardScreen(
                    userProfile = profile,
                    viewModel = viewModel,
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
                    currentLanguage = currentLanguage
                  )
                }
                UserRole.PASTOR -> {
                  PastorDashboardScreen(
                    userProfile = profile,
                    viewModel = viewModel,
                    memberRepository = memberRepository,
                    bibleContentRepository = bibleContentRepository,
                    announcementRepository = announcementRepository,
                    financeRepository = financeRepository,
                    prayerRepository = prayerRepository,
                    churchEventRepository = churchEventRepository,
                    sermonRepository = sermonRepository,
                    fellowshipRepository = fellowshipRepository,
                    pastoralCareRepository = pastoralCareRepository,
                    churchAdminRepository = churchAdminRepository,
                    languageManager = languageManager,
                    currentLanguage = currentLanguage
                  )
                }
                UserRole.SUPER_ADMIN -> {
                  SuperAdminDashboardScreen(
                    userProfile = profile,
                    viewModel = viewModel,
                    churchAdminRepository = churchAdminRepository,
                    memberRepository = memberRepository,
                    fellowshipRepository = fellowshipRepository,
                    churchEventRepository = churchEventRepository,
                    languageManager = languageManager,
                    currentLanguage = currentLanguage
                  )
                }
              }
            }
          }
        }

        is AuthUiState.Loading, is AuthUiState.Initial -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(CleanBackground)
              .testTag("app_loading"),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = ClassicChurchBlue)
          }
        }

        else -> {
          PhoneLoginScreen(
            viewModel = viewModel,
            onNavigateToOtp = { screenState = ScreenState.OTP_VERIFY }
          )
        }
      }
    }
  }
}
