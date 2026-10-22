package com.bodasmel.adminml

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regressão do crash de inicialização (app abria e fechava).
 *
 * Causa original: no `AppContainer`, as stores (`GuestsStore`/`MessagesStore`)
 * eram construídas ANTES do fluxo `rsvpEventFlow` existir, e a corrotina criada
 * no `init` delas acessava a variável ainda nula -> NullPointerException ->
 * o Android encerra o processo.
 *
 * Hoje o fluxo é declarado antes das stores e os observadores só são ligados em
 * `startBackgroundObservers()`, chamado depois que o container está completo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class)
class StartupTest {

  @Test
  fun `Application cria o container sem erro`() {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    assertNull("A inicialização falhou: ${app.initError}", app.initError)
    assertNotNull(app.container)
  }

  @Test
  fun `AppContainer expoe o fluxo de eventos antes das stores`() {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    val container = app.container
    assertNotNull(container.rsvpEventFlow)
    assertNotNull(container.guestsStore)
    assertNotNull(container.messagesStore)
  }

  @Test
  fun `ligar os observadores de fundo nao lanca excecao`() {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    app.container.startBackgroundObservers()
    assertNotNull(app.container.rsvpEventFlow)
  }
}
