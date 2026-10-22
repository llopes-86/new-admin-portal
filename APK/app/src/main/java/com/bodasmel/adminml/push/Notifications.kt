package com.bodasmel.adminml.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bodasmel.adminml.MainActivity
import com.bodasmel.adminml.R
import com.bodasmel.adminml.theme.PortalColors

/** Canais e construção das notificações do app. */
object Notifications {

  const val CHANNEL_RSVP = "rsvp_confirmacoes"
  const val CHANNEL_SERVICE = "monitoramento"

  const val NOTIFICATION_ID_RSVP = 1001
  const val NOTIFICATION_ID_SERVICE = 1002

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val rsvp =
      NotificationChannel(
        CHANNEL_RSVP,
        "Confirmações de presença",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Avisa quando alguém confirma presença no site de RSVP."
        enableVibration(true)
      }

    val service =
      NotificationChannel(
        CHANNEL_SERVICE,
        "Monitoramento",
        NotificationManager.IMPORTANCE_MIN,
      ).apply {
        description = "Notificação persistente do serviço que monitora novos RSVPs."
        setShowBadge(false)
      }

    manager.createNotificationChannel(rsvp)
    manager.createNotificationChannel(service)
  }

  /** Notificação persistente do Foreground Service (card compacto, uma linha). */
  fun buildServiceNotification(context: Context, connected: Boolean): Notification {
    val contentIntent =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(context, CHANNEL_SERVICE)
      .setSmallIcon(R.drawable.ic_stat_rsvp)
      .setContentTitle("Monitorando")
      .setContentText(if (connected) "Conectado" else "Conectando…")
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setOngoing(true)
      .setShowWhen(false)
      .setSilent(true)
      .setOnlyAlertOnce(true)
      .setLocalOnly(true)
      .setContentIntent(contentIntent)
      .build()
  }

  /** Notificação de novo RSVP (card compacto: uma linha, sem expansão). */
  fun showRsvpNotification(context: Context, message: GotifyMessage) {
    val title = message.title.ifBlank { "Novo RSVP" }
    val body =
      message.message.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "Novo RSVP recebido." }

    val contentIntent =
      PendingIntent.getActivity(
        context,
        message.id.toInt(),
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(MainActivity.EXTRA_OPEN_TAB, "guests")
        },
        PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_RSVP)
        .setSmallIcon(R.drawable.ic_stat_rsvp)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setColor(PortalColors.SavannaGreen.value.toInt())
        .setContentIntent(contentIntent)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RSVP, notification)
    }
  }
}
