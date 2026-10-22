package com.bodasmel.adminml.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paginação com elipses — paridade com `renderPagination` do portal web.
 */
@Composable
fun PaginationBar(
  currentPage: Int,
  totalPages: Int,
  onPage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (totalPages <= 1) return

  val maxVisible = 5
  var start = (currentPage - maxVisible / 2).coerceAtLeast(1)
  var end = (start + maxVisible - 1).coerceAtMost(totalPages)
  if (end - start + 1 < maxVisible) start = (end - maxVisible + 1).coerceAtLeast(1)

  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (start > 1) {
      PageButton("1", currentPage == 1) { onPage(1) }
      if (start > 2) Ellipsis()
    }
    for (page in start..end) {
      PageButton(page.toString(), page == currentPage) { onPage(page) }
    }
    if (end < totalPages) {
      if (end < totalPages - 1) Ellipsis()
      PageButton(totalPages.toString(), currentPage == totalPages) { onPage(totalPages) }
    }
  }
}

@Composable
private fun PageButton(label: String, active: Boolean, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.size(36.dp)
        .clip(CircleShape)
        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
      color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun Ellipsis() {
  Text(
    text = "...",
    modifier = Modifier.widthIn(min = 16.dp),
    fontSize = 13.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
