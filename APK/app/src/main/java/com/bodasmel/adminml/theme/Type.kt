package com.bodasmel.adminml.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Tipografia do app alinhada ao portal web:
 * `body { font-family: var(--font-sans); font-size: 14px; line-height: 1.5 }`
 * (`public/css/base/reset.css`).
 */
private fun sans(
  size: TextUnit,
  lineHeight: TextUnit,
  weight: FontWeight = FontWeight.Normal,
  letterSpacing: TextUnit = 0.sp,
) =
  TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
  )

val Typography =
  Typography(
    displayLarge = sans(57.sp, 64.sp, FontWeight.Normal, (-0.25).sp),
    displayMedium = sans(45.sp, 52.sp),
    displaySmall = sans(36.sp, 44.sp),
    headlineLarge = sans(32.sp, 40.sp, FontWeight.SemiBold),
    headlineMedium = sans(28.sp, 36.sp, FontWeight.SemiBold),
    headlineSmall = sans(24.sp, 32.sp, FontWeight.SemiBold),
    titleLarge = sans(22.sp, 28.sp, FontWeight.SemiBold),
    titleMedium = sans(16.sp, 24.sp, FontWeight.SemiBold, 0.15.sp),
    titleSmall = sans(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = sans(16.sp, 24.sp, FontWeight.Normal, 0.5.sp),
    bodyMedium = sans(14.sp, 21.sp, FontWeight.Normal, 0.25.sp),
    bodySmall = sans(12.sp, 18.sp, FontWeight.Normal, 0.4.sp),
    labelLarge = sans(14.sp, 20.sp, FontWeight.Medium, 0.1.sp),
    labelMedium = sans(12.sp, 16.sp, FontWeight.Medium, 0.5.sp),
    labelSmall = sans(11.sp, 16.sp, FontWeight.Medium, 0.5.sp),
  )
