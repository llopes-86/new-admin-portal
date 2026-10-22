package com.bodasmel.adminml.domain

import com.bodasmel.adminml.data.RsvpRecord

/**
 * Regras de filtro da lista de convidados — paridade com
 * `public/js/features/guests.js` do portal web.
 */
object GuestFilters {

  const val ALL = "all"

  val STATUS_OPTIONS: List<Pair<String, String>> =
    listOf(ALL to "Todos", "Pendente" to "Pendentes", "Confirmado" to "Confirmados")

  val CATEGORY_OPTIONS: List<Pair<String, String>> =
    listOf(
      ALL to "Todas as categorias",
      "adultos" to "Adultos",
      "criancas" to "Crianças",
      "necessidades" to "Necessidades especiais",
      "veganos" to "Veganos",
      "lactose" to "Intolerância à lactose",
    )

  /** Busca por nome, responsável, nome da criança e telefone (somente dígitos, >= 2). */
  fun matchesSearch(item: RsvpRecord, search: String, searchDigits: String): Boolean {
    if (search.isEmpty()) return true
    val haystack =
      listOfNotNull(item.nome, item.responsavel, item.nomeCrianca).joinToString(" ").lowercase()
    if (haystack.contains(search)) return true
    if (searchDigits.length >= 2) {
      val contatoDigits = (item.contato ?: "").filter { it.isDigit() }
      if (contatoDigits.isNotEmpty() && contatoDigits.contains(searchDigits)) return true
    }
    return false
  }

  fun matchesCategory(item: RsvpRecord, category: String): Boolean {
    return when (category) {
      "adultos" -> item.adulto == "1"
      "criancas" -> item.criancas == "1"
      "necessidades" -> {
        val joined = ParseUtils.parseRestricoes(item.restricoes).joinToString(",").lowercase()
        joined.contains("necessidade") || joined.contains("especial")
      }
      "veganos" -> {
        val joined = ParseUtils.parseRestricoes(item.restricoes).joinToString(",").lowercase()
        joined.contains("vegano") || joined.contains("vegan")
      }
      "lactose" -> {
        val joined = ParseUtils.parseRestricoes(item.restricoes).joinToString(",").lowercase()
        joined.contains("lactose")
      }
      else -> true
    }
  }

  fun apply(
    all: List<RsvpRecord>,
    search: String,
    statusFilter: String,
    categoryFilter: String,
  ): List<RsvpRecord> {
    val normalized = search.trim().lowercase()
    val digits = normalized.filter { it.isDigit() }
    return all.filter { item ->
      if (!matchesSearch(item, normalized, digits)) return@filter false
      if (statusFilter != ALL && item.normalizedStatus != statusFilter) return@filter false
      if (categoryFilter != ALL && !matchesCategory(item, categoryFilter)) return@filter false
      true
    }
  }

  /** Contadores das opções de status (respeitando busca e categoria). */
  fun countByStatus(
    all: List<RsvpRecord>,
    search: String,
    categoryFilter: String,
  ): Map<String, Int> {
    val normalized = search.trim().lowercase()
    val digits = normalized.filter { it.isDigit() }
    return STATUS_OPTIONS.associate { (value, _) ->
      value to
        all.count { item ->
          if (!matchesSearch(item, normalized, digits)) return@count false
          if (categoryFilter != ALL && !matchesCategory(item, categoryFilter)) return@count false
          if (value != ALL && item.normalizedStatus != value) return@count false
          true
        }
    }
  }

  /** Contadores das opções de categoria (respeitando busca e status). */
  fun countByCategory(
    all: List<RsvpRecord>,
    search: String,
    statusFilter: String,
  ): Map<String, Int> {
    val normalized = search.trim().lowercase()
    val digits = normalized.filter { it.isDigit() }
    return CATEGORY_OPTIONS.associate { (value, _) ->
      value to
        all.count { item ->
          if (!matchesSearch(item, normalized, digits)) return@count false
          if (statusFilter != ALL && item.normalizedStatus != statusFilter) return@count false
          if (value != ALL && !matchesCategory(item, value)) return@count false
          true
        }
    }
  }
}
