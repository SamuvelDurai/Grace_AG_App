package com.example.core.localization

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageManager private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _currentLanguage = MutableStateFlow(
    prefs.getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
  )
  val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

  fun setLanguage(languageCode: String) {
    val code = if (languageCode == LANG_TAMIL) LANG_TAMIL else LANG_ENGLISH
    prefs.edit().putString(KEY_LANGUAGE, code).apply()
    _currentLanguage.value = code
  }

  fun applyLocale(context: Context): Context {
    val language = _currentLanguage.value
    val locale = Locale(language)
    Locale.setDefault(locale)

    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
  }

  companion object {
    const val LANG_ENGLISH = "en"
    const val LANG_TAMIL = "ta"
    private const val PREFS_NAME = "church_connect_locale"
    private const val KEY_LANGUAGE = "selected_language"

    @Volatile
    private var instance: LanguageManager? = null

    fun getInstance(context: Context): LanguageManager {
      return instance ?: synchronized(this) {
        instance ?: LanguageManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
