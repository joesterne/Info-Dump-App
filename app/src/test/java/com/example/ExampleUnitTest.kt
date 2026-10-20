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

  @Test
  fun appSettings_defaults_and_high_contrast_support() {
    val defaultSettings = com.example.data.AppSettings()
    assertTrue(defaultSettings.isDarkMode)
    assertEquals(1.0f, defaultSettings.textSizeMultiplier, 0.001f)
    org.junit.Assert.assertFalse(defaultSettings.isHighContrast)

    val highContrastSettings = defaultSettings.copy(
      isHighContrast = true,
      textSizeMultiplier = 1.4f
    )
    assertTrue(highContrastSettings.isHighContrast)
    assertEquals(1.4f, highContrastSettings.textSizeMultiplier, 0.001f)
  }

  @Test
  fun dynamicFontScaling_clamping_and_presets() {
    val presets = listOf(0.85f, 1.0f, 1.20f, 1.40f, 1.70f, 2.0f)
    presets.forEach { scale ->
      val clamped = scale.coerceIn(0.75f, 2.2f)
      assertEquals(scale, clamped, 0.001f)
    }

    val belowMin = 0.5f.coerceIn(0.75f, 2.2f)
    assertEquals(0.75f, belowMin, 0.001f)

    val aboveMax = 3.0f.coerceIn(0.75f, 2.2f)
    assertEquals(2.2f, aboveMax, 0.001f)
  }
}
