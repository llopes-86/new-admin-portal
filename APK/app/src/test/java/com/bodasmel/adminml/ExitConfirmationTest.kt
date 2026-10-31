package com.bodasmel.adminml

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AuthApi
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.SecureStore
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.data.ApiException
import com.bodasmel.adminml.data.AuthResponse
import com.bodasmel.adminml.data.UserRecord
import com.bodasmel.adminml.theme.AdminMLTheme
import com.bodasmel.adminml.ui.home.HomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class, qualifiers = "w393dp-h851dp-xhdpi")
class ExitConfirmationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val email = "admin@bodasmel.com.br"

  private fun signedInSession(): SessionManager {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    val session = SessionManager(SecureStore(app), FakeAuthApi())
    runBlocking { session.login(email, "senha123") }
    return session
  }

  private fun setHome(session: SessionManager) {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    composeRule.setContent {
      AdminMLTheme {
        HomeScreen(
          container = app.container,
          onRequestNotificationPermission = {},
          session = session,
        )
      }
    }
  }

  private fun pressBack() = composeRule.runOnIdle {
    composeRule.activity.onBackPressedDispatcher.onBackPressed()
  }

  @Test
  fun `voltar na raiz mostra dialogo de saida`() {
    val session = signedInSession()
    setHome(session)

    composeRule.onNodeWithText("Lista de convidados").assertIsDisplayed()
    pressBack()

    composeRule.onNodeWithText("Sair do aplicativo").assertIsDisplayed()
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  @Test
  fun `cancelar a saida mantem a sessao`() {
    val session = signedInSession()
    setHome(session)

    pressBack()
    composeRule.onNodeWithText("Cancelar").performClick()

    composeRule.onNodeWithText("Sair do aplicativo").assertDoesNotExist()
    assertTrue(session.state.value is AuthState.SignedIn)
    assertFalse(composeRule.activity.isFinishing)
  }

  @Test
  fun `confirmar a saida encerra sessao e activity`() {
    val session = signedInSession()
    setHome(session)

    pressBack()
    composeRule.onNodeWithText("Sair do app").performClick()

    composeRule.waitForIdle()
    assertEquals(AuthState.SignedOut, session.state.value)
    assertTrue(composeRule.activity.isFinishing)
  }

  @Test
  fun `voltar em ajustes retorna ao painel`() {
    val session = signedInSession()
    setHome(session)

    composeRule.onNodeWithContentDescription("Ajustes").performClick()
    composeRule.onNodeWithText("Conexões").assertIsDisplayed()

    pressBack()

    composeRule.onNodeWithText("Conexões").assertDoesNotExist()
    composeRule.onNodeWithText("Lista de convidados").assertIsDisplayed()
    assertTrue(session.state.value is AuthState.SignedIn)
  }

  private class FakeAuthApi : AuthApi {
    override suspend fun authWithPassword(email: String, password: String): AuthResponse {
      if (password != "senha123") throw ApiException(400, "invalid credentials")
      return AuthResponse("token", UserRecord(id = "user-1", email = email.trim()))
    }

    override suspend fun changePassword(
      userId: String,
      oldPassword: String,
      newPassword: String,
    ): UserRecord = UserRecord(id = userId, email = "admin@bodasmel.com.br")
  }
}
