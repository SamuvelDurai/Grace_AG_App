package com.example.core.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val prefValue: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromPref(value: String?): AppThemeMode {
      return entries.firstOrNull { it.prefValue == value } ?: SYSTEM
    }
  }
}

class ThemeManager private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _themeMode = MutableStateFlow(
    AppThemeMode.fromPref(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.prefValue))
  )
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  fun setThemeMode(mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.prefValue).apply()
    _themeMode.value = mode
  }

  companion object {
    private const val PREFS_NAME = "church_connect_theme"
    private const val KEY_THEME_MODE = "selected_theme_mode"

    @Volatile
    private var instance: ThemeManager? = null

    fun getInstance(context: Context): ThemeManager {
      return instance ?: synchronized(this) {
        instance ?: ThemeManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
