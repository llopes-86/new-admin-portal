package com.bodasmel.adminml.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mensagem do Gotify (formato de `/message` e do WebSocket `/stream`). */
@Serializable
data class GotifyMessage(
  val id: Long = 0,
  val appid: Long = 0,
  val message: String = "",
  val title: String = "",
  val priority: Int = 0,
  val date: String = "",
  @SerialName("extras") val extras: Map<String, String>? = null,
)

/** Resposta da criação de client token (`POST /client`). */
@Serializable
data class GotifyClientCreated(
  val id: Long = 0,
  val token: String = "",
  val name: String = "",
)

/** Resultado do diagnóstico de conexão com o Gotify. */
sealed interface GotifyDiagnosis {
  data class Ok(val version: String) : GotifyDiagnosis
  data class Error(val message: String) : GotifyDiagnosis
}
