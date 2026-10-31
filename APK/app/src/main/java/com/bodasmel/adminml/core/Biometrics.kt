package com.bodasmel.adminml.core

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fachada do desbloqueio biométrico. O app nunca fica inacessível sem
 * sensor/registro — nesse caso a tela de bloqueio oferece a senha.
 *
 * Nenhuma exceção é propagada para a UI: qualquer falha vira `onError`.
 */
object Biometrics {

  /** `true` quando o aparelho tem biometria cadastrada e utilizável. */
  fun isAvailable(context: Context): Boolean =
    runCatching {
        BiometricManager.from(context)
          .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
          BiometricManager.BIOMETRIC_SUCCESS
      }
      .getOrDefault(false)

  fun authenticate(
    activity: Activity,
    title: String = "Desbloquear Admin M&L",
    subtitle: String? = null,
    negativeButton: String = "Usar senha",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
  ) {
    val fragmentActivity = activity as? FragmentActivity
    if (fragmentActivity == null) {
      onError("Este aparelho não oferece suporte à biometria. Use a senha.")
      return
    }
    if (!isAvailable(fragmentActivity)) {
      onError("Biometria indisponível. Cadastre uma digital ou use a senha.")
      return
    }

    runCatching {
        val callback =
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
              result: BiometricPrompt.AuthenticationResult
            ) {
              onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              when (errorCode) {
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_CANCELED -> onCancel()
                else -> onError(friendlyMessage(errorCode))
              }
            }
          }

        val prompt =
          BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(fragmentActivity),
            callback,
          )

        val info =
          BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText(negativeButton)
            .apply { subtitle?.takeIf { it.isNotBlank() }?.let(::setSubtitle) }
            .build()

        prompt.authenticate(info)
      }
      .onFailure { onError("Não foi possível iniciar a biometria. Use a senha.") }
  }

  /** Mensagens em português para os erros do [BiometricPrompt]. */
  private fun friendlyMessage(errorCode: Int): String =
    when (errorCode) {
      BiometricPrompt.ERROR_LOCKOUT ->
        "Muitas tentativas. Aguarde alguns segundos e tente novamente ou use a senha."
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
        "Biometria bloqueada por excesso de tentativas. Use a senha."
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      BiometricPrompt.ERROR_NO_BIOMETRICS ->
        "Biometria indisponível. Cadastre uma digital ou use a senha."
      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
        "Nenhuma credencial cadastrada no aparelho. Use a senha."
      BiometricPrompt.ERROR_TIMEOUT ->
        "Tempo esgotado. Tente novamente ou use a senha."
      else -> "Não foi possível ler a biometria. Tente novamente ou use a senha."
    }
}
