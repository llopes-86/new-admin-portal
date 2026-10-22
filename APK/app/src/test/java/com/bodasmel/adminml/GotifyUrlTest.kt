package com.bodasmel.adminml

import com.bodasmel.adminml.push.GotifyUrl
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Normalização do endereço do Gotify.
 *
 * Regressão: o campo já vinha preenchido e o usuário colava a mesma URL por cima,
 * resultando em "https://hosthttps://host" -> "unable to resolve host".
 */
class GotifyUrlTest {

  @Test
  fun `mantem a url correta`() {
    assertEquals(
      "https://websites-gotify.qlnxj1.easypanel.host",
      GotifyUrl.sanitize("https://websites-gotify.qlnxj1.easypanel.host"),
    )
  }

  @Test
  fun `acrescenta https quando falta o esquema`() {
    assertEquals(
      "https://gotify.exemplo.com",
      GotifyUrl.sanitize("gotify.exemplo.com"),
    )
  }

  @Test
  fun `corrige esquema duplicado (url colada por cima do campo)`() {
    assertEquals(
      "https://websites-gotify.qlnxj1.easypanel.host",
      GotifyUrl.sanitize(
        "https://websites-gotify.qlnxj1.easypanel.host" +
          "https://websites-gotify.qlnxj1.easypanel.host"
      ),
    )
  }

  @Test
  fun `remove espacos e barra final`() {
    assertEquals(
      "https://gotify.exemplo.com",
      GotifyUrl.sanitize("  https://Gotify.exemplo.com/  "),
    )
  }

  @Test
  fun `preserva o caminho do proxy do gotify`() {
    assertEquals(
      "https://portal.bodasmel.com.br/gotify",
      GotifyUrl.sanitize("https://portal.bodasmel.com.br/gotify/"),
    )
    assertEquals(
      "wss://portal.bodasmel.com.br/gotify/stream?sinceId=1",
      GotifyUrl.webSocketUrl("https://portal.bodasmel.com.br/gotify", "stream", "sinceId=1"),
    )
  }

  @Test
  fun `gera a url de websocket a partir do https`() {
    assertEquals(
      "wss://websites-gotify.qlnxj1.easypanel.host/stream",
      GotifyUrl.webSocketUrl("https://websites-gotify.qlnxj1.easypanel.host", "stream"),
    )
  }
}
