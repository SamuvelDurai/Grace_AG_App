package com.example.core.model

data class AuditLogEntry(
  val logId: String = "",
  val churchId: String = "church_main",
  val action: String = "",
  val actorName: String = "",
  val actorUid: String = "",
  val timestampText: String = "",
  val status: String = "SUCCESS"
)
