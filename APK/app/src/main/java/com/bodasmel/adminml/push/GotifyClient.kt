package com.bodasmel.adminml.push

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Cliente do Gotify: cria *client token* e abre o WebSocket `/stream`
 * (o endpoint `/stream` do Gotify é WebSocket, não SSE).
 */
class GotifyClient {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
  private val jsonMedia = "application/json; charset=utf-8".toMediaType()

  private val http =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(0, TimeUnit.MILLISECONDS) // conexão longa
      .writeTimeout(20, TimeUnit.SECONDS)
      .pingInterval(30, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()

  private val shortHttp =
    http.newBuilder().readTimeout(30, TimeUnit.SECONDS).build()

  private fun normalize(baseUrl: String): String = GotifyUrl.base(baseUrl)

  private fun wsUrl(baseUrl: String, path: String, query: String = ""): String =
    GotifyUrl.webSocketUrl(baseUrl, path, query)
  /** Diagnóstico do servidor: distingue DNS, TLS, conexão e HTTP. */
  suspend fun diagnose(baseUrl: String): GotifyDiagnosis =
    withContext(Dispatchers.IO) {
      val url = normalize(baseUrl)
      if (url.isEmpty()) return@withContext GotifyDiagnosis.Error("Endereço do Gotify vazio.")

      val request =
        Request.Builder().url(url + "/version").get().build()

      try {
        shortHttp.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (response.isSuccessful) {
            GotifyDiagnosis.Ok(body.trim().ifEmpty { "sem corpo" })
          } else {
            GotifyDiagnosis.Error("O servidor respondeu HTTP ${response.code}.")
          }
        }
      } catch (error: Throwable) {
        GotifyDiagnosis.Error(GotifyUrl.describeFailure(url, error))
      }
    }

  /** Cria um client token usando as credenciais do usuário Gotify (basic auth). */
  suspend fun createClientToken(
    baseUrl: String,
    user: String,
    password: String,
    clientName: String = "Admin M&L Android",
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload = JsonObject(mapOf("name" to kotlinx.serialization.json.JsonPrimitive(clientName)))
        val request =
          Request.Builder()
            .url(normalize(baseUrl) + "/client")
            .header("Authorization", Credentials.basic(user, password))
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()
        shortHttp.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            val message =
              when (response.code) {
                401, 403 -> "Usuário ou senha do Gotify incorretos."
                else ->
                  runCatching {
                      json.parseToJsonElement(body).let { it as? kotlinx.serialization.json.JsonObject }
                        ?.get("errorDescription")
                        ?.toString()
                        ?.trim('"')
                    }
                    .getOrNull()
              }
            throw GotifyException(
              response.code,
              message ?: "Não foi possível conectar ao Gotify (HTTP ${response.code}).",
            )
          }
          json.decodeFromString(GotifyClientCreated.serializer(), body).token
        }
      }
    }

  /** Histórico de mensagens (usado no primeiro connect para não perder avisos). */
  suspend fun fetchMessages(
    baseUrl: String,
    clientToken: String,
    sinceId: Long,
    limit: Int = 50,
  ): Result<List<GotifyMessage>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request =
          Request.Builder()
            .url(normalize(baseUrl) + "/message?sinceId=$sinceId&limit=$limit")
            .header("X-Gotify-Key", clientToken)
            .get()
            .build()
        shortHttp.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw GotifyException(response.code, "Falha ao consultar mensagens do Gotify.")
          }
          val body = response.body?.string().orEmpty()
          val element = json.parseToJsonElement(body)
          val obj = element as? kotlinx.serialization.json.JsonObject
          val items = obj?.get("messages") as? kotlinx.serialization.json.JsonArray
          items
            ?.mapNotNull { item ->
              runCatching {
                  json.decodeFromJsonElement(GotifyMessage.serializer(), item)
                }
                .getOrNull()
            }
            .orEmpty()
        }
      }
    }

  /** Abre o WebSocket de mensagens. `onMessage` recebe cada mensagem push. */
  fun openStream(
    baseUrl: String,
    clientToken: String,
    sinceId: Long,
    onMessage: (GotifyMessage) -> Unit,
    onFailure: (Throwable) -> Unit,
    onOpen: () -> Unit,
  ): WebSocket {
    val request =
      Request.Builder()
        .url(wsUrl(baseUrl, "/stream", "sinceId=$sinceId"))
        .header("X-Gotify-Key", clientToken)
        .build()
    return http.newWebSocket(
      request,
      object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) = onOpen()

        override fun onMessage(webSocket: WebSocket, text: String) {
          runCatching { json.decodeFromString(GotifyMessage.serializer(), text) }
            .onSuccess(onMessage)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onFailure(t)
      },
    )
  }
}

class GotifyException(val code: Int, override val message: String) : Exception(message)
