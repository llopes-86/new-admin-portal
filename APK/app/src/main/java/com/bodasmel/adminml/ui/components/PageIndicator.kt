package com.bodasmel.adminml.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Indicador flutuante "página atual/total" — paridade com o `showPageIndicator`
 * do portal web (aparece e some ao trocar de página).
 */
@Composable
fun BoxScope.PageIndicator(currentPage: Int, totalPages: Int) {
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(currentPage, totalPages) {
    if (totalPages <= 1) return@LaunchedEffect
    visible = true
    delay(700)
    visible = false
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
  ) {
    Text(
      text = "$currentPage/$totalPages",
      modifier =
        Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
          .padding(horizontal = 12.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.surface,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}
