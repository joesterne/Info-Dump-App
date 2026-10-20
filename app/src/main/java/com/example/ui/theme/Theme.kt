package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val LocalHighContrast = compositionLocalOf { false }

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

private val HighContrastDarkColorScheme = darkColorScheme(
  primary = HcDarkPrimary,
  onPrimary = HcDarkOnPrimary,
  secondary = HcDarkSecondary,
  onSecondary = HcDarkOnSecondary,
  tertiary = HcDarkTertiary,
  onTertiary = HcDarkOnTertiary,
  background = HcDarkBackground,
  onBackground = HcDarkOnBackground,
  surface = HcDarkSurface,
  onSurface = HcDarkOnSurface,
  surfaceVariant = HcDarkSurfaceVariant,
  onSurfaceVariant = HcDarkOnSurface,
  outline = HcDarkOutline,
  outlineVariant = HcDarkOutlineVariant
)

private val HighContrastLightColorScheme = lightColorScheme(
  primary = HcLightPrimary,
  onPrimary = HcLightOnPrimary,
  secondary = HcLightSecondary,
  onSecondary = HcLightOnSecondary,
  tertiary = HcLightTertiary,
  onTertiary = HcLightOnTertiary,
  background = HcLightBackground,
  onBackground = HcLightOnBackground,
  surface = HcLightSurface,
  onSurface = HcLightOnSurface,
  surfaceVariant = HcLightSurfaceVariant,
  onSurfaceVariant = HcLightOnSurface,
  outline = HcLightOutline,
  outlineVariant = HcLightOutlineVariant
)

// Proportionately scale font size and line height so text never collides or visual-crowds
private fun TextStyle.scaleProportionately(multiplier: Float): TextStyle {
  val scaledSize = (this.fontSize.value * multiplier).sp
  val safeLineHeight = (scaledSize.value * 1.38f).sp
  return this.copy(
    fontSize = scaledSize,
    lineHeight = safeLineHeight
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  textSizeMultiplier: Float = 1.0f,
  isHighContrast: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      isHighContrast && darkTheme -> HighContrastDarkColorScheme
      isHighContrast && !darkTheme -> HighContrastLightColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val multiplier = textSizeMultiplier.coerceIn(0.75f, 2.2f)

  val scaledTypography = androidx.compose.material3.Typography(
    displayLarge = Typography.displayLarge.scaleProportionately(multiplier),
    displayMedium = Typography.displayMedium.scaleProportionately(multiplier),
    displaySmall = Typography.displaySmall.scaleProportionately(multiplier),
    headlineLarge = Typography.headlineLarge.scaleProportionately(multiplier),
    headlineMedium = Typography.headlineMedium.scaleProportionately(multiplier),
    headlineSmall = Typography.headlineSmall.scaleProportionately(multiplier),
    titleLarge = Typography.titleLarge.scaleProportionately(multiplier),
    titleMedium = Typography.titleMedium.scaleProportionately(multiplier),
    titleSmall = Typography.titleSmall.scaleProportionately(multiplier),
    bodyLarge = Typography.bodyLarge.scaleProportionately(multiplier),
    bodyMedium = Typography.bodyMedium.scaleProportionately(multiplier),
    bodySmall = Typography.bodySmall.scaleProportionately(multiplier),
    labelLarge = Typography.labelLarge.scaleProportionately(multiplier),
    labelMedium = Typography.labelMedium.scaleProportionately(multiplier),
    labelSmall = Typography.labelSmall.scaleProportionately(multiplier)
  )

  CompositionLocalProvider(LocalHighContrast provides isHighContrast) {
    MaterialTheme(colorScheme = colorScheme, typography = scaledTypography, content = content)
  }
}
