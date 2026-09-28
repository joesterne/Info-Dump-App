package com.example

import com.example.data.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun pomodoroPresets_haveValidDurations() {
    assertEquals(10, PomodoroPreset.QUICK_SPRINT.focusMinutes)
    assertEquals(3, PomodoroPreset.QUICK_SPRINT.breakMinutes)
    assertEquals(25, PomodoroPreset.CLASSIC.focusMinutes)
    assertEquals(5, PomodoroPreset.CLASSIC.breakMinutes)
    assertEquals(45, PomodoroPreset.HYPERFOCUS.focusMinutes)
    assertEquals(10, PomodoroPreset.HYPERFOCUS.breakMinutes)
  }

  @Test
  fun focusSession_creation_and_defaults() {
    val session = FocusSession(
      subject = "Astrophysics",
      targetDurationMinutes = 25,
      actualDurationMinutes = 25,
      reflectionMood = "In the Flow 🌊",
      notesSummary = "Studied neutron stars and magnetars"
    )
    assertEquals("Astrophysics", session.subject)
    assertEquals("FOCUS", session.sessionType)
    assertTrue(session.completed)
    assertEquals("In the Flow 🌊", session.reflectionMood)
    assertEquals("Studied neutron stars and magnetars", session.notesSummary)
  }
}
