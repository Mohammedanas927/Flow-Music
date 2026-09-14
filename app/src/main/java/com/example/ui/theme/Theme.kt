package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlowLightColorScheme = lightColorScheme(
  primary = FlowPrimary,
  onPrimary = Color.White,
  primaryContainer = FlowPrimaryLight,
  onPrimaryContainer = FlowPrimaryDark,
  secondary = FlowSecondary,
  onSecondary = Color.White,
  secondaryContainer = FlowSecondaryContainer,
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = FlowAccent,
  onTertiary = Color.White,
  background = FlowBackground,
  onBackground = FlowTextPrimary,
  surface = FlowSurface,
  onSurface = FlowTextPrimary,
  surfaceVariant = FlowSurfaceVariant,
  onSurfaceVariant = FlowTextSecondary,
  outline = FlowCardBorder,
  outlineVariant = Color(0xFFF1F5F9)
)

@Composable
fun FlowMusicTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = FlowLightColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  FlowMusicTheme(content = content)
}

