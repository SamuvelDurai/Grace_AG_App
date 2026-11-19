package com.example.core.util

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreProvider {
  @Volatile
  private var configured = false

  fun getInstance(context: Context): FirebaseFirestore {
    val dbId = context.applicationContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(dbId)
    if (!configured) {
      synchronized(this) {
        if (!configured) {
          try {
            val cacheSettings = PersistentCacheSettings.newBuilder()
              .setSizeBytes(40L * 1024L * 1024L) // 40 MB offline cache to save free-tier reads
              .build()
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
              .setLocalCacheSettings(cacheSettings)
              .build()
          } catch (_: IllegalStateException) {
            // Firestore instance already started (e.g., in Robolectric/emulator tests)
          }
          configured = true
        }
      }
    }
    return db
  }
}
