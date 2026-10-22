package com.bodasmel.adminml.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodasmel.adminml.theme.StatusPalette

/**
 * Badge de status clicável — paridade com `.badge` do portal web
 * (Pendente / Confirmado / Aprovado / Rejeitado).
 */
@Composable
fun StatusBadge(
  status: String,
  dark: Boolean,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val scale by animateFloatAsState(targetValue = 1f, label = "badgeScale")

  val (background, foreground) =
    when (status.lowercase()) {
      "pendente" -> StatusPalette.pendenteBg(dark) to StatusPalette.pendenteFg(dark)
      "aprovado", "confirmado" -> StatusPalette.successBg(dark) to StatusPalette.successFg(dark)
      "rejeitado" -> StatusPalette.dangerBg(dark) to StatusPalette.dangerFg(dark)
      else -> Color(0xFFE2E8F0) to Color(0xFF1E293B)
    }

  Text(
    text = status,
    modifier =
      modifier
        .clip(RoundedCornerShape(999.dp))
        .background(background)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 10.dp, vertical = 3.dp)
        .scale(scale),
    color = foreground,
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
  )
}
