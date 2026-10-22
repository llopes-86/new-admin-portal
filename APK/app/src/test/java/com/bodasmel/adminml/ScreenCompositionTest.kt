package com.bodasmel.adminml

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.bodasmel.adminml.theme.AdminMLTheme
import com.bodasmel.adminml.ui.home.HomeScreen
import com.bodasmel.adminml.ui.login.LoginScreen
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Garante que as telas realmente compõem sem exceção (era o que matava o app:
 * "abre e fecha"). O Robolectric executa a composição de verdade na JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class)
class ScreenCompositionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun `Activity principal abre sem estourar`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    shadowOf(Looper.getMainLooper()).idle()
    assertNotNull(controller.get())
  }

  @Test
  fun `tela de login compõe`() {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    composeRule.setContent {
      AdminMLTheme { LoginScreen(app.container.session) }
    }
    assertNotNull(app.container.session)
  }

  @Test
  fun `tela do painel compõe`() {
    val app = ApplicationProvider.getApplicationContext<AdminMlApp>()
    composeRule.setContent {
      AdminMLTheme {
        HomeScreen(container = app.container, onRequestNotificationPermission = {})
      }
    }
    assertNotNull(app.container.guestsStore)
  }
}
