package com.bodasmel.adminml.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRecord(
  val id: String,
  val email: String = "",
  val name: String? = null,
  @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)

@Serializable
data class AuthResponse(val token: String, val record: UserRecord)

@Serializable
data class ListResponse<T>(
  val page: Int = 1,
  val perPage: Int = 30,
  val totalItems: Int = 0,
  val totalPages: Int = 1,
  val items: List<T> = emptyList(),
)

/** Registro da coleção `rsvp`. */
@Serializable
data class RsvpRecord(
  val id: String,
  @SerialName("sheet_row_id") val sheetRowId: Double? = null,
  val nome: String? = null,
  val adulto: String? = null,
  val criancas: String? = null,
  val responsavel: String? = null,
  @SerialName("nome_crianca") val nomeCrianca: String? = null,
  val idade: String? = null,
  val restricoes: String? = null,
  val contato: String? = null,
  val status: String? = null,
  val created: String? = null,
  val updated: String? = null,
) {
  /** O portal normaliza o status legado 'Validado' para 'Confirmado'. */
  val normalizedStatus: String
    get() = if (status == "Validado") "Confirmado" else (status ?: STATUS_PENDENTE)

  val isPendente: Boolean get() = normalizedStatus == STATUS_PENDENTE

  companion object {
    const val STATUS_PENDENTE = "Pendente"
    const val STATUS_CONFIRMADO = "Confirmado"
  }
}

/** Registro da coleção `messages`. */
@Serializable
data class MessageRecord(
  val id: String,
  @SerialName("sheet_row_id") val sheetRowId: Double? = null,
  val nome: String? = null,
  val mensagem: String? = null,
  val status: String? = null,
  val created: String? = null,
) {
  val normalizedStatus: String get() = status ?: STATUS_PENDENTE

  companion object {
    const val STATUS_PENDENTE = "Pendente"
    const val STATUS_APROVADO = "Aprovado"
    const val STATUS_REJEITADO = "Rejeitado"
  }
}

/** Erro de API do PocketBase (status HTTP + mensagem do servidor). */
class ApiException(val code: Int, override val message: String) : Exception(message)
