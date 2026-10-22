package com.bodasmel.adminml

import com.bodasmel.adminml.push.GotifyClient
import com.bodasmel.adminml.push.GotifyDiagnosis
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Teste de fumaça contra o Gotify de PRODUÇÃO, usando o mesmo código do app.
 * Confirma que a URL funciona e que o handshake do WebSocket /stream chega ao
 * servidor (que deve recusar por falta de token válido).
 */
class GotifyProductionSmokeTest {

  private val url = "https://websites-gotify.qlnxj1.easypanel.host"

  @Test
  fun `servidor gotify de producao responde`() = runBlocking {
    val result = GotifyClient().diagnose(url)
    println("DIAGNÓSTICO: $result")
    assertTrue(
      "Esperava Ok, veio $result",
      result is GotifyDiagnosis.Ok,
    )
  }

  @Test
  fun `websocket do stream chega ao servidor`() {
    val http = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    val latch = CountDownLatch(1)
    var status: Int? = null
    var error: Throwable? = null

    val request =
      Request.Builder()
        .url("wss://websites-gotify.qlnxj1.easypanel.host/stream?sinceId=0")
        .header("X-Gotify-Key", "token-invalido-de-teste")
        .build()

    http.newWebSocket(
      request,
      object : WebSocketListener() {
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          status = response?.code
          error = t
          latch.countDown()
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
          status = response.code
          webSocket.close(1000, "teste")
          latch.countDown()
        }
      },
    )

    assertTrue("O handshake não retornou em 15s", latch.await(15, TimeUnit.SECONDS))
    println("WEBSOCKET -> status=$status erro=$error")

    // O servidor deve recusar (401) — isso prova que chegamos nele.
    assertNotNull(status)
    assertEquals("O servidor recusou o handshake como esperado", 401, status)
  }
}
