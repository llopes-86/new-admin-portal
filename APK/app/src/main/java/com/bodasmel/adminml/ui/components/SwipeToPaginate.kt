package com.bodasmel.adminml.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Swipe horizontal para paginar — paridade com o `initSwipe`/`paginateSwipe`
 * do portal web (que pagina a aba atual: convidados ou mural).
 *
 * O gesto só dispara quando o deslocamento horizontal é dominante, então não
 * briga com a rolagem vertical da lista nem com a rolagem horizontal de tabelas.
 */
object SwipeToPaginate {

  /** Distância mínima (px) para trocar de página — mesma ideia do web (60px). */
  const val MIN_DISTANCE = 120f

  /**
   * Decide a página de destino a partir do deslocamento horizontal acumulado.
   * Retorna `null` quando o gesto não deve trocar de página.
   */
  fun nextPage(current: Int, totalPages: Int, dragTotal: Float): Int? {
    if (totalPages <= 1) return null
    if (abs(dragTotal) < MIN_DISTANCE) return null
    val target = if (dragTotal < 0) current + 1 else current - 1
    return target.coerceIn(1, totalPages).takeIf { it != current }
  }
}

/** Aplica o swipe de paginação em um container. */
fun Modifier.swipeToPaginate(
  currentPage: Int,
  totalPages: Int,
  onPage: (Int) -> Unit,
): Modifier =
  this.pointerInput(currentPage, totalPages) {
    var dragTotal = 0f
    detectHorizontalDragGestures(
      onDragStart = { dragTotal = 0f },
      onDragEnd = {
        SwipeToPaginate.nextPage(currentPage, totalPages, dragTotal)?.let(onPage)
      },
      onHorizontalDrag = { change, amount ->
        dragTotal += amount
        change.consume()
      },
    )
  }
