package com.bodasmel.adminml

import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.ui.login.LoginMode
import com.bodasmel.adminml.ui.login.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tela de login: credenciais lembradas e validações.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class)
class LoginViewModelTest {

  private fun container() =
    ApplicationProvider.getApplicationContext<AdminMlApp>().container

  @Test
  fun `preenche e-mail e senha lembrados`() {
    val app = container()
    app.secureStore.rememberedEmail = "admin@bodasmel.com.br"
    app.secureStore.rememberedPassword = "senha-secreta"

    val viewModel = LoginViewModel(app.session)
    val state = viewModel.uiState.value

    assertEquals("admin@bodasmel.com.br", state.email)
    assertEquals("senha-secreta", state.password)
    assertTrue(state.rememberCredentials)
  }

  @Test
  fun `desligar o lembrar-me apaga as credenciais guardadas`() {
    val app = container()
    app.secureStore.rememberedEmail = "admin@bodasmel.com.br"
    app.secureStore.rememberedPassword = "senha-secreta"
    app.secureStore.rememberCredentials = true

    val viewModel = LoginViewModel(app.session)
    viewModel.onRememberChange(false)

    assertFalse(viewModel.uiState.value.rememberCredentials)
    assertNull(app.secureStore.rememberedEmail)
    assertNull(app.secureStore.rememberedPassword)
  }

  @Test
  fun `login sem e-mail mostra erro`() {
    val app = container()
    val viewModel = LoginViewModel(app.session)
    viewModel.onEmailChange("")
    viewModel.onPasswordChange("12345678")
    viewModel.submitLogin()

    val state = viewModel.uiState.value
    assertNotNull(state.error)
    assertEquals("Informe e-mail e senha.", state.error)
    assertEquals(1, state.errorCount)
  }

  @Test
  fun `troca de senha exige 8 caracteres`() {
    val app = container()
    val viewModel = LoginViewModel(app.session)
    viewModel.onCurrentPasswordChange("antiga123")
    viewModel.onNewPasswordChange("curta")
    viewModel.onConfirmPasswordChange("curta")
    viewModel.submitChangePassword()

    assertEquals(
      "A nova senha precisa ter no mínimo 8 caracteres.",
      viewModel.uiState.value.error,
    )
  }

  @Test
  fun `troca de senha exige confirmacao igual`() {
    val app = container()
    val viewModel = LoginViewModel(app.session)
    viewModel.onCurrentPasswordChange("antiga123")
    viewModel.onNewPasswordChange("novasenha123")
    viewModel.onConfirmPasswordChange("outrasenha123")
    viewModel.submitChangePassword()

    assertEquals(
      "As novas senhas não conferem. Repita a nova senha nos dois campos.",
      viewModel.uiState.value.error,
    )
  }

  @Test
  fun `modo inicial e login`() {
    val app = container()
    val viewModel = LoginViewModel(app.session)
    assertEquals(LoginMode.LOGIN, viewModel.uiState.value.mode)
  }
}
