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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentRed,
    secondary = White,
    tertiary = GrayText,
    background = PureBlack,
    surface = DarkGray,
    onPrimary = White,
    onSecondary = PureBlack,
    onTertiary = White,
    onBackground = White,
    onSurface = White
  )

private val LightColorScheme = DarkColorScheme // Forced dark theme

@Composable
fun MyApplicationTheme(
  // Always dark theme for this app
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
