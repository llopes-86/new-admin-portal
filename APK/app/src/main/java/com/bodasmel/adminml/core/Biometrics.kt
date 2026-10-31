package com.bodasmel.adminml.core

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Resultado da checagem de biometria do aparelho. */
enum class BiometricStatus {
  AVAILABLE,
  NO_HARDWARE,
  NONE_ENROLLED,
  UNAVAILABLE,
  UNSUPPORTED,
}

/**
 * Porta de entrada do desbloqueio biométrico (permite fake nos testes).
 * Nenhuma exceção é propagada para a UI: qualquer falha vira `onError`.
 */
interface BiometricGate {
  fun status(context: Context): BiometricStatus

  fun authenticate(
    activity: Activity,
    title: String = "Desbloquear Admin M&L",
    subtitle: String? = null,
    negativeButton: String = "Usar senha",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
  )
}

/** Implementação real usando [BiometricPrompt]. */
object AndroidBiometricGate : BiometricGate {

  override fun status(context: Context): BiometricStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return BiometricStatus.UNSUPPORTED
    val code =
      runCatching {
          BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        }
        .getOrElse { return BiometricStatus.UNAVAILABLE }
    return when (code) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
      BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.UNSUPPORTED
      BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.UNAVAILABLE
      else -> BiometricStatus.UNAVAILABLE
    }
  }

  override fun authenticate(
    activity: Activity,
    title: String,
    subtitle: String?,
    negativeButton: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
  ) {
    val fragmentActivity = activity as? FragmentActivity
    if (fragmentActivity == null) {
      onError("Este aparelho não oferece suporte à biometria. Use a senha.")
      return
    }
    if (fragmentActivity.isFinishing || fragmentActivity.isDestroyed) {
      onError("Não foi possível iniciar a biometria. Use a senha.")
      return
    }
    when (status(fragmentActivity)) {
      BiometricStatus.AVAILABLE -> Unit
      BiometricStatus.NONE_ENROLLED -> {
        onError("Nenhuma biometria cadastrada. Cadastre uma digital ou rosto no aparelho.")
        return
      }
      BiometricStatus.NO_HARDWARE -> {
        onError("Este aparelho não tem leitor biométrico. Use a senha.")
        return
      }
      BiometricStatus.UNSUPPORTED -> {
        onError("Biometria requer Android 9 ou superior. Use a senha.")
        return
      }
      BiometricStatus.UNAVAILABLE -> {
        onError("Biometria indisponível agora. Tente novamente ou use a senha.")
        return
      }
    }

    runCatching {
        val callback =
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
              result: BiometricPrompt.AuthenticationResult
            ) {
              runCatching { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              when (errorCode) {
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_CANCELED -> runCatching { onCancel() }
                else -> runCatching { onError(friendlyMessage(errorCode)) }
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

/** Compatibilidade com o código existente da tela de bloqueio. */
object Biometrics {
  fun isAvailable(context: Context): Boolean =
    AndroidBiometricGate.status(context) == BiometricStatus.AVAILABLE

  fun authenticate(
    activity: Activity,
    title: String = "Desbloquear Admin M&L",
    subtitle: String? = null,
    negativeButton: String = "Usar senha",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
  ) = AndroidBiometricGate.authenticate(
    activity = activity,
    title = title,
    subtitle = subtitle,
    negativeButton = negativeButton,
    onSuccess = onSuccess,
    onError = onError,
    onCancel = onCancel,
  )
}
