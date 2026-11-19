package com.example.core.model

enum class UserRole {
  BELIEVER,
  PASTOR,
  SUPER_ADMIN;

  companion object {
    fun fromString(value: String?): UserRole {
      return entries.find { it.name.equals(value, ignoreCase = true) } ?: BELIEVER
    }
  }
}
