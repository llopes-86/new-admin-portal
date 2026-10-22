package com.bodasmel.adminml.data

import com.bodasmel.adminml.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessagesState(
  val loading: Boolean = true,
  val error: String? = null,
  val all: List<MessageRecord> = emptyList(),
  val filter: String = "all",
  val currentPage: Int = 1,
  val pageSize: Int = 12,
  val busy: Boolean = false,
  val message: String? = null,
) {
  val filtered: List<MessageRecord>
    get() = if (filter == "all") all else all.filter { it.normalizedStatus == filter }

  val pendentes: List<MessageRecord>
    get() = all.filter { it.normalizedStatus == MessageRecord.STATUS_PENDENTE }

  val totalPages: Int get() = (filtered.size + pageSize - 1) / pageSize

  val pageItems: List<MessageRecord>
    get() {
      val start = (currentPage - 1) * pageSize
      if (start >= filtered.size) return emptyList()
      return filtered.subList(start, minOf(start + pageSize, filtered.size))
    }

  val counts: Map<String, Int>
    get() =
      mapOf(
        "all" to all.size,
        MessageRecord.STATUS_PENDENTE to all.count { it.normalizedStatus == MessageRecord.STATUS_PENDENTE },
        MessageRecord.STATUS_APROVADO to all.count { it.normalizedStatus == MessageRecord.STATUS_APROVADO },
        MessageRecord.STATUS_REJEITADO to all.count { it.normalizedStatus == MessageRecord.STATUS_REJEITADO },
      )

  val statsText: String
    get() {
      val base = "${all.size} mensagens (${pendentes.size} pendentes)"
      return if (filter != "all") "$base • ${filtered.size} na seleção" else base
    }
}

/** Estado compartilhado do mural — usado pela tela e pelo Exportar. */
class MessagesStore(private val container: AppContainer) {

  private val _state = MutableStateFlow(MessagesState())
  val state: StateFlow<MessagesState> = _state.asStateFlow()

  init {
    // Guarda de ordem de inicialização (ver comentário em GuestsStore).
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
      runCatching { container.client.listMessages() }
        .onSuccess { records ->
          _state.update { it.copy(loading = false, error = null, all = records, currentPage = 1) }
        }
        .onFailure { err ->
          _state.update { it.copy(loading = false, error = "Erro ao carregar mensagens: ${err.message}") }
        }
    }
  }

  fun onFilterChange(value: String) = _state.update { it.copy(filter = value, currentPage = 1) }

  fun goToPage(page: Int) =
    _state.update { it.copy(currentPage = page.coerceIn(1, maxOf(1, it.totalPages))) }

  fun setStatus(record: MessageRecord, status: String) {
    container.appScope.launch {
      runCatching { container.client.updateMessageStatus(record.id, status) }
        .onSuccess { updated ->
          _state.update { state ->
            state.copy(
              all = state.all.map { if (it.id == updated.id) updated else it },
              message =
                when (updated.normalizedStatus) {
                  MessageRecord.STATUS_APROVADO -> "Mensagem aprovada"
                  MessageRecord.STATUS_REJEITADO -> "Mensagem rejeitada"
                  else -> "Mensagem marcada como pendente"
                },
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
      _state.update { it.copy(busy = true, message = "Aprovando ${pending.size} mensagens…") }
      var success = 0
      var failed = 0
      pending.forEach { record ->
        runCatching { container.client.updateMessageStatus(record.id, MessageRecord.STATUS_APROVADO) }
          .onSuccess {
            success++
            _state.update { state ->
              state.copy(
                all = state.all.map {
                  if (it.id == record.id) it.copy(status = MessageRecord.STATUS_APROVADO) else it
                }
              )
            }
          }
          .onFailure { failed++ }
      }
      _state.update {
        it.copy(
          busy = false,
          message = if (failed > 0) "$success aprovadas, $failed falha(s)" else "$success mensagens aprovadas!",
        )
      }
    }
  }

  fun consumeMessage() = _state.update { it.copy(message = null) }
}
