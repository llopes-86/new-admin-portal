package com.bodasmel.adminml.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.ChangePasswordResult
import com.bodasmel.adminml.core.LoginResult
import com.bodasmel.adminml.core.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginMode { LOGIN, CHANGE_PASSWORD }

data class LoginUiState(
  val mode: LoginMode = LoginMode.LOGIN,
  val email: String = "",
  val password: String = "",
  val currentPassword: String = "",
  val newPassword: String = "",
  val confirmPassword: String = "",
  val showPassword: Boolean = false,
  val rememberCredentials: Boolean = true,
  val loading: Boolean = false,
  val error: String? = null,
  /** Contador de erros — usado para animar o "shake" dos campos. */
  val errorCount: Int = 0,
)

class LoginViewModel(private val session: SessionManager) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      LoginUiState(
        // Pré-preenche com o que ficou salvo da última vez
        email = session.rememberedEmail.orEmpty(),
        password = session.rememberedPassword.orEmpty(),
        rememberCredentials = session.rememberCredentials,
      )
    )
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  init {
    // Sessão retomada com troca de senha pendente: abre direto no formulário.
    if (session.state.value is AuthState.NeedsPasswordChange) {
      _uiState.update { it.copy(mode = LoginMode.CHANGE_PASSWORD) }
    }
  }

  fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

  fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

  fun onCurrentPasswordChange(value: String) =
    _uiState.update { it.copy(currentPassword = value, error = null) }

  fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, error = null) }

  fun onConfirmPasswordChange(value: String) =
    _uiState.update { it.copy(confirmPassword = value, error = null) }

  fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }

  /** Switch "Lembrar-me". */
  fun onRememberChange(value: Boolean) {
    session.setRememberCredentials(value)
    _uiState.update { it.copy(rememberCredentials = value) }
  }

  fun submitLogin() {
    val state = _uiState.value
    if (state.loading) return
    if (state.email.isBlank() || state.password.isBlank()) {
      setError("Informe e-mail e senha.")
      return
    }
    _uiState.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
      when (val result = session.login(state.email, state.password, state.rememberCredentials)) {
        is LoginResult.Success -> _uiState.update { it.copy(loading = false) }
        is LoginResult.NeedsPasswordChange -> {
          _uiState.update {
            it.copy(
              loading = false,
              mode = LoginMode.CHANGE_PASSWORD,
              currentPassword = it.password,
              password = "",
              error = null,
            )
          }
        }
        is LoginResult.Error -> setError(result.message)
      }
    }
  }

  fun submitChangePassword() {
    val state = _uiState.value
    if (state.loading) return
    when {
      state.newPassword.length < 8 -> {
        setError("A nova senha precisa ter no mínimo 8 caracteres.")
        return
      }
      state.newPassword != state.confirmPassword -> {
        setError("As novas senhas não conferem. Repita a nova senha nos dois campos.")
        return
      }
      state.currentPassword == state.newPassword -> {
        setError("Crie uma senha diferente da atual.")
        return
      }
    }
    _uiState.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
      when (val result = session.changePassword(state.currentPassword, state.newPassword)) {
        is ChangePasswordResult.Success -> _uiState.update { it.copy(loading = false) }
        is ChangePasswordResult.Error -> setError(result.message)
      }
    }
  }

  fun backToLogin() {
    _uiState.update {
      it.copy(
        mode = LoginMode.LOGIN,
        error = null,
        currentPassword = "",
        newPassword = "",
        confirmPassword = "",
      )
    }
  }

  private fun setError(message: String) {
    _uiState.update { it.copy(loading = false, error = message, errorCount = it.errorCount + 1) }
  }
}
