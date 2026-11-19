package com.example.core.model

enum class AccountStatus {
  PENDING,
  APPROVED,
  SUSPENDED,
  REJECTED;

  companion object {
    fun fromString(value: String?): AccountStatus {
      return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
  }
}
