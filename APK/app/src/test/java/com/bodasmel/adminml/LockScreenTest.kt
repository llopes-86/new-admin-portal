package com.bodasmel.adminml

import android.app.Activity
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AuthApi
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.BiometricGate
import com.bodasmel.adminml.core.BiometricStatus
import com.bodasmel.adminml.core.SecureStore
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.data.ApiException
import com.bodasmel.adminml.data.AuthResponse
import com.bodasmel.adminml.data.UserRecord
import com.bodasmel.adminml.theme.AdminMLTheme
import com.bodasmel.adminml.ui.lock.LockScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class, qualifiers = "w393dp-h851dp-xhdpi")
class LockScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val email = "admin@bodasmel.com.br"
  private val expectedPassword = "senha123"

  private fun lockedSession(): SessionManager {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    val session = SessionManager(SecureStore(app), FakeAuthApi(expectedPassword))
    runBlocking { session.login(email, expectedPassword) }
    session.lock()
    return session
  }

  @Test
  fun `mostra titulo e e-mail da sessao bloqueada`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.NO_HARDWARE)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.onNodeWithText("Sessão bloqueada").assertIsDisplayed()
    composeRule.onNodeWithText(email).assertIsDisplayed()
  }

  @Test
  fun `senha correta desbloqueia a sessao`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.NO_HARDWARE)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.onNode(hasSetTextAction()).performTextInput(expectedPassword)
    composeRule.onNodeWithText("Desbloquear").performClick()

    composeRule.waitForIdle()
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  @Test
  fun `senha incorreta mostra erro e mantem bloqueado`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.NO_HARDWARE)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.onNode(hasSetTextAction()).performTextInput("senha-errada")
    composeRule.onNodeWithText("Desbloquear").performClick()

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Senha incorreta. Tente novamente.").assertIsDisplayed()
    assertTrue(session.state.value is AuthState.Locked)
  }

  @Test
  fun `sair da conta encerra a sessao`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.NO_HARDWARE)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.onNodeWithText("Sair da conta").performClick()

    composeRule.waitForIdle()
    assertEquals(AuthState.SignedOut, session.state.value)
  }

  @Test
  fun `biometria disponivel dispara o prompt e desbloqueia`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Success)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.waitUntil(5_000) { gate.authenticateCalls > 0 }
    composeRule.waitForIdle()

    assertEquals(1, gate.authenticateCalls)
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  @Test
  fun `cancelar mantem modo biometria e oferece nova tentativa`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Cancel)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tentar biometria novamente").assertIsDisplayed()
    composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    assertTrue(session.state.value is AuthState.Locked)
  }

  @Test
  fun `tentar novamente chama o gate outra vez`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Cancel)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitForIdle()

    gate.outcome = Outcome.Success
    composeRule.onNodeWithText("Tentar biometria novamente").performClick()

    composeRule.waitUntil(5_000) { gate.authenticateCalls == 2 }
    composeRule.waitForIdle()
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  @Test
  fun `usar senha troca para o campo de senha`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Cancel)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Usar senha").performClick()
    composeRule.waitForIdle()

    composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
    composeRule.onNodeWithText("Usar biometria").assertIsDisplayed()
    assertTrue(session.state.value is AuthState.Locked)
  }

  @Test
  fun `sem biometria cadastrada vai para senha com explicacao`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.NONE_ENROLLED)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
    composeRule.onNodeWithText("Cadastrar biometria").assertIsDisplayed()
    assertEquals(0, gate.authenticateCalls)
  }

  @Test
  fun `biometria indisponivel permanece na biometria com retry`() {
    val session = lockedSession()
    val gate = FakeBiometricGate(BiometricStatus.UNAVAILABLE)
    composeRule.setContent { AdminMLTheme { LockScreen(session = session, gate = gate) } }

    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tentar biometria novamente").assertIsDisplayed()
    composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    assertEquals(0, gate.authenticateCalls)
  }

  private enum class Outcome { Success, Cancel }

  private class FakeBiometricGate(
    private val status: BiometricStatus,
    var outcome: Outcome = Outcome.Cancel,
  ) : BiometricGate {
    var authenticateCalls = 0

    override fun status(context: Context): BiometricStatus = status

    override fun authenticate(
      activity: Activity,
      title: String,
      subtitle: String?,
      negativeButton: String,
      onSuccess: () -> Unit,
      onError: (String) -> Unit,
      onCancel: () -> Unit,
    ) {
      authenticateCalls++
      when (outcome) {
        Outcome.Success -> onSuccess()
        Outcome.Cancel -> onCancel()
      }
    }
  }

  private class FakeAuthApi(private val expectedPassword: String) : AuthApi {
    override suspend fun authWithPassword(email: String, password: String): AuthResponse {
      if (password != expectedPassword) throw ApiException(400, "invalid credentials")
      return AuthResponse("token", UserRecord(id = "user-1", email = email.trim()))
    }

    override suspend fun changePassword(
      userId: String,
      oldPassword: String,
      newPassword: String,
    ): UserRecord = UserRecord(id = userId, email = "admin@bodasmel.com.br")
  }
}
