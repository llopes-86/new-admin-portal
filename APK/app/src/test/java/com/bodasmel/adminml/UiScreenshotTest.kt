package com.bodasmel.adminml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renderiza telas do app em PNG (Robolectric com gráficos nativos) para
 * conferência visual contra o portal web.
 *
 * Saída: `APK/app/build/screenshots/<tela>-<largura>dp.png`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AdminMlApp::class, qualifiers = "w393dp-h851dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiScreenshotTest {

  @Test
  fun `renderiza a tela de login`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    capture(controller.get(), "login-393dp")
  }

  @Test
  fun `renderiza a tela de convidados`() {
    val app = ApplicationProviderHolder.app()
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    controller.get().setContent {
      com.bodasmel.adminml.theme.AdminMLTheme {
        com.bodasmel.adminml.ui.guests.GuestsScreen(container = app.container)
      }
    }
    shadowOf(Looper.getMainLooper()).idle()
    capture(controller.get(), "guests-393dp")
  }

  @Test
  fun `renderiza o card de convidado`() {
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    controller.get().setContent {
      com.bodasmel.adminml.theme.AdminMLTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
          modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          com.bodasmel.adminml.ui.guests.GuestCard(
            item =
              com.bodasmel.adminml.data.RsvpRecord(
                id = "1",
                sheetRowId = 138.0,
                nome = "Leandro da Silva Lopes",
                adulto = "1",
                criancas = "0",
                contato = "(11) 98192-7487",
                restricoes = "[\"Vegetariano(a) / Vegano(a)\", \"Intolerância à Lactose\"]",
                status = "Pendente",
                created = "2026-10-22 03:52:29.868Z",
              ),
            dark = true,
            onToggle = {},
          )
          com.bodasmel.adminml.ui.guests.GuestCard(
            item =
              com.bodasmel.adminml.data.RsvpRecord(
                id = "2",
                sheetRowId = 137.0,
                nome = "Leandro da Silva Lopes",
                adulto = "1",
                criancas = "0",
                contato = "(11) 98192-7487",
                restricoes = "Nenhuma",
                status = "Confirmado",
                created = "2026-10-22 04:31:12.100Z",
              ),
            dark = true,
            onToggle = {},
          )
        }
      }
    }
    shadowOf(Looper.getMainLooper()).idle()
    capture(controller.get(), "card-convidado-393dp")
  }

  @Test
  fun `renderiza os campos do login preenchidos`() {
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    controller.get().setContent {
      com.bodasmel.adminml.theme.AdminMLTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
          modifier =
            androidx.compose.ui.Modifier
              .fillMaxSize()
              .background(com.bodasmel.adminml.theme.PortalColors.LoginCardBg)
              .padding(24.dp),
        ) {
          com.bodasmel.adminml.ui.components.PortalInput(
            value = "admin@bodasmel.com.br",
            onValueChange = {},
            placeholder = "seu@email.com",
            modifier = Modifier.fillMaxWidth(),
          )
          androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
          com.bodasmel.adminml.ui.components.PortalInput(
            value = "SenhaSecreta123",
            onValueChange = {},
            placeholder = "••••••••",
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
    shadowOf(Looper.getMainLooper()).idle()
    capture(controller.get(), "login-campos-393dp")
  }
  private fun capture(activity: ComponentActivity, name: String) {
    shadowOf(Looper.getMainLooper()).idle()

    val decor = activity.window.decorView
    val metrics = activity.resources.displayMetrics
    decor.measure(
      View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
    )
    decor.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    shadowOf(Looper.getMainLooper()).idle()

    val bitmap = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888)
    decor.draw(Canvas(bitmap))

    val dir = File("build/screenshots").apply { mkdirs() }
    val out = File(dir, "$name.png")
    out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    assertTrue("PNG nao foi gerado", out.exists() && out.length() > 0)
    println("SCREENSHOT: ${out.absolutePath} (${out.length()} bytes)")
  }
}
