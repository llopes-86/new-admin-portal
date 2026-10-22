package com.bodasmel.adminml.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.BuildConfig
import com.bodasmel.adminml.core.ThemeMode
import com.bodasmel.adminml.push.GotifyClient
import com.bodasmel.adminml.push.GotifyDiagnosis
import com.bodasmel.adminml.push.GotifyService
import com.bodasmel.adminml.push.GotifyUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
  val settings by container.settings.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  var pbUrl by remember(settings.pbBaseUrl) { mutableStateOf(settings.pbBaseUrl) }
  var gotifyUrl by remember(settings.gotifyUrl) { mutableStateOf(settings.gotifyUrl) }
  var gotifyUser by remember { mutableStateOf("") }
  var gotifyPassword by remember { mutableStateOf("") }
  var linking by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Ajustes") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        "Conexões",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      OutlinedTextField(
        value = pbUrl,
        onValueChange = { pbUrl = it; status = null },
        label = { Text("URL do PocketBase") },
        supportingText = { Text("Ex.: https://portal.bodasmel.com.br/api/pb") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
      )

      OutlinedTextField(
        value = gotifyUrl,
        onValueChange = { gotifyUrl = it; status = null },
        label = { Text("URL do Gotify") },
        supportingText = {
          Text(
            if (gotifyUrl.isBlank()) {
              "Ex.: https://websites-gotify.qlnxj1.easypanel.host"
            } else {
              "Será usado: ${GotifyUrl.sanitize(gotifyUrl)}"
            }
          )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = {
            scope.launch {
              val cleanGotify = GotifyUrl.sanitize(gotifyUrl)
              container.settingsStore.setPbBaseUrl(pbUrl)
              container.settingsStore.setGotifyUrl(cleanGotify)
              gotifyUrl = cleanGotify
              status = "Configurações salvas."
            }
          }
        ) { Text("Salvar") }

        Button(
          onClick = {
            scope.launch {
              val ok = container.client.health()
              status = if (ok) "PocketBase respondeu com sucesso." else "Não foi possível falar com o PocketBase."
            }
          }
        ) { Text("Testar PocketBase") }

        Button(
          onClick = {
            scope.launch {
              status = "Testando o Gotify…"
              when (val result = GotifyClient().diagnose(gotifyUrl)) {
                is GotifyDiagnosis.Ok -> status = "Gotify OK (${result.version})."
                is GotifyDiagnosis.Error -> status = result.message
              }
            }
          }
        ) { Text("Testar Gotify") }
      }

      status?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, style = MaterialTheme.typography.bodyMedium)
      }

      Spacer(Modifier.height(8.dp))
      Text(
        "Tema",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { scope.launch { container.settingsStore.setThemeMode(ThemeMode.LIGHT) } },
          colors =
            androidx.compose.material3.ButtonDefaults.buttonColors(
              containerColor =
                if (settings.themeMode == ThemeMode.LIGHT) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) { Text("Claro") }
        Button(
          onClick = { scope.launch { container.settingsStore.setThemeMode(ThemeMode.DARK) } },
          colors =
            androidx.compose.material3.ButtonDefaults.buttonColors(
              containerColor =
                if (settings.themeMode == ThemeMode.DARK) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) { Text("Escuro") }
      }

      Spacer(Modifier.height(8.dp))
      Text(
        "Notificações",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        "O app monitora o servidor Gotify em segundo plano e avisa a cada novo RSVP enviado pelo site. " +
          "Informe a URL do Gotify acima e faça login uma vez para vincular este aparelho.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = gotifyUser,
        onValueChange = { gotifyUser = it; status = null },
        label = { Text("Usuário do Gotify") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = gotifyPassword,
        onValueChange = { gotifyPassword = it; status = null },
        label = { Text("Senha do Gotify") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
      )
      Spacer(Modifier.height(8.dp))
      Button(
        onClick = {
          scope.launch {
            linking = true
            status = "Conectando ao Gotify…"
            val usedUrl =
              GotifyUrl.sanitize(if (gotifyUrl.isBlank()) settings.gotifyUrl else gotifyUrl)
            val result =
              GotifyClient().createClientToken(
                baseUrl = usedUrl,
                user = gotifyUser,
                password = gotifyPassword,
              )
            result
              .onSuccess { token ->
                container.secureStore.gotifyClientToken = token
                container.settingsStore.setGotifyUrl(usedUrl)
                gotifyUrl = usedUrl
                gotifyPassword = ""
                status = "Aparelho vinculado. As notificações começarão a chegar."
                GotifyService.start(context)
              }
              .onFailure {
                status = GotifyUrl.describeFailure(usedUrl, it)
              }
            linking = false
          }
        },
        enabled = !linking
      ) { Text(if (linking) "Vinculando…" else "Vincular este aparelho") }

      if (container.secureStore.gotifyClientToken != null) {
        Spacer(Modifier.height(4.dp))
        Text(
          "Este aparelho já está vinculado ao Gotify.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(Modifier.height(8.dp))
      Text(
        "Admin M&L ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
      )
      Text(
        "build ${BuildConfig.VERSION_CODE} · commit ${BuildConfig.GIT_COMMIT} · ${BuildConfig.GIT_TAG}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
