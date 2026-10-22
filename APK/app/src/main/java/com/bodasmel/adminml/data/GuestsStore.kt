package com.bodasmel.adminml.data

import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.domain.GuestFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestsState(
  val loading: Boolean = true,
  val error: String? = null,
  val all: List<RsvpRecord> = emptyList(),
  val search: String = "",
  val statusFilter: String = GuestFilters.ALL,
  val categoryFilter: String = GuestFilters.ALL,
  val currentPage: Int = 1,
  val pageSize: Int = 25,
  val busy: Boolean = false,
  val message: String? = null,
) {
  val filtered: List<RsvpRecord>
    get() = GuestFilters.apply(all, search, statusFilter, categoryFilter)

  val pendentes: List<RsvpRecord> get() = all.filter { it.isPendente }

  val totalPages: Int get() = (filtered.size + pageSize - 1) / pageSize

  val pageItems: List<RsvpRecord>
    get() {
      val start = (currentPage - 1) * pageSize
      if (start >= filtered.size) return emptyList()
      return filtered.subList(start, minOf(start + pageSize, filtered.size))
    }

  val statusCounts: Map<String, Int> get() = GuestFilters.countByStatus(all, search, categoryFilter)

  val categoryCounts: Map<String, Int> get() = GuestFilters.countByCategory(all, search, statusFilter)

  val statsText: String
    get() {
      val base = "${all.size} registros (${pendentes.size} pendentes)"
      val hasFilter =
        search.isNotBlank() || statusFilter != GuestFilters.ALL || categoryFilter != GuestFilters.ALL
      return if (hasFilter) "$base • ${filtered.size} na seleção" else base
    }
}

/**
 * Estado compartilhado da lista de convidados.
 * Fica no container para que a tela e o Exportar usem exatamente a mesma seleção.
 */
class GuestsStore(private val container: AppContainer) {

  private val _state = MutableStateFlow(GuestsState())
  val state: StateFlow<GuestsState> = _state.asStateFlow()

  init {
    // Guarda de ordem de inicialização: ler o fluxo aqui garante que ele JÁ foi
    // criado quando a store é construída. Se alguém declarar as stores antes do
    // `rsvpEventFlow` no AppContainer, esta leitura lança NullPointerException
    // aqui (e o app abriria e fecharia) — falha imediata e fácil de diagnosticar.
    val eventos = container.rsvpEventFlow
    check(eventos != null) { "rsvpEventFlow precisa existir antes das stores" }
  }

  /**
   * Começa a observar os avisos do Gotify.
   *
   * NÃO é chamado no `init`: o container precisa estar totalmente construído
   * antes (foi exatamente essa corrida que derrubava o app na abertura).
   */
  fun observeRsvpEvents() {
    container.appScope.launch {
      try {
        container.rsvpEventFlow.collect { load(silent = true) }
      } catch (error: Throwable) {
        android.util.Log.e("AdminML", "Falha ao observar eventos de RSVP", error)
      }
    }
  }

  /** Carrega a lista (chamado quando há sessão ativa). */
  fun load(silent: Boolean = false) {
    container.appScope.launch {
      if (!silent) _state.update { it.copy(loading = true, error = null) }
      runCatching { container.client.listRsvp() }
        .onSuccess { records ->
          _state.update { current ->
            val totalPages =
              maxOf(1, (records.size + current.pageSize - 1) / current.pageSize)
            current.copy(
              loading = false,
              error = null,
              all = records,
              currentPage = current.currentPage.coerceIn(1, totalPages),
            )
          }
        }
        .onFailure { err ->
          _state.update { it.copy(loading = false, error = "Erro ao carregar convidados: ${err.message}") }
        }
    }
  }

  fun onSearchChange(value: String) = _state.update { it.copy(search = value, currentPage = 1) }

  fun onStatusFilterChange(value: String) = _state.update { it.copy(statusFilter = value, currentPage = 1) }

  fun onCategoryFilterChange(value: String) = _state.update { it.copy(categoryFilter = value, currentPage = 1) }

  fun goToPage(page: Int) =
    _state.update { it.copy(currentPage = page.coerceIn(1, maxOf(1, it.totalPages))) }

  fun nextPage() = goToPage(_state.value.currentPage + 1)

  fun previousPage() = goToPage(_state.value.currentPage - 1)

  fun toggleStatus(item: RsvpRecord) {
    val next =
      if (item.normalizedStatus == RsvpRecord.STATUS_PENDENTE) RsvpRecord.STATUS_CONFIRMADO
      else RsvpRecord.STATUS_PENDENTE
    container.appScope.launch {
      runCatching { container.client.updateRsvpStatus(item.id, next) }
        .onSuccess { updated ->
          _state.update { state ->
            state.copy(
              all = state.all.map { if (it.id == updated.id) updated else it },
              message = "Status alterado para $next",
            )
          }
        }
        .onFailure { err ->
          _state.update { it.copy(message = "Erro ao atualizar: ${err.message}") }
        }
    }
  }

  fun approveAll() {
    val pending = _state.value.pendentes
    if (pending.isEmpty()) return
    container.appScope.launch {
      _state.update { it.copy(busy = true, message = "Confirmando ${pending.size} convidados…") }
      var success = 0
      var failed = 0
      pending.forEach { record ->
        runCatching { container.client.updateRsvpStatus(record.id, RsvpRecord.STATUS_CONFIRMADO) }
          .onSuccess {
            success++
            _state.update { state ->
              state.copy(
                all = state.all.map {
                  if (it.id == record.id) it.copy(status = RsvpRecord.STATUS_CONFIRMADO) else it
                }
              )
            }
          }
          .onFailure { failed++ }
      }
      _state.update {
        it.copy(
          busy = false,
          message =
            if (failed > 0) "$success confirmados, $failed falha(s)" else "$success convidados confirmados!",
        )
      }
    }
  }

  fun consumeMessage() = _state.update { it.copy(message = null) }
}
