package com.bodasmel.adminml.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.theme.PortalColors

@Composable
fun LoginScreen(
  session: SessionManager,
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel = viewModel { LoginViewModel(session) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .imePadding()
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier.size(56.dp).background(PortalColors.NightBlue, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "M&L",
              color = PortalColors.Sand,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          Spacer(Modifier.height(16.dp))

          Text(
            text = if (state.mode == LoginMode.LOGIN) "Admin M&L" else "Alterar Senha",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text =
              if (state.mode == LoginMode.LOGIN) "Portal de administração — Bodas de Esmeralda"
              else "Defina sua nova senha",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Spacer(Modifier.height(24.dp))

          if (state.mode == LoginMode.LOGIN) {
            OutlinedTextField(
              value = state.email,
              onValueChange = viewModel::onEmailChange,
              label = { Text("E-mail") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
              value = state.password,
              onValueChange = viewModel::onPasswordChange,
              label = { Text("Senha") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              visualTransformation =
                if (state.showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              trailingIcon = {
                IconButton(onClick = viewModel::togglePasswordVisibility) {
                  Icon(
                    imageVector =
                      if (state.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (state.showPassword) "Ocultar senha" else "Mostrar senha",
                  )
                }
              },
            )
          } else {
            OutlinedTextField(
              value = state.currentPassword,
              onValueChange = viewModel::onCurrentPasswordChange,
              label = { Text("Senha atual") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
              value = state.newPassword,
              onValueChange = viewModel::onNewPasswordChange,
              label = { Text("Nova senha") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
              value = state.confirmPassword,
              onValueChange = viewModel::onConfirmPasswordChange,
              label = { Text("Repita a nova senha") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
          }

          state.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
            )
          }

          Spacer(Modifier.height(20.dp))

          Button(
            onClick = {
              if (state.mode == LoginMode.LOGIN) viewModel.submitLogin()
              else viewModel.submitChangePassword()
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
          ) {
            if (state.loading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Text(if (state.mode == LoginMode.LOGIN) "Entrar" else "Salvar nova senha")
            }
          }

          if (state.mode == LoginMode.CHANGE_PASSWORD) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::backToLogin) { Text("Voltar para o login") }
          }
        }
      }

      Spacer(Modifier.height(16.dp))
      Text(
        text = "Portal Admin — Bodas M&L",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
