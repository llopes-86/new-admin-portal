package com.bodasmel.adminml.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.core.UnlockResult
import kotlinx.coroutines.launch

/**
 * Tela de bloqueio exibida ao voltar ao app depois de sair.
 * O desbloqueio por biometria é ligado pelo agente de plataforma; até lá a
 * senha é o fallback padrão.
 */
@Composable
fun LockScreen(session: SessionManager, modifier: Modifier = Modifier) {
  val user = session.currentUser
  val scope = rememberCoroutineScope()
  var password by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text("Sessão bloqueada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      user?.email?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
      }

      OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = null },
        label = { Text("Senha") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
      )

      error?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
      }

      Button(
        enabled = !busy && password.isNotBlank(),
        onClick = {
          busy = true
          scope.launch {
            when (val result = session.unlockWithPassword(password)) {
              is UnlockResult.Success -> password = ""
              is UnlockResult.Error -> error = result.message
            }
            busy = false
          }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
      ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.height(20.dp))
        else Text("Desbloquear")
      }

      TextButton(onClick = { session.logout() }, modifier = Modifier.padding(top = 8.dp)) {
        Text("Sair da conta")
      }
    }
  }
}
