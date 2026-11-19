package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyVerseAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (!NotificationHelper.isDailyVerseEnabled(context)) return
    if (!NotificationHelper.hasNotificationPermission(context)) return
    NotificationHelper.showDailyVerseReminderNow(context)
  }
}
