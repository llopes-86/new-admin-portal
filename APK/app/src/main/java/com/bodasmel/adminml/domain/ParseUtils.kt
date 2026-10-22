package com.bodasmel.adminml.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Réplica exata de `public/js/utils/parse.js` do portal web.
 *
 * `restricoes` pode chegar como JSON (array) ou como texto separado por vírgula.
 */
object ParseUtils {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  fun parseRestricoes(raw: String?): List<String> {
    val source = raw?.trim().orEmpty()
    if (source.isEmpty()) return emptyList()

    val parsed = runCatching { json.parseToJsonElement(source) }.getOrNull()
    if (parsed != null) {
      when (parsed) {
        is JsonArray -> return parsed.mapNotNull { elementToString(it) }.filter { it.isNotBlank() }
        is JsonPrimitive ->
          if (parsed.isString) {
            val single = parsed.content
            if (single.isNotBlank()) return listOf(single)
          }
        else -> return emptyList()
      }
    }

    return source.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  }

  private fun elementToString(element: JsonElement): String? =
    when (element) {
      is JsonPrimitive -> element.content
      else -> element.toString()
    }

  fun hasNecessidades(restricoes: List<String>): Boolean {
    val joined = restricoes.joinToString(",").lowercase()
    return joined.contains("necessidade") || joined.contains("especial")
  }

  fun outrasRestricoes(restricoes: List<String>): List<String> =
    restricoes.filter {
      val lower = it.lowercase()
      !lower.contains("necessidade") && !lower.contains("especial")
    }
}
