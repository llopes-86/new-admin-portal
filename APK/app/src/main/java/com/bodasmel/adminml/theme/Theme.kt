package com.bodasmel.adminml.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = PortalColors.DarkSidebarActive,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PortalColors.DarkPrimaryLight,
    onPrimaryContainer = PortalColors.DarkText,
    secondary = PortalColors.SavannaGreen,
    background = PortalColors.DarkBg,
    onBackground = PortalColors.DarkText,
    surface = PortalColors.DarkSurface,
    onSurface = PortalColors.DarkText,
    surfaceVariant = PortalColors.DarkBorder,
    onSurfaceVariant = PortalColors.DarkTextSecondary,
    error = PortalColors.LightDanger,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = PortalColors.DarkDangerLight,
    onErrorContainer = PortalColors.DarkDangerStrong,
    outline = PortalColors.DarkBorder,
    outlineVariant = PortalColors.DarkBorder,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PortalColors.LightPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PortalColors.LightPrimaryLight,
    onPrimaryContainer = PortalColors.LightText,
    secondary = PortalColors.SavannaGreen,
    background = PortalColors.LightBg,
    onBackground = PortalColors.LightText,
    surface = PortalColors.LightSurface,
    onSurface = PortalColors.LightText,
    surfaceVariant = PortalColors.LightBorder,
    onSurfaceVariant = PortalColors.LightTextSecondary,
    error = PortalColors.LightDanger,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = PortalColors.LightDangerLight,
    onErrorContainer = PortalColors.LightDangerStrong,
    outline = PortalColors.LightBorder,
    outlineVariant = PortalColors.LightBorder,
  )

/** Cores semânticas fora do ColorScheme (badges de status, topo da sidebar). */
object StatusPalette {
  fun isDark(dark: Boolean) = dark

  fun pendenteBg(dark: Boolean) = if (dark) PortalColors.DarkWarningLight else PortalColors.LightWarningLight
  fun pendenteFg(dark: Boolean) = if (dark) PortalColors.DarkWarningStrong else PortalColors.LightWarningStrong

  fun successBg(dark: Boolean) = if (dark) PortalColors.DarkSuccessLight else PortalColors.LightSuccessLight
  fun successFg(dark: Boolean) = if (dark) PortalColors.DarkSuccessText else PortalColors.LightSuccessText

  fun dangerBg(dark: Boolean) = if (dark) PortalColors.DarkDangerLight else PortalColors.LightDangerLight
  fun dangerFg(dark: Boolean) = if (dark) PortalColors.DarkDangerStrong else PortalColors.LightDangerStrong
}

@Composable
fun AdminMLTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = Typography,
    content = content,
  )
}
