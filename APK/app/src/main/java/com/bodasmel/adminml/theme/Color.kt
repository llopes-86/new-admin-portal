package com.bodasmel.adminml.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta extraída de `public/css/base/variables.css` do portal web
 * (paridade visual entre o app e o portal).
 */
object PortalColors {
  // Light
  val LightBg = Color(0xFFF8FAFC)
  val LightSurface = Color(0xFFFFFFFF)
  val LightSidebar = Color(0xFF0F172A)
  val LightSidebarHover = Color(0xFF1E293B)
  val LightSidebarText = Color(0xFF94A3B8)
  val LightPrimary = Color(0xFF6366F1)
  val LightPrimaryHover = Color(0xFF4F46E5)
  val LightPrimaryLight = Color(0xFFEEF2FF)
  val LightSuccess = Color(0xFF10B981)
  val LightSuccessLight = Color(0xFFECFDF5)
  val LightSuccessText = Color(0xFF065F46)
  val LightDanger = Color(0xFFEF4444)
  val LightDangerLight = Color(0xFFFEF2F2)
  val LightDangerStrong = Color(0xFF991B1B)
  val LightWarning = Color(0xFFF59E0B)
  val LightWarningLight = Color(0xFFFFFBEB)
  val LightWarningStrong = Color(0xFF92400E)
  val LightText = Color(0xFF1E293B)
  val LightTextSecondary = Color(0xFF64748B)
  val LightTextMuted = Color(0xFF94A3B8)
  val LightBorder = Color(0xFFE2E8F0)

  // Dark
  val DarkBg = Color(0xFF0F172A)
  val DarkSurface = Color(0xFF1E293B)
  val DarkSidebar = Color(0xFF020617)
  val DarkSidebarHover = Color(0xFF1E293B)
  val DarkSidebarText = Color(0xFF94A3B8)
  val DarkSidebarActive = Color(0xFF818CF8)
  val DarkPrimaryLight = Color(0x1A1E1BFF)
  val DarkSuccessLight = Color(0xFF064E3B)
  val DarkSuccessText = Color(0xFFA7F3D0)
  val DarkDangerLight = Color(0xFF450A0A)
  val DarkDangerStrong = Color(0xFFFCA5A5)
  val DarkWarningLight = Color(0xFF451A03)
  val DarkWarningStrong = Color(0xFFFCD34D)
  val DarkText = Color(0xFFE2E8F0)
  val DarkTextSecondary = Color(0xFF94A3B8)
  val DarkTextMuted = Color(0xFF64748B)
  val DarkBorder = Color(0xFF334155)

  // Login (paridade com as regras .login-* do portal web)
  val LoginCardBg = Color(0xEAF5E6D3) // rgba(245,230,211,0.92)
  val LoginOverlayCardBg = Color(0xFAF5E6D3) // rgba(245,230,211,0.98)
  val LoginGoldBorder = Color(0x26D4A017) // rgba(212,160,23,0.15)
  val LoginInputBg = Color(0xD9FFFFFF) // rgba(255,255,255,0.85)
  val LoginInputBorder = Color(0x1F5E3A2A) // rgba(94,58,42,0.12)
  val LoginFocusRing = Color(0x26B85C38) // rgba(184,92,56,0.15)
  val LoginPlaceholder = Color(0xFF6B5D52)
  val LoginErrorBg = Color(0x1AEF4444) // rgba(239,68,68,0.1)
  val LoginErrorBorder = Color(0x33EF4444) // rgba(239,68,68,0.2)

  // Marca
  val NightBlue = Color(0xFF0F1A2E)
  val Sand = Color(0xFFF5E6D3)
  val Gold = Color(0xFFD4A017)
  val Terracotta = Color(0xFFB85C38)
  val SavannaGreen = Color(0xFF5B7F3F)
  val Brown = Color(0xFF5E3A2A)
}
