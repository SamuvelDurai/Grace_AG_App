package com.example.core.model

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale

enum class CelebrationType {
  BIRTHDAY,
  WEDDING_ANNIVERSARY
}

data class CelebrationWish(
  val senderUid: String = "",
  val senderName: String = "",
  val message: String = "",
  val createdAtText: String = ""
)

data class ChurchCelebration(
  val celebrationId: String = "",
  val churchId: String = "church_main",
  val memberUid: String = "",
  val memberName: String = "",
  val profilePhotoUrl: String = "",
  val type: String = CelebrationType.BIRTHDAY.name,
  val monthDay: String = "", // Privacy-safe "MM-DD" (omits birth year)
  val displayDate: String = "", // e.g. "19 Nov"
  val wishCount: Int = 0,
  val wishedByUids: List<String> = emptyList(),
  val recentWishes: List<CelebrationWish> = emptyList(),
  val updatedAt: Timestamp? = null
) {
  val celebrationType: CelebrationType
    get() = runCatching { CelebrationType.valueOf(type) }.getOrDefault(CelebrationType.BIRTHDAY)

  fun daysUntil(today: Calendar = Calendar.getInstance()): Int {
    val parsed = parseMonthDayComponents(monthDay) ?: return Int.MAX_VALUE
    val (month, day) = parsed
    val current = (today.clone() as Calendar).apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val target = (current.clone() as Calendar).apply {
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    if (target.before(current)) {
      target.add(Calendar.YEAR, 1)
      target.set(Calendar.MONTH, month - 1)
      target.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(target.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    val diffMillis = target.timeInMillis - current.timeInMillis
    return (diffMillis / (1000L * 60L * 60L * 24L)).toInt()
  }

  fun isToday(today: Calendar = Calendar.getInstance()): Boolean = daysUntil(today) == 0

  fun isWithinNextWeek(today: Calendar = Calendar.getInstance()): Boolean {
    val days = daysUntil(today)
    return days in 0..7
  }

  companion object {
    private val MONTH_NAMES = listOf(
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun parseMonthDayComponents(monthDay: String): Pair<Int, Int>? {
      val parts = monthDay.trim().split("-")
      if (parts.size != 2) return null
      val m = parts[0].toIntOrNull() ?: return null
      val d = parts[1].toIntOrNull() ?: return null
      if (m !in 1..12 || d !in 1..31) return null
      return m to d
    }

    fun formatDisplayDate(month: Int, day: Int): String {
      if (month !in 1..12 || day !in 1..31) return ""
      return String.format(Locale.US, "%02d %s", day, MONTH_NAMES[month - 1])
    }

    fun formatMonthDay(month: Int, day: Int): String {
      if (month !in 1..12 || day !in 1..31) return ""
      return String.format(Locale.US, "%02d-%02d", month, day)
    }

    /**
     * Parses common date strings entered by believers or pastors:
     * - "15/05/1990", "15-05-1990" (DD/MM/YYYY)
     * - "1990-05-15", "1990/05/15" (YYYY-MM-DD)
     * - "15 May 1990", "15 May", "May 15"
     * - "05-15" (MM-DD)
     * Returns Pair(monthDay="MM-DD", displayDate="DD MMM") or null if invalid.
     */
    fun parseFlexibleDate(raw: String): Pair<String, String>? {
      val trimmed = raw.trim()
      if (trimmed.isBlank()) return null

      // MM-DD directly
      val mmddMatch = Regex("^(\\d{2})-(\\d{2})$").matchEntire(trimmed)
      if (mmddMatch != null) {
        val m = mmddMatch.groupValues[1].toInt()
        val d = mmddMatch.groupValues[2].toInt()
        if (m in 1..12 && d in 1..31) return formatMonthDay(m, d) to formatDisplayDate(m, d)
      }

      // YYYY-MM-DD or YYYY/MM/DD
      val isoMatch = Regex("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})$").matchEntire(trimmed)
      if (isoMatch != null) {
        val m = isoMatch.groupValues[2].toInt()
        val d = isoMatch.groupValues[3].toInt()
        if (m in 1..12 && d in 1..31) return formatMonthDay(m, d) to formatDisplayDate(m, d)
      }

      // DD/MM/YYYY or DD-MM-YYYY or DD/MM
      val dmyMatch = Regex("^(\\d{1,2})[-/](\\d{1,2})(?:[-/](\\d{2,4}))?$").matchEntire(trimmed)
      if (dmyMatch != null) {
        val first = dmyMatch.groupValues[1].toInt()
        val second = dmyMatch.groupValues[2].toInt()
        val (day, month) = if (second in 1..12 && first in 1..31) {
          first to second
        } else if (first in 1..12 && second in 1..31) {
          second to first
        } else {
          return null
        }
        return formatMonthDay(month, day) to formatDisplayDate(month, day)
      }

      // "15 May 1990" or "15 May"
      val dMonMatch = Regex("^(\\d{1,2})\\s+([A-Za-z]{3,9})(?:[,\\s]+\\d{2,4})?$").matchEntire(trimmed)
      if (dMonMatch != null) {
        val d = dMonMatch.groupValues[1].toIntOrNull() ?: return null
        val m = monthNameToNumber(dMonMatch.groupValues[2]) ?: return null
        if (d in 1..31) return formatMonthDay(m, d) to formatDisplayDate(m, d)
      }

      // "May 15, 1990" or "May 15"
      val monDMatch = Regex("^([A-Za-z]{3,9})\\s+(\\d{1,2})(?:[,\\s]+\\d{2,4})?$").matchEntire(trimmed)
      if (monDMatch != null) {
        val m = monthNameToNumber(monDMatch.groupValues[1]) ?: return null
        val d = monDMatch.groupValues[2].toIntOrNull() ?: return null
        if (d in 1..31) return formatMonthDay(m, d) to formatDisplayDate(m, d)
      }

      return null
    }

    private fun monthNameToNumber(name: String): Int? {
      val prefix = name.trim().lowercase(Locale.US).take(3)
      val idx = MONTH_NAMES.indexOfFirst { it.lowercase(Locale.US) == prefix }
      return if (idx >= 0) idx + 1 else null
    }
  }
}
