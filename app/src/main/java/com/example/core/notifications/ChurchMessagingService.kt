package com.example.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChurchMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    NotificationHelper.saveFcmToken(applicationContext, token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    val type = data["type"].orEmpty().lowercase()

    if (type == "daily_verse" && !NotificationHelper.isDailyVerseEnabled(applicationContext)) {
      return
    }
    if (type != "daily_verse" && !NotificationHelper.isNoticesEnabled(applicationContext)) {
      return
    }

    val title = message.notification?.title
      ?: data["title"]
      ?: getString(com.example.R.string.app_name)
    val body = message.notification?.body
      ?: data["body"]
      ?: return

    val channelId = when (type) {
      "daily_verse" -> NotificationHelper.CHANNEL_DAILY_VERSE
      "account", "receipt" -> NotificationHelper.CHANNEL_ACCOUNT
      else -> NotificationHelper.CHANNEL_NOTICES
    }

    NotificationHelper.showNotification(
      context = applicationContext,
      channelId = channelId,
      notificationId = (message.messageId ?: "${title}_$body").hashCode(),
      title = title,
      body = body
    )
  }
}
