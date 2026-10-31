package com.bodasmel.adminml

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AuthApi
import com.bodasmel.adminml.core.AuthState
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
    composeRule.setContent { AdminMLTheme { LockScreen(session = session) } }

    composeRule.onNodeWithText("Sessão bloqueada").assertIsDisplayed()
    composeRule.onNodeWithText(email).assertIsDisplayed()
  }

  @Test
  fun `senha correta desbloqueia a sessao`() {
    val session = lockedSession()
    composeRule.setContent { AdminMLTheme { LockScreen(session = session) } }

    composeRule.onNode(hasSetTextAction()).performTextInput(expectedPassword)
    composeRule.onNodeWithText("Desbloquear").performClick()

    composeRule.waitForIdle()
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  @Test
  fun `senha incorreta mostra erro e mantem bloqueado`() {
    val session = lockedSession()
    composeRule.setContent { AdminMLTheme { LockScreen(session = session) } }

    composeRule.onNode(hasSetTextAction()).performTextInput("senha-errada")
    composeRule.onNodeWithText("Desbloquear").performClick()

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Senha incorreta. Tente novamente.").assertIsDisplayed()
    assertTrue(session.state.value is AuthState.Locked)
  }

  @Test
  fun `sair da conta encerra a sessao`() {
    val session = lockedSession()
    composeRule.setContent { AdminMLTheme { LockScreen(session = session) } }

    composeRule.onNodeWithText("Sair da conta").performClick()

    composeRule.waitForIdle()
    assertEquals(AuthState.SignedOut, session.state.value)
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
