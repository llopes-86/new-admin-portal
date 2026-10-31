package com.bodasmel.adminml

import android.app.Activity
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.BiometricGate
import com.bodasmel.adminml.core.BiometricStatus
import com.bodasmel.adminml.theme.AdminMLTheme
import com.bodasmel.adminml.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class, qualifiers = "w393dp-h851dp-xhdpi")
class BiometricSettingsTest {

  @get:Rule val composeRule = createComposeRule()

  private fun app(): AdminMlApp = ApplicationProvider.getApplicationContext()

  private fun seedBiometricSetting(enabled: Boolean) = runBlocking {
    val container = app().container
    container.settingsStore.setBiometricUnlockEnabled(enabled)
    withTimeout(5_000) { container.settings.first { it.biometricUnlockEnabled == enabled } }
  }

  private fun showSettings(gate: BiometricGate) {
    val container = app().container
    composeRule.setContent {
      AdminMLTheme { SettingsScreen(container = container, onBack = {}, biometricGate = gate) }
    }
    composeRule.waitForIdle()
  }

  @Test
  fun `ligar com sucesso persiste true`() {
    seedBiometricSetting(false)
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Success)
    showSettings(gate)

    composeRule.onNode(isToggleable()).performClick()
    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitUntil(5_000) { app().container.settings.value.biometricUnlockEnabled }
    composeRule.waitForIdle()

    assertTrue(app().container.settings.value.biometricUnlockEnabled)
    composeRule.onNodeWithText("Desbloqueio por biometria ativado.").assertIsDisplayed()
  }

  @Test
  fun `cancelar nao persiste true`() {
    seedBiometricSetting(false)
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Cancel)
    showSettings(gate)

    composeRule.onNode(isToggleable()).performClick()
    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitForIdle()

    assertFalse(app().container.settings.value.biometricUnlockEnabled)
    composeRule
      .onNodeWithText("Biometria não confirmada", substring = true)
      .assertIsDisplayed()
  }

  @Test
  fun `erro na biometria nao persiste true`() {
    seedBiometricSetting(false)
    val gate = FakeBiometricGate(BiometricStatus.AVAILABLE, Outcome.Error)
    showSettings(gate)

    composeRule.onNode(isToggleable()).performClick()
    composeRule.waitUntil(5_000) { gate.authenticateCalls == 1 }
    composeRule.waitForIdle()

    assertFalse(app().container.settings.value.biometricUnlockEnabled)
    composeRule
      .onNodeWithText("Não foi possível ler a biometria.", substring = true)
      .assertIsDisplayed()
  }

  @Test
  fun `sem biometria cadastrada desabilita o switch`() {
    seedBiometricSetting(false)
    val gate = FakeBiometricGate(BiometricStatus.NONE_ENROLLED)
    showSettings(gate)

    composeRule.onNode(isToggleable()).assertIsNotEnabled()
    composeRule
      .onNodeWithText("Nenhuma biometria cadastrada", substring = true)
      .assertIsDisplayed()
  }

  private enum class Outcome { Success, Cancel, Error }

  private class FakeBiometricGate(
    private val status: BiometricStatus,
    private val outcome: Outcome = Outcome.Success,
  ) : BiometricGate {
    var authenticateCalls = 0

    override fun status(context: Context): BiometricStatus = status

    override fun authenticate(
      activity: Activity,
      title: String,
      subtitle: String?,
      negativeButton: String,
      onSuccess: () -> Unit,
      onError: (String) -> Unit,
      onCancel: () -> Unit,
    ) {
      authenticateCalls++
      when (outcome) {
        Outcome.Success -> onSuccess()
        Outcome.Cancel -> onCancel()
        Outcome.Error -> onError("Não foi possível ler a biometria. Tente novamente.")
      }
    }
  }
}
