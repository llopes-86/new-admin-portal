package com.bodasmel.adminml.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatação de datas em pt-BR — paridade com `formatDate` do portal web.
 *
 * O PocketBase (v0.40) devolve os campos de data no formato SQLite
 * ("2026-10-22 03:52:29.868Z", com **espaço** no lugar do "T"), enquanto o
 * padrão ISO usa "T". Os dois são aceitos aqui — sem isso a data aparecia
 * crua no app.
 */
object Format {

  private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", Locale("pt", "BR"))

  fun dateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val raw = iso.trim()
    return parse(raw)?.atZone(ZoneId.systemDefault())?.format(dateTimeFormatter) ?: raw
  }

  /** Converte o texto do PocketBase/ISO em um instante, ou `null` se não der. */
  private fun parse(raw: String): Instant? {
    // "2026-10-22 03:52:29.868Z" -> "2026-10-22T03:52:29.868Z"
    val normalized = if (raw.length > 10 && raw[10] == ' ') raw.replaceRange(10, 11, "T") else raw

    runCatching { return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_DATE_TIME).toInstant() }
    runCatching { return Instant.parse(normalized) }
    // Sem fuso (raro): assume o fuso do aparelho.
    runCatching {
      return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .atZone(ZoneId.systemDefault())
        .toInstant()
    }
    return null
  }

  /** Nome do arquivo no padrão do portal: prefixo-AAAAMMDD-hash8 */
  fun stamp(): String {
    val now = java.time.LocalDateTime.now()
    val hash = (1..8).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    return "%04d%02d%02d-%s".format(now.year, now.monthValue, now.dayOfMonth, hash)
  }
}
