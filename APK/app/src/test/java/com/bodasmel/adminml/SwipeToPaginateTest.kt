package com.bodasmel.adminml

import com.bodasmel.adminml.ui.components.SwipeToPaginate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Swipe de paginação — paridade com o `paginateSwipe` do portal web.
 */
class SwipeToPaginateTest {

  @Test
  fun `arrastar para a esquerda vai para a proxima pagina`() {
    assertEquals(2, SwipeToPaginate.nextPage(current = 1, totalPages = 5, dragTotal = -200f))
  }

  @Test
  fun `arrastar para a direita volta para a pagina anterior`() {
    assertEquals(2, SwipeToPaginate.nextPage(current = 3, totalPages = 5, dragTotal = 200f))
  }

  @Test
  fun `arrasto curto nao troca de pagina`() {
    assertNull(SwipeToPaginate.nextPage(current = 1, totalPages = 5, dragTotal = -40f))
  }

  @Test
  fun `nao passa da primeira pagina`() {
    assertNull(SwipeToPaginate.nextPage(current = 1, totalPages = 5, dragTotal = 200f))
  }

  @Test
  fun `nao passa da ultima pagina`() {
    // Ja esta na ultima: o gesto nao produz troca de pagina
    assertNull(SwipeToPaginate.nextPage(current = 5, totalPages = 5, dragTotal = -200f))
  }

  @Test
  fun `pagina unica nao troca`() {
    assertNull(SwipeToPaginate.nextPage(current = 1, totalPages = 1, dragTotal = -400f))
  }
}
