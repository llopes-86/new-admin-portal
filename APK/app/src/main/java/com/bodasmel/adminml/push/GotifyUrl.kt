package com.bodasmel.adminml.push

import java.util.Locale

/**
 * Normalização do endereço do Gotify.
 *
 * O campo de URL já vem preenchido e é comum colar a mesma URL por cima
 * ("https://host" + "https://host" = endereço inválido -> "unable to resolve host").
 * Aqui o endereço é normalizado para sempre funcionar.
 */
object GotifyUrl {

  /**
   * Resultado da normalização: `https://host` ou `https://host/caminho`.
   * Remove espaços, esquema duplicado, query/fragment e barra final.
   */
  fun sanitize(raw: String): String {
    var value = raw.trim().replace(Regex("\\s+"), "")
    if (value.isEmpty()) return ""

    // Esquema duplicado (várias ocorrências de "://"): fica com o último
    val lastScheme = value.lastIndexOf("://")
    if (lastScheme > 0) {
      value = "https://" + value.substring(lastScheme + 3)
    }

    val scheme = if (value.startsWith("http://")) "http://" else "https://"
    val rest = value.substringAfter("://")

    val withoutQuery = rest.substringBefore("?").substringBefore("#")
    val host = withoutQuery.substringBefore("/").lowercase(Locale.ROOT)
    if (host.isEmpty()) return ""

    val rawPath = withoutQuery.substringAfter("/", "")
    val path = if (rawPath.isEmpty()) "" else "/" + rawPath.trim('/')

    return scheme + host + path
  }

  /** URL base normalizada (sem barra final). */
  fun base(raw: String): String = sanitize(raw).trimEnd('/')

  /**
   * URL de WebSocket derivada da URL base (https -> wss, http -> ws).
   * Preserva o caminho base (ex.: https://host/gotify -> wss://host/gotify/stream).
   */
  fun webSocketUrl(raw: String, endpoint: String, query: String = ""): String {
    val base = sanitize(raw)
    val scheme = if (base.startsWith("https://")) "wss://" else "ws://"
    val rest = base.substringAfter("://")
    val endpointPath = "/" + endpoint.trim('/')
    return "$scheme$rest$endpointPath" + if (query.isEmpty()) "" else "?$query"
  }

  /** Mensagem amigável para o erro de conexão, com o endereço efetivamente usado. */
  fun describeFailure(usedUrl: String, error: Throwable): String {
    val host = runCatching { base(usedUrl).substringAfter("://") }.getOrDefault(usedUrl)
    val cause = generateSequence(error) { it.cause }.last()
    return when {
      cause is java.net.UnknownHostException ->
        "Não foi possível resolver \"$host\". Confira o endereço e a sua conexão (Wi-Fi ou dados)."
      cause is javax.net.ssl.SSLException ->
        "Problema de certificado HTTPS em \"$host\". Use um endereço com https válido."
      cause is java.net.ConnectException ->
        "Conexão recusada por \"$host\". O servidor Gotify pode estar fora do ar."
      cause is java.net.SocketTimeoutException ->
        "Tempo esgotado ao falar com \"$host\"."
      else -> "Falha ao conectar no Gotify: ${cause.message ?: cause.javaClass.simpleName}"
    }
  }
}
