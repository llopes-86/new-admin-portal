package com.bodasmel.adminml

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AndroidBiometricGate
import com.bodasmel.adminml.core.BiometricGate
import com.bodasmel.adminml.core.BiometricStatus
import com.bodasmel.adminml.core.Biometrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BiometricGateTest {

  @Test
  @Config(sdk = [27])
  fun `android 8 retorna UNSUPPORTED sem lancar`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    assertEquals(BiometricStatus.UNSUPPORTED, AndroidBiometricGate.status(context))
    assertFalse(Biometrics.isAvailable(context))
  }

  @Test
  @Config(sdk = [34])
  fun `android 14 retorna um status valido sem lancar`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val status = AndroidBiometricGate.status(context)

    assertTrue("Status fora do enum: $status", status in BiometricStatus.entries)
  }

  @Test
  @Config(sdk = [34])
  fun `authenticate com Activity sem suporte chama onError e nunca lanca`() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    var error: String? = null
    var succeeded = false
    var canceled = false

    AndroidBiometricGate.authenticate(
      activity = activity,
      onSuccess = { succeeded = true },
      onError = { error = it },
      onCancel = { canceled = true },
    )

    assertNotNull("O fallback para senha deveria receber um erro", error)
    assertTrue("A mensagem deve orientar o uso da senha: $error", error!!.contains("senha"))
    assertFalse(succeeded)
    assertFalse(canceled)
  }

  @Test
  @Config(sdk = [34])
  fun `fake do BiometricGate documenta o contrato de callbacks`() {
    val gate = FakeBiometricGate()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    var succeeded = false

    assertEquals(BiometricStatus.AVAILABLE, gate.status(context))
    gate.authenticate(
      activity = activity,
      onSuccess = { succeeded = true },
      onError = {},
      onCancel = {},
    )

    assertTrue("O fake deve poder simular o desbloqueio", succeeded)
  }

  private class FakeBiometricGate : BiometricGate {
    override fun status(context: Context): BiometricStatus = BiometricStatus.AVAILABLE

    override fun authenticate(
      activity: Activity,
      title: String,
      subtitle: String?,
      negativeButton: String,
      onSuccess: () -> Unit,
      onError: (String) -> Unit,
      onCancel: () -> Unit,
    ) {
      onSuccess()
    }
  }
}
