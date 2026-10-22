package com.bodasmel.adminml

import com.bodasmel.adminml.core.Format
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O PocketBase v0.40 devolve as datas com espaço ("2026-10-22 03:52:29.868Z")
 * e não com "T". Antes desta correção o app caía no `catch` e mostrava a data
 * crua no card do convidado.
 */
class FormatTest {

  private fun esperado(isoComEspaco: String): String {
    val normalizado = isoComEspaco.replace(' ', 'T')
    return (
      java.time.OffsetDateTime.parse(normalizado, DateTimeFormatter.ISO_DATE_TIME)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", Locale("pt", "BR")))
    )
  }

  @Test
  fun `data do PocketBase com espaco e formatada`() {
    val raw = "2026-10-22 03:52:29.868Z"
    assertEquals(esperado(raw), Format.dateTime(raw))
  }

  @Test
  fun `data ISO com T e formatada`() {
    val raw = "2026-10-22T03:52:29.868Z"
    assertEquals(esperado(raw), Format.dateTime(raw))
  }

  @Test
  fun `data com deslocamento de fuso e formatada`() {
    val raw = "2026-10-22T00:52:29.868-03:00"
    assertEquals(esperado(raw), Format.dateTime(raw))
  }

  @Test
  fun `data vazia vira traco`() {
    assertEquals("-", Format.dateTime(null))
    assertEquals("-", Format.dateTime(""))
    assertEquals("-", Format.dateTime("   "))
  }

  @Test
  fun `data invalida volta como veio`() {
    assertEquals("nao-e-data", Format.dateTime("nao-e-data"))
  }
}
