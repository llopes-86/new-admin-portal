package com.bodasmel.adminml.core

import com.bodasmel.adminml.data.ApiException
import com.bodasmel.adminml.data.PocketBaseClient
import com.bodasmel.adminml.data.UserRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Estado de autenticação do app. */
sealed interface AuthState {
  data object SignedOut : AuthState
  data class NeedsPasswordChange(val user: UserRecord) : AuthState
  data class SignedIn(val user: UserRecord) : AuthState
}

sealed interface LoginResult {
  data class Success(val user: UserRecord) : LoginResult
  data class NeedsPasswordChange(val user: UserRecord) : LoginResult
  data class Error(val message: String) : LoginResult
}

sealed interface ChangePasswordResult {
  data class Success(val user: UserRecord) : ChangePasswordResult
  data class Error(val message: String) : ChangePasswordResult
}

/**
 * Sessão do admin no PocketBase.
 * Espelha `public/js/utils/auth.js` + `public/js/login.js` do portal web.
 */
class SessionManager(
  private val secureStore: SecureStore,
  private val client: PocketBaseClient,
) {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  private val _state = MutableStateFlow<AuthState>(restoreSession())
  val state: StateFlow<AuthState> = _state.asStateFlow()

  val currentUser: UserRecord?
    get() =
      when (val current = _state.value) {
        is AuthState.SignedIn -> current.user
        is AuthState.NeedsPasswordChange -> current.user
        AuthState.SignedOut -> null
      }

  /** E-mail e senha lembrados ("Lembrar-me"). */
  val rememberedEmail: String? get() = secureStore.rememberedEmail
  val rememberedPassword: String? get() = secureStore.rememberedPassword
  val rememberCredentials: Boolean get() = secureStore.rememberCredentials

  /** Liga/desliga o lembrete de credenciais. */
  fun setRememberCredentials(enabled: Boolean) {
    secureStore.rememberCredentials = enabled
    if (!enabled) secureStore.clearRememberedCredentials()
  }

  private fun restoreSession(): AuthState {
    // Qualquer falha ao ler o storage significa "sem sessão" — nunca derruba o app.
    return runCatching {
        val token = secureStore.pbToken
        val userJson = secureStore.pbUserJson
        if (token.isNullOrBlank() || userJson.isNullOrBlank()) return@runCatching AuthState.SignedOut
        val user =
          runCatching { json.decodeFromString(UserRecord.serializer(), userJson) }.getOrNull()
            ?: return@runCatching AuthState.SignedOut
        if (user.mustChangePassword) AuthState.NeedsPasswordChange(user)
        else AuthState.SignedIn(user)
      }
      .getOrElse { error ->
        android.util.Log.e("AdminML", "Falha ao restaurar a sessão", error)
        AuthState.SignedOut
      }
  }

  suspend fun login(email: String, password: String, remember: Boolean = true): LoginResult =
    try {
      val auth = client.authWithPassword(email, password)
      secureStore.pbToken = auth.token
      secureStore.pbUserJson = json.encodeToString(UserRecord.serializer(), auth.record)

      // Guarda (ou apaga) as credenciais lembradas
      if (remember) {
        secureStore.rememberedEmail = email.trim()
        secureStore.rememberedPassword = password
        secureStore.rememberCredentials = true
      } else {
        secureStore.clearRememberedCredentials()
        secureStore.rememberCredentials = false
      }

      if (auth.record.mustChangePassword) {
        _state.value = AuthState.NeedsPasswordChange(auth.record)
        LoginResult.NeedsPasswordChange(auth.record)
      } else {
        _state.value = AuthState.SignedIn(auth.record)
        LoginResult.Success(auth.record)
      }
    } catch (err: ApiException) {
      LoginResult.Error(
        if (err.code == 400) {
          "E-mail ou senha inválidos. Verifique e tente novamente."
        } else {
          "Sem conexão com o servidor. Verifique sua internet e tente novamente."
        }
      )
    } catch (_: Exception) {
      LoginResult.Error("Sem conexão com o servidor. Verifique sua internet e tente novamente.")
    }

  suspend fun changePassword(oldPassword: String, newPassword: String): ChangePasswordResult {
    val user = currentUser
    if (user == null) return ChangePasswordResult.Error("Sessão expirada. Faça login novamente.")
    return try {
      client.changePassword(user.id, oldPassword, newPassword)
      // Re-autentica com a nova senha (igual ao portal web).
      val auth = client.authWithPassword(user.email, newPassword)
      secureStore.pbToken = auth.token
      secureStore.pbUserJson = json.encodeToString(UserRecord.serializer(), auth.record)
      // Se estiver lembrando credenciais, guarda a senha nova.
      if (secureStore.rememberCredentials) {
        secureStore.rememberedEmail = user.email
        secureStore.rememberedPassword = newPassword
      }
      _state.value = AuthState.SignedIn(auth.record)
      ChangePasswordResult.Success(auth.record)
    } catch (err: ApiException) {
      val message =
        if (err.message.contains("oldPassword", ignoreCase = true) ||
          err.message.contains("authenticate", ignoreCase = true)
        ) {
          "Senha atual incorreta. Digite novamente."
        } else {
          "Não foi possível salvar a nova senha. Tente novamente."
        }
      ChangePasswordResult.Error(message)
    } catch (_: Exception) {
      ChangePasswordResult.Error("Não foi possível salvar a nova senha. Tente novamente.")
    }
  }

  fun logout() {
    secureStore.clearSession()
    _state.value = AuthState.SignedOut
  }
}
