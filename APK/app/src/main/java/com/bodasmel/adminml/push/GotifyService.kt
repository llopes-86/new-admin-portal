package com.bodasmel.adminml.push

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.bodasmel.adminml.AdminMlApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.WebSocket

/**
 * Foreground Service que mantém a conexão com o Gotify e gera a notificação
 * de cada novo RSVP — inclusive com o app fechado.
 *
 * Usa `specialUse` para não sofrer o limite de 6 h/dia do tipo `dataSync`
 * no Android 15+.
 */
class GotifyService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var webSocket: WebSocket? = null
  private var reconnectJob: Job? = null
  private var retryDelayMs = 5_000L
  private var connected = false

  private val gotifyClient = GotifyClient()

  override fun onCreate() {
    super.onCreate()
    Notifications.createChannels(this)
    startForegroundCompat(connected = false)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }
    connect()
    return START_STICKY
  }

  override fun onDestroy() {
    reconnectJob?.cancel()
    webSocket?.close(1000, "serviço encerrado")
    webSocket = null
    scope.cancel()
    super.onDestroy()
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    // Android pode matar o serviço quando o usuário remove a task: reagenda.
    reconnectJob?.cancel()
    reconnectJob =
      scope.launch {
        delay(3_000)
        connect()
      }
    super.onTaskRemoved(rootIntent)
  }

  private fun startForegroundCompat(connected: Boolean) {
    val notification = Notifications.buildServiceNotification(this, connected)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        Notifications.NOTIFICATION_ID_SERVICE,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(Notifications.NOTIFICATION_ID_SERVICE, notification)
    }
  }

  private fun connect() {
    val container = (application as AdminMlApp).container
    val gotifyUrl = container.settings.value.gotifyUrl
    val clientToken = container.secureStore.gotifyClientToken

    if (gotifyUrl.isBlank() || clientToken.isNullOrBlank()) {
      // Sem configuração do Gotify: encerra o serviço em vez de ficar rodando à toa.
      stopSelf()
      return
    }

    webSocket?.cancel()
    webSocket =
      gotifyClient.openStream(
        baseUrl = gotifyUrl,
        clientToken = clientToken,
        sinceId = container.secureStore.gotifyLastMessageId,
        onOpen = {
          connected = true
          retryDelayMs = 5_000L
          startForegroundCompat(connected = true)
        },
        onMessage = { message -> handleMessage(message) },
        onFailure = {
          connected = false
          startForegroundCompat(connected = false)
          scheduleReconnect()
        },
      )
  }

  private fun handleMessage(message: GotifyMessage) {
    val container = (application as AdminMlApp).container
    val lastId = container.secureStore.gotifyLastMessageId
    if (message.id <= lastId) return // dedupe (reconexão reenvia o histórico)
    container.secureStore.gotifyLastMessageId = message.id

    if (message.title.contains("RSVP", ignoreCase = true) ||
      message.message.contains("Nome:", ignoreCase = true)
    ) {
      Notifications.showRsvpNotification(this, message)
      container.rsvpEvents.tryEmit(message)
    }
  }

  private fun scheduleReconnect() {
    reconnectJob?.cancel()
    reconnectJob =
      scope.launch {
        delay(retryDelayMs)
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(5 * 60_000L)
        connect()
      }
  }

  companion object {
    const val ACTION_STOP = "com.bodasmel.adminml.action.STOP_GOTIFY"

    /** Inicia o monitoramento (idempotente). */
    fun start(context: android.content.Context) {
      val intent = Intent(context, GotifyService::class.java)
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      }
    }

    fun stop(context: android.content.Context) {
      runCatching {
        context.stopService(Intent(context, GotifyService::class.java))
      }
    }
  }
}
