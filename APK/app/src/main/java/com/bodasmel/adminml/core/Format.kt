package com.bodasmel.adminml.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação de datas em pt-BR — paridade com `formatDate` do portal web. */
object Format {

  private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", Locale("pt", "BR"))

  fun dateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return runCatching {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
      }
      .getOrDefault(iso)
  }

  /** Nome do arquivo no padrão do portal: prefixo-AAAAMMDD-hash8 */
  fun stamp(): String {
    val now = java.time.LocalDateTime.now()
    val hash = (1..8).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    return "%04d%02d%02d-%s".format(now.year, now.monthValue, now.dayOfMonth, hash)
  }
}
