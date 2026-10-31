package com.bodasmel.adminml.core

import android.app.Activity
import android.content.Context

/**
 * Fachada do desbloqueio biométrico. O agente de plataforma implementa o
 * `BiometricPrompt`; o app nunca fica inacessível sem sensor/registro — nesse
 * caso a tela de bloqueio oferece a senha.
 */
object Biometrics {

  fun isAvailable(context: Context): Boolean = false

  fun authenticate(
    activity: Activity,
    title: String = "Desbloquear Admin M&L",
    subtitle: String? = null,
    negativeButton: String = "Usar senha",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
  ) {
    onError("Biometria indisponível neste aparelho. Use a senha.")
  }
}
