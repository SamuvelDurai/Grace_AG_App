package com.example

import com.example.core.model.ChurchCelebration
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Local JVM unit tests for GRACE AG core logic.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun churchCelebration_parsesFlexibleDatesAndComputesWeeklyReminder() {
    assertEquals("11-19" to "19 Nov", ChurchCelebration.parseFlexibleDate("19/11/1995"))
    assertEquals("11-19" to "19 Nov", ChurchCelebration.parseFlexibleDate("1995-11-19"))
    assertEquals("11-19" to "19 Nov", ChurchCelebration.parseFlexibleDate("19 Nov"))
    assertEquals("01-03" to "03 Jan", ChurchCelebration.parseFlexibleDate("01-03"))
    assertNull(ChurchCelebration.parseFlexibleDate("99/99/1995"))

    val dec29 = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 29, 8, 0, 0) }
    val todayBday = ChurchCelebration(memberName = "Sister Ruth", monthDay = "12-29", displayDate = "29 Dec")
    val newYearBday = ChurchCelebration(memberName = "Brother Paul", monthDay = "01-02", displayDate = "2 Jan")
    val laterBday = ChurchCelebration(memberName = "Sister Mary", monthDay = "01-15", displayDate = "15 Jan")

    assertTrue(todayBday.isToday(dec29))
    assertEquals(0, todayBday.daysUntil(dec29))
    assertTrue(todayBday.isWithinNextWeek(dec29))

    assertFalse(newYearBday.isToday(dec29))
    assertEquals(4, newYearBday.daysUntil(dec29))
    assertTrue(newYearBday.isWithinNextWeek(dec29))

    assertEquals(17, laterBday.daysUntil(dec29))
    assertFalse(laterBday.isWithinNextWeek(dec29))
  }
}
