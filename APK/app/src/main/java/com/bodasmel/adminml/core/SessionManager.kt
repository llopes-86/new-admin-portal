package com.bodasmel.adminml.core

import com.bodasmel.adminml.data.ApiException
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
  data class Locked(val user: UserRecord) : AuthState
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

sealed interface UnlockResult {
  data object Success : UnlockResult
  data class Error(val message: String) : UnlockResult
}

/**
 * Sessão do admin no PocketBase.
 * Espelha `public/js/utils/auth.js` + `public/js/login.js` do portal web.
 *
 * A sessão nunca é retomada em um processo novo: o app sempre abre no login.
 * Ao ir para segundo plano a sessão é bloqueada (biometria/senha) e, após o
 * tempo de inatividade, encerrada de vez.
 */
class SessionManager(
  private val secureStore: SecureStore,
  private val client: AuthApi,
) {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
  val state: StateFlow<AuthState> = _state.asStateFlow()

  val currentUser: UserRecord?
    get() =
      when (val current = _state.value) {
        is AuthState.SignedIn -> current.user
        is AuthState.NeedsPasswordChange -> current.user
        is AuthState.Locked -> current.user
        AuthState.SignedOut -> null
      }

  /** E-mail lembrado ("Lembrar meu e-mail"). A senha nunca é guardada. */
  val rememberedEmail: String? get() = secureStore.rememberedEmail
  val rememberCredentials: Boolean get() = secureStore.rememberCredentials

  /** Liga/desliga o lembrete do e-mail. */
  fun setRememberCredentials(enabled: Boolean) {
    secureStore.rememberCredentials = enabled
    if (!enabled) secureStore.clearRememberedCredentials()
  }

  suspend fun login(email: String, password: String, remember: Boolean = true): LoginResult =
    try {
      val auth = client.authWithPassword(email, password)
      secureStore.pbToken = auth.token
      secureStore.pbUserJson = json.encodeToString(UserRecord.serializer(), auth.record)

      if (remember) {
        secureStore.rememberedEmail = email.trim()
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
      if (secureStore.rememberCredentials) {
        secureStore.rememberedEmail = user.email
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

  /** Bloqueia a sessão mantendo o token (desbloqueio por biometria ou senha). */
  fun lock() {
    val current = _state.value
    if (current is AuthState.SignedIn) _state.value = AuthState.Locked(current.user)
  }

  /** Desbloqueio biométrico: o token atual continua válido. */
  fun unlockWithBiometrics(): Boolean {
    val current = _state.value
    if (current !is AuthState.Locked) return false
    _state.value = AuthState.SignedIn(current.user)
    return true
  }

  /** Fallback do bloqueio: valida a senha no PocketBase e renova o token. */
  suspend fun unlockWithPassword(password: String): UnlockResult {
    val current = _state.value
    if (current !is AuthState.Locked) {
      return UnlockResult.Error("Sessão expirada. Faça login novamente.")
    }
    return try {
      val auth = client.authWithPassword(current.user.email, password)
      secureStore.pbToken = auth.token
      secureStore.pbUserJson = json.encodeToString(UserRecord.serializer(), auth.record)
      _state.value = AuthState.SignedIn(auth.record)
      UnlockResult.Success
    } catch (err: ApiException) {
      UnlockResult.Error(
        if (err.code == 400) {
          "Senha incorreta. Tente novamente."
        } else {
          "Sem conexão com o servidor. Verifique sua internet e tente novamente."
        }
      )
    } catch (_: Exception) {
      UnlockResult.Error("Sem conexão com o servidor. Verifique sua internet e tente novamente.")
    }
  }

  fun logout() {
    secureStore.clearSession()
    _state.value = AuthState.SignedOut
  }

  /** Encerra a sessão por inatividade (mesmo efeito do logout). */
  fun expireSession() = logout()
}
