package com.bodasmel.adminml

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bodasmel.adminml.core.AuthState
import com.bodasmel.adminml.core.ThemeMode
import com.bodasmel.adminml.theme.AdminMLTheme
import com.bodasmel.adminml.ui.home.HomeScreen
import com.bodasmel.adminml.ui.login.LoginScreen

class MainActivity : ComponentActivity() {

  /** Usado pela notificação para abrir direto na aba de convidados. */
  companion object {
    const val EXTRA_OPEN_TAB = "com.bodasmel.adminml.extra.OPEN_TAB"
  }

  private var openGuests by mutableStateOf(false)

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIntent(intent)

    setContent {
      val app = application as? AdminMlApp
      val initError = app?.initError
      val container = runCatching { app?.container }.getOrNull()

      if (initError != null || container == null) {
        StartupErrorScreen(initError ?: "Não foi possível iniciar o aplicativo.")
        return@setContent
      }

      val settings by container.settings.collectAsStateWithLifecycle()
      val authState by container.session.state.collectAsStateWithLifecycle()

      AdminMLTheme(darkTheme = settings.themeMode == ThemeMode.DARK) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          when (authState) {
            is AuthState.SignedOut, is AuthState.NeedsPasswordChange -> LoginScreen(container.session)
            is AuthState.SignedIn ->
              HomeScreen(
                container = container,
                openGuests = openGuests,
                onRequestNotificationPermission = ::ensureNotificationPermission,
              )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getStringExtra(EXTRA_OPEN_TAB) == "guests") openGuests = true
  }

  /** Android 13+ exige permissão explícita para exibir notificações. */
  private fun ensureNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val granted =
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }
}

/** Tela exibida quando a inicialização falha — mostra o motivo em vez de fechar o app. */
@Composable
private fun StartupErrorScreen(message: String) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Scaffold { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Não foi possível abrir o Admin M&L",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
          text = "Reinicie o aplicativo. Se persistir, envie esta mensagem.",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 12.dp),
        )
      }
    }
  }
}
