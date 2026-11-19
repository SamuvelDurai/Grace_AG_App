package com.example.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.localization.LanguageManager
import com.example.core.model.BibleVerse
import com.example.core.model.DailyVerseConfig
import com.example.core.model.UserRole
import com.example.data.content.BibleContentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object NotificationHelper {

  const val PREFS_NAME = "church_connect_prefs"
  const val PREF_DAILY_VERSE = "pref_daily_verse"
  const val PREF_NOTICES_ALERTS = "pref_notices_alerts"
  const val PREF_NOTIF_PROMPTED = "pref_notif_prompted"
  const val PREF_FCM_TOKEN = "pref_fcm_token"

  private const val PREF_NOTIFIED_KEYS = "pref_notified_keys"
  private const val PREF_CACHED_AUTO_ENABLED = "pref_cached_auto_verse_enabled"
  private const val PREF_CACHED_VERSES_JSON = "pref_cached_verses_json"

  const val CHANNEL_NOTICES = "grace_ag_notices"
  const val CHANNEL_DAILY_VERSE = "grace_ag_daily_verse"
  const val CHANNEL_ACCOUNT = "grace_ag_account"

  fun isDailyVerseEnabled(context: Context): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_DAILY_VERSE, true)
  }

  fun isNoticesEnabled(context: Context): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_NOTICES_ALERTS, true)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val noticesChannel = NotificationChannel(
      CHANNEL_NOTICES,
      context.getString(R.string.notif_channel_notices_name),
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = context.getString(R.string.notif_channel_notices_desc)
    }

    val verseChannel = NotificationChannel(
      CHANNEL_DAILY_VERSE,
      context.getString(R.string.notif_channel_verse_name),
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = context.getString(R.string.notif_channel_verse_desc)
    }

    val accountChannel = NotificationChannel(
      CHANNEL_ACCOUNT,
      context.getString(R.string.notif_channel_account_name),
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = context.getString(R.string.notif_channel_account_desc)
    }

    manager.createNotificationChannels(listOf(noticesChannel, verseChannel, accountChannel))
  }

  fun showNotification(
    context: Context,
    channelId: String,
    notificationId: Int,
    title: String,
    body: String
  ): Boolean {
    ensureChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification_church)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(
        if (channelId == CHANNEL_DAILY_VERSE) NotificationCompat.PRIORITY_DEFAULT
        else NotificationCompat.PRIORITY_HIGH
      )
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      true
    }.getOrDefault(false)
  }

  fun notifyIfNew(
    context: Context,
    dedupeKey: String,
    channelId: String,
    title: String,
    body: String
  ): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(PREF_NOTIFIED_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
    if (existing.contains(dedupeKey)) {
      return false
    }
    val shown = showNotification(
      context = context,
      channelId = channelId,
      notificationId = dedupeKey.hashCode(),
      title = title,
      body = body
    )
    if (shown) {
      existing.add(dedupeKey)
      // Cap stored keys to prevent unbounded preference growth
      val trimmed = if (existing.size > 120) existing.drop(existing.size - 100).toSet() else existing
      prefs.edit().putStringSet(PREF_NOTIFIED_KEYS, trimmed).apply()
    }
    return shown
  }

  fun cacheDailyVerseSnapshot(
    context: Context,
    customVerses: List<BibleVerse>,
    config: DailyVerseConfig
  ) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val array = JSONArray()
    customVerses.take(30).forEach { v ->
      array.put(
        JSONObject().apply {
          put("verseId", v.verseId)
          put("bookReference", v.bookReference)
          put("textEnglish", v.textEnglish)
          put("textTamil", v.textTamil)
          put("devotionalEnglish", v.devotionalEnglish)
          put("devotionalTamil", v.devotionalTamil)
          put("pinDateKey", v.pinDateKey)
          put("publishedByPastor", v.publishedByPastor)
        }
      )
    }
    prefs.edit()
      .putBoolean(PREF_CACHED_AUTO_ENABLED, config.autoDailyEnabled)
      .putString(PREF_CACHED_VERSES_JSON, array.toString())
      .apply()
  }

  fun resolveVerseForReminder(
    context: Context,
    calendar: Calendar = Calendar.getInstance()
  ): BibleVerse {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val autoEnabled = prefs.getBoolean(PREF_CACHED_AUTO_ENABLED, true)
    val jsonStr = prefs.getString(PREF_CACHED_VERSES_JSON, null)
    val cachedVerses = buildList {
      if (!jsonStr.isNullOrBlank()) {
        runCatching {
          val arr = JSONArray(jsonStr)
          for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            add(
              BibleVerse(
                verseId = obj.optString("verseId"),
                bookReference = obj.optString("bookReference"),
                textEnglish = obj.optString("textEnglish"),
                textTamil = obj.optString("textTamil"),
                devotionalEnglish = obj.optString("devotionalEnglish"),
                devotionalTamil = obj.optString("devotionalTamil"),
                pinDateKey = obj.optString("pinDateKey"),
                publishedByPastor = obj.optString("publishedByPastor")
              )
            )
          }
        }
      }
    }
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val todayKey = BibleContentRepository.todayDateKey(calendar)
    return BibleContentRepository.resolveActiveDailyVerse(
      customVerses = cachedVerses,
      autoConfig = DailyVerseConfig(autoDailyEnabled = autoEnabled),
      dayOfYear = dayOfYear,
      todayKey = todayKey
    ).verse
  }

  fun showDailyVerseReminderNow(
    context: Context,
    calendar: Calendar = Calendar.getInstance()
  ): Boolean {
    val verse = resolveVerseForReminder(context, calendar)
    val isTamil = LanguageManager.getInstance(context).currentLanguage.value == LanguageManager.LANG_TAMIL
    val titlePrefix = if (isTamil) "இன்றைய வேத வசனம்" else "Daily Bible Verse"
    val verseText = if (isTamil) verse.textTamil.ifBlank { verse.textEnglish } else verse.textEnglish
    return showNotification(
      context = context,
      channelId = CHANNEL_DAILY_VERSE,
      notificationId = 6001,
      title = "$titlePrefix • ${verse.bookReference}",
      body = verseText
    )
  }

  fun syncFcmSubscriptions(
    context: Context,
    churchId: String,
    role: UserRole,
    noticesEnabled: Boolean = isNoticesEnabled(context),
    dailyVerseEnabled: Boolean = isDailyVerseEnabled(context),
    firestore: FirebaseFirestore? = null
  ) {
    val cleanChurchId = churchId.ifBlank { "church_main" }
    runCatching {
      val messaging = FirebaseMessaging.getInstance()
      val noticesTopic = "${cleanChurchId}_notices"
      val verseTopic = "${cleanChurchId}_daily_verse"
      val pastorsTopic = "${cleanChurchId}_pastors"

      if (noticesEnabled) {
        messaging.subscribeToTopic(noticesTopic)
      } else {
        messaging.unsubscribeFromTopic(noticesTopic)
      }

      if (dailyVerseEnabled) {
        messaging.subscribeToTopic(verseTopic)
      } else {
        messaging.unsubscribeFromTopic(verseTopic)
      }

      if (role == UserRole.PASTOR || role == UserRole.SUPER_ADMIN) {
        messaging.subscribeToTopic(pastorsTopic)
      } else {
        messaging.unsubscribeFromTopic(pastorsTopic)
      }

      messaging.token.addOnSuccessListener { token ->
        if (!token.isNullOrBlank()) {
          saveFcmToken(context, token, firestore)
        }
      }
    }
  }

  fun saveFcmToken(
    context: Context,
    token: String,
    firestore: FirebaseFirestore? = null
  ) {
    if (token.isBlank()) return
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_FCM_TOKEN, token).apply()

    runCatching {
      val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
      val db = firestore ?: com.example.core.util.FirestoreProvider.getInstance(context)
      db.collection("users").document(uid).update(
        mapOf(
          "fcmToken" to token,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }
  }
}
