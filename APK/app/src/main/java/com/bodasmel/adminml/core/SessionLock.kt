package com.bodasmel.adminml.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bloqueia a sessão quando o app vai para segundo plano e desloga após o
 * período de inatividade. Os dois temporizadores são cancelados ao voltar.
 */
class SessionLock(
  private val session: SessionManager,
  private val scope: CoroutineScope,
  private val lockDelayMs: Long = LOCK_DELAY_MS,
  private val logoutDelayMs: Long = LOGOUT_DELAY_MS,
) {

  private var lockJob: Job? = null
  private var logoutJob: Job? = null

  fun onForeground() {
    lockJob?.cancel()
    logoutJob?.cancel()
    lockJob = null
    logoutJob = null
  }

  fun onBackground() {
    // Idempotente: um segundo aviso de "saiu" não reinicia o prazo de logout
    // (o lock pode já ter disparado enquanto o logout continua agendado).
    if (lockJob?.isActive == true || logoutJob?.isActive == true) return
    lockJob = scope.launch {
      delay(lockDelayMs)
      session.lock()
    }
    logoutJob = scope.launch {
      delay(logoutDelayMs)
      session.expireSession()
    }
  }

  companion object {
    const val LOCK_DELAY_MS = 1_500L
    const val LOGOUT_DELAY_MS = 15 * 60_000L
  }
}
