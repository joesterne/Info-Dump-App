package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  textSizeMultiplier: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val scaledTypography = androidx.compose.material3.Typography(
    displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * textSizeMultiplier),
    displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * textSizeMultiplier),
    displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * textSizeMultiplier),
    headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * textSizeMultiplier),
    headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * textSizeMultiplier),
    headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * textSizeMultiplier),
    titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * textSizeMultiplier),
    titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * textSizeMultiplier),
    titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * textSizeMultiplier),
    bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * textSizeMultiplier),
    bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * textSizeMultiplier),
    bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * textSizeMultiplier),
    labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * textSizeMultiplier),
    labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * textSizeMultiplier),
    labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * textSizeMultiplier)
  )

  MaterialTheme(colorScheme = colorScheme, typography = scaledTypography, content = content)
}
