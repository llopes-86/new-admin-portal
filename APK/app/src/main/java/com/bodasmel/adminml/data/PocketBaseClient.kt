package com.bodasmel.adminml.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP do PocketBase, escrito à mão (5 endpoints) para não depender de
 * SDK de terceiros e evitar drift de versão.
 *
 * Base URL esperado: `https://portal.bodasmel.com.br/api/pb`
 * (o SDK web usava `/api/pb` e o nginx fazia proxy de `/api/pb/api/` -> `pb/api/`).
 */
class PocketBaseClient(
  private val baseUrlProvider: () -> String,
  private val tokenProvider: () -> String?,
) {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
  }

  private val http =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()

  private val jsonMedia = "application/json; charset=utf-8".toMediaType()

  private fun endpoint(path: String) = baseUrlProvider().trimEnd('/') + path

  private fun Request.Builder.auth(): Request.Builder {
    tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", it) }
    return this
  }

  private suspend fun execute(request: Request): String =
    withContext(Dispatchers.IO) {
      http.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val serverMessage =
            runCatching {
              json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull()
          throw ApiException(response.code, serverMessage ?: "Erro ${response.code} ao falar com o servidor")
        }
        body
      }
    }

  private fun jsonBody(payload: JsonObject) = payload.toString().toRequestBody(jsonMedia)

  // ---------------------------------------------------------------- autenticação

  suspend fun authWithPassword(email: String, password: String): AuthResponse {
    val payload = buildJsonObject {
      put("identity", email.trim())
      put("password", password)
    }
    val body =
      execute(
        Request.Builder()
          .url(endpoint("/api/collections/users/auth-with-password"))
          .post(jsonBody(payload))
          .build()
      )
    return json.decodeFromString(AuthResponse.serializer(), body)
  }

  /** Troca de senha + limpeza de `must_change_password` (paridade com o portal). */
  suspend fun changePassword(
    userId: String,
    oldPassword: String,
    newPassword: String,
  ): UserRecord {
    val payload = buildJsonObject {
      put("oldPassword", oldPassword)
      put("password", newPassword)
      put("passwordConfirm", newPassword)
      put("must_change_password", false)
    }
    val body =
      execute(
        Request.Builder()
          .url(endpoint("/api/collections/users/records/$userId"))
          .patch(jsonBody(payload))
          .auth()
          .build()
      )
    return json.decodeFromString(UserRecord.serializer(), body)
  }

  // ---------------------------------------------------------------- leituras

  suspend fun listRsvp(): List<RsvpRecord> = listAll<RsvpRecord>("rsvp")

  suspend fun listMessages(): List<MessageRecord> = listAll<MessageRecord>("messages")

  private suspend inline fun <reified T> listAll(collection: String): List<T> {
    val items = mutableListOf<T>()
    var page = 1
    var totalPages = 1
    do {
      val url =
        endpoint("/api/collections/$collection/records") +
          "?page=$page&perPage=200&sort=-created"
      val body =
        execute(Request.Builder().url(url).get().auth().build())
      val response = json.decodeFromString(ListResponse.serializer(serializer<T>()), body)
      items += response.items
      totalPages = response.totalPages.coerceAtLeast(1)
      page++
    } while (page <= totalPages)
    return items
  }

  // ---------------------------------------------------------------- escritas

  suspend fun updateRsvpStatus(id: String, status: String): RsvpRecord {
    val payload = buildJsonObject { put("status", status) }
    val body =
      execute(
        Request.Builder()
          .url(endpoint("/api/collections/rsvp/records/$id"))
          .patch(jsonBody(payload))
          .auth()
          .build()
      )
    return json.decodeFromString(RsvpRecord.serializer(), body)
  }

  suspend fun updateMessageStatus(id: String, status: String): MessageRecord {
    val payload = buildJsonObject { put("status", status) }
    val body =
      execute(
        Request.Builder()
          .url(endpoint("/api/collections/messages/records/$id"))
          .patch(jsonBody(payload))
          .auth()
          .build()
      )
    return json.decodeFromString(MessageRecord.serializer(), body)
  }

  /** Usado apenas para diagnóstico nas telas de ajustes. */
  suspend fun health(): Boolean =
    runCatching {
      execute(Request.Builder().url(endpoint("/api/health")).get().build())
      true
    }.getOrDefault(false)
}
