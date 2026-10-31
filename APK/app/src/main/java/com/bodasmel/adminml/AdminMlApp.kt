package com.bodasmel.adminml

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.bodasmel.adminml.core.AppSettings
import com.bodasmel.adminml.core.SecureStore
import com.bodasmel.adminml.core.SessionLock
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.core.SettingsStore
import com.bodasmel.adminml.data.GuestsStore
import com.bodasmel.adminml.data.MessagesStore
import com.bodasmel.adminml.data.PocketBaseClient
import com.bodasmel.adminml.push.GotifyMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Service locator simples — o app tem um único container.
 *
 * ORDEM DE INICIALIZAÇÃO IMPORTA: os fluxos compartilhados (rsvpEventFlow) precisam
 * existir ANTES das stores, porque o `init` delas já observa esses fluxos.
 */
class AppContainer(application: Application) {

  val secureStore = SecureStore(application)
  val settingsStore = SettingsStore(application)

  /**
   * Escopo de vida do app. O [CoroutineExceptionHandler] evita que uma exceção
   * em uma tarefa de fundo mate o aplicativo (no Android isso encerra o processo).
   */
  val appScope: CoroutineScope =
    CoroutineScope(
      SupervisorJob() +
        Dispatchers.Default +
        CoroutineExceptionHandler { _, throwable ->
          Log.e("AdminML", "Erro em tarefa de fundo", throwable)
        }
    )

  val settings: StateFlow<AppSettings> =
    settingsStore.settings.stateIn(appScope, SharingStarted.Eagerly, AppSettings())

  val client =
    PocketBaseClient(
      baseUrlProvider = { settings.value.pbBaseUrl },
      tokenProvider = { secureStore.pbToken },
    )

  val session = SessionManager(secureStore, client)

  /** Bloqueia a sessão em segundo plano e desloga após inatividade. */
  val sessionLock = SessionLock(session, appScope)

  /** Eventos de novo RSVP originados pelo WebSocket do Gotify. */
  val rsvpEvents = MutableSharedFlow<GotifyMessage>(extraBufferCapacity = 16)
  val rsvpEventFlow: SharedFlow<GotifyMessage> = rsvpEvents

  /** Estado compartilhado das listagens (telas + Exportar usam a mesma seleção). */
  val guestsStore = GuestsStore(this)
  val messagesStore = MessagesStore(this)

  /**
   * Liga os observadores de fundo. Só pode ser chamado DEPOIS que o container
   * estiver completo — no `init` das stores o fluxo `rsvpEventFlow` ainda não
   * existia e causava NullPointerException (app abria e fechava).
   */
  fun startBackgroundObservers() {
    guestsStore.observeRsvpEvents()
    messagesStore.observeRsvpEvents()
  }
}

class AdminMlApp : Application() {

  lateinit var container: AppContainer
    private set

  /** Se a inicialização falhar, guardamos o erro para exibir na tela (em vez de crash). */
  var initError: String? = null
    private set

  private var startedActivities = 0

  override fun onCreate() {
    super.onCreate()

    // Log de qualquer exceção não tratada (útil via `adb logcat -s AdminML`)
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      Log.e("AdminML", "Exceção não tratada em ${thread.name}", error)
      previous?.uncaughtException(thread, error)
    }

    runCatching {
        container = AppContainer(this)
        container.startBackgroundObservers()
      }
      .onSuccess {
        // A sessão nunca sobrevive ao processo: o app sempre começa no login.
        container.secureStore.clearSession()
        container.secureStore.clearLegacyRememberedPassword()
        registerSessionLock()
      }
      .onFailure { error ->
        initError = error.message ?: error.javaClass.simpleName
        Log.e("AdminML", "Falha ao inicializar o container", error)
      }
  }

  private fun registerSessionLock() {
    registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
          startedActivities++
          if (startedActivities == 1) container.sessionLock.onForeground()
        }

        override fun onActivityStopped(activity: Activity) {
          startedActivities = (startedActivities - 1).coerceAtLeast(0)
          if (startedActivities == 0 && !activity.isChangingConfigurations) {
            container.sessionLock.onBackground()
          }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
      }
    )
  }
}
