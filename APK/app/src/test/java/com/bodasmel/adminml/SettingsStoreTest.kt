package com.bodasmel.adminml

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.core.AppSettings
import com.bodasmel.adminml.core.SettingsStore
import com.bodasmel.adminml.core.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettingsStoreTest {

  private val context = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var store: SettingsStore

  /** A DataStore é única por processo; cada teste parte do mesmo estado padrão. */
  @Before
  fun setUp() {
    store = SettingsStore(context)
    runBlocking {
      store.setBiometricUnlockEnabled(true)
      store.setPbBaseUrl("")
      store.setGotifyUrl("")
      store.setThemeMode(ThemeMode.LIGHT)
    }
  }

  @Test
  fun `biometria vem habilitada por padrao`() = runBlocking {
    assertTrue(AppSettings().biometricUnlockEnabled)
    assertTrue(store.settings.first().biometricUnlockEnabled)
  }

  @Test
  fun `persiste a biometria desabilitada e le de volta`() = runBlocking {
    store.setBiometricUnlockEnabled(false)

    assertFalse(store.settings.first().biometricUnlockEnabled)
  }

  @Test
  fun `persiste a biometria habilitada novamente`() = runBlocking {
    store.setBiometricUnlockEnabled(false)
    store.setBiometricUnlockEnabled(true)

    assertTrue(store.settings.first().biometricUnlockEnabled)
  }

  @Test
  fun `mantem os demais campos funcionando`() = runBlocking {
    store.setPbBaseUrl(" https://pb.exemplo.com.br/ ")
    store.setGotifyUrl(" https://gotify.exemplo.com.br/ ")
    store.setThemeMode(ThemeMode.DARK)

    val settings = store.settings.first()

    assertEquals("https://pb.exemplo.com.br", settings.pbBaseUrl)
    assertEquals("https://gotify.exemplo.com.br", settings.gotifyUrl)
    assertEquals(ThemeMode.DARK, settings.themeMode)
    assertTrue(settings.biometricUnlockEnabled)
  }

  @Test
  fun `url vazia do pocketbase volta ao padrao`() = runBlocking {
    store.setPbBaseUrl("   ")

    assertEquals(AppSettings.DEFAULT_PB_BASE_URL, store.settings.first().pbBaseUrl)
  }
}
