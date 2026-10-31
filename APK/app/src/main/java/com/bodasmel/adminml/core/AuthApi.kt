package com.bodasmel.adminml.core

import com.bodasmel.adminml.data.AuthResponse
import com.bodasmel.adminml.data.UserRecord

/** Operações de autenticação usadas pelo [SessionManager] (permite fake nos testes). */
interface AuthApi {
  suspend fun authWithPassword(email: String, password: String): AuthResponse

  suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): UserRecord
}
