package com.bodasmel.adminml

import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AuthApi
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.SecureStore
import com.bodasmel.adminml.core.SessionLock
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.data.AuthResponse
import com.bodasmel.adminml.data.UserRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Bloqueio automático ao sair do app e logout por inatividade.
 *
 * Tempos curtos injetados no construtor para não esperar 1,5 s / 15 min.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionLockTest {

  private val lockDelayMs = 100L
  private val logoutDelayMs = 1_000L
  private val token = "token-123"

  private val user =
    UserRecord(id = "user-1", email = "admin@bodasmel.com.br", name = "Admin")

  private lateinit var store: SecureStore
  private lateinit var session: SessionManager
  private lateinit var dispatcher: TestDispatcher
  private lateinit var sessionLock: SessionLock

  private val fakeApi =
    object : AuthApi {
      override suspend fun authWithPassword(email: String, password: String): AuthResponse =
        AuthResponse(token = token, record = user)

      override suspend fun changePassword(
        userId: String,
        oldPassword: String,
        newPassword: String,
      ): UserRecord = user
    }

  @Before
  fun setUp() {
    store = SecureStore(ApplicationProvider.getApplicationContext())
    session = SessionManager(store, fakeApi)
    dispatcher = StandardTestDispatcher()
    sessionLock = SessionLock(session, CoroutineScope(dispatcher), lockDelayMs, logoutDelayMs)
    runBlocking { session.login(user.email, "senha123") }
  }

  @Test
  fun `bloqueia depois de sair e desloga por inatividade`() {
    assertTrue(session.state.value is AuthState.SignedIn)

    sessionLock.onBackground()
    dispatcher.scheduler.advanceTimeBy(lockDelayMs + 1)
    dispatcher.scheduler.runCurrent()

    assertTrue("Esperava a sessão bloqueada", session.state.value is AuthState.Locked)
    assertEquals("O bloqueio não pode apagar o token", token, store.pbToken)

    dispatcher.scheduler.advanceTimeBy(logoutDelayMs + 1)
    dispatcher.scheduler.runCurrent()

    assertEquals(AuthState.SignedOut, session.state.value)
    assertNull("O logout por inatividade deve limpar o token", store.pbToken)
  }

  @Test
  fun `voltar antes do delay cancela bloqueio e logout`() {
    sessionLock.onBackground()
    dispatcher.scheduler.advanceTimeBy(lockDelayMs / 2)
    dispatcher.scheduler.runCurrent()

    sessionLock.onForeground()

    dispatcher.scheduler.advanceTimeBy(logoutDelayMs * 2)
    dispatcher.scheduler.runCurrent()

    assertTrue("Voltar ao app não deve bloquear", session.state.value is AuthState.SignedIn)
    assertEquals(token, store.pbToken)
  }

  @Test
  fun `sair duas vezes nao duplica os temporizadores`() {
    sessionLock.onBackground()
    sessionLock.onBackground()

    dispatcher.scheduler.advanceTimeBy(lockDelayMs + 1)
    dispatcher.scheduler.runCurrent()
    assertTrue(session.state.value is AuthState.Locked)

    sessionLock.onForeground()
    dispatcher.scheduler.advanceTimeBy(logoutDelayMs * 2)
    dispatcher.scheduler.runCurrent()

    assertTrue("O logout agendado em dobro deveria ter sido cancelado", session.state.value is AuthState.Locked)
  }

  @Test
  fun `sair de novo depois do bloqueio nao reinicia o logout`() {
    sessionLock.onBackground()
    dispatcher.scheduler.advanceTimeBy(lockDelayMs + 1)
    dispatcher.scheduler.runCurrent()
    assertTrue(session.state.value is AuthState.Locked)

    // Já bloqueado: um novo aviso de segundo plano não pode adiar o logout.
    sessionLock.onBackground()
    dispatcher.scheduler.advanceTimeBy(logoutDelayMs - lockDelayMs + 1)
    dispatcher.scheduler.runCurrent()

    assertEquals(AuthState.SignedOut, session.state.value)
  }
}
