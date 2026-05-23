package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CoralAccent,
    secondary = DeepTeal,
    tertiary = GoldenHighlight,
    background = DarkCharcoal,
    surface = Color(0xFF253331),
    onPrimary = OffWhite,
    onSecondary = LightSand,
    onBackground = LightSand,
    onSurface = OffWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepTeal,
    secondary = CoralAccent,
    tertiary = LightTeal,
    background = LightSand,
    surface = OffWhite,
    onPrimary = OffWhite,
    onSecondary = OffWhite,
    onBackground = DarkCharcoal,
    onSurface = DarkCharcoal,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Always use light theme for Fiji-branded sand/teal colors
  // Dynamic color is disabled by default to maintain Fiji themed brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
