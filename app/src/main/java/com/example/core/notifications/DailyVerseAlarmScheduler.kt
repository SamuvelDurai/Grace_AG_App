package com.example.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object DailyVerseAlarmScheduler {

  private const val REQUEST_CODE_DAILY_VERSE = 6001

  fun nextSixAmTriggerMillis(now: Calendar = Calendar.getInstance()): Long {
    val target = (now.clone() as Calendar).apply {
      set(Calendar.HOUR_OF_DAY, 6)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    if (!target.after(now)) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
  }

  fun scheduleDailyVerse(context: Context) {
    NotificationHelper.ensureChannels(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildPendingIntent(context)
    val triggerAtMillis = nextSixAmTriggerMillis()

    // Uses inexact repeating daily wakeups so no restricted exact-alarm permissions are required
    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      triggerAtMillis,
      AlarmManager.INTERVAL_DAY,
      pendingIntent
    )
  }

  fun cancelDailyVerse(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    alarmManager.cancel(buildPendingIntent(context))
  }

  fun syncSchedule(context: Context) {
    if (NotificationHelper.isDailyVerseEnabled(context)) {
      scheduleDailyVerse(context)
    } else {
      cancelDailyVerse(context)
    }
  }

  private fun buildPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DailyVerseAlarmReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_DAILY_VERSE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}
