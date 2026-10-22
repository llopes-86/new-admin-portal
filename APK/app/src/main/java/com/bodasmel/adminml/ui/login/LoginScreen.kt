package com.bodasmel.adminml.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodasmel.adminml.BuildConfig
import com.bodasmel.adminml.R
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.theme.PortalColors
import kotlin.math.roundToInt

/**
 * Tela de login com a identidade visual do portal web
 * (referência: public/index.html + public/css/admin.css).
 */
@Composable
fun LoginScreen(
  session: SessionManager,
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel = viewModel { LoginViewModel(session) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val shake = rememberShake(state.errorCount)

  Box(modifier = modifier.fillMaxSize()) {
    // Fundo: mesmo tecido do portal web
    Image(
      painter = painterResource(R.drawable.africa_day_bg),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .imePadding()
          .padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      LoginCard { LoginContent(state = state, viewModel = viewModel, shake = shake) }

      Spacer(Modifier.height(24.dp))
      Text(
        text = "v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.bodySmall,
        color = PortalColors.Brown,
      )
    }

    // Overlay "Acessando painel" (igual ao .login-overlay da web)
    AnimatedVisibility(
      visible = state.loading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier = Modifier.fillMaxSize().background(PortalColors.NightBlue.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .padding(32.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(PortalColors.Sand.copy(alpha = 0.98f))
              .border(1.dp, PortalColors.Gold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
              .padding(horizontal = 28.dp, vertical = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 2.dp,
            color = PortalColors.Gold,
          )
          Spacer(Modifier.height(14.dp))
          Text(
            "Acessando painel",
            style = MaterialTheme.typography.titleSmall,
            color = PortalColors.LightText,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Verificando suas credenciais…",
            style = MaterialTheme.typography.bodySmall,
            color = PortalColors.Brown,
          )
        }
      }
    }
  }
}

/** Card de login (mesmo papel do .login-card da web). */
@Composable
private fun LoginCard(content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .shadow(24.dp, RoundedCornerShape(20.dp), clip = false)
        .clip(RoundedCornerShape(20.dp))
        .background(PortalColors.Sand.copy(alpha = 0.96f))
        .border(1.dp, PortalColors.Gold.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        .padding(horizontal = 28.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = { content() },
  )
}

@Composable
private fun LoginContent(
  state: LoginUiState,
  viewModel: LoginViewModel,
  shake: Float,
) {
  // Cabeçalho: Sankofa + títulos
  Image(
    painter = painterResource(R.drawable.sankofa),
    contentDescription = "Sankofa",
    modifier = Modifier.size(150.dp).clip(CircleShape),
  )
  Spacer(Modifier.height(16.dp))
  Text(
    text = "Portal Admin",
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
    color = PortalColors.LightText,
  )
  Text(
    text = "Bodas de Esmeralda — M&L",
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = PortalColors.Brown,
  )

  Spacer(Modifier.height(28.dp))

  if (state.mode == LoginMode.LOGIN) {
    // E-mail
    FieldLabel("E-mail")
    OutlinedTextField(
      value = state.email,
      onValueChange = viewModel::onEmailChange,
      modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.roundToInt(), 0) },
      placeholder = { Text("seu@email.com") },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )

    Spacer(Modifier.height(14.dp))

    // Senha
    FieldLabel("Senha")
    OutlinedTextField(
      value = state.password,
      onValueChange = viewModel::onPasswordChange,
      modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.roundToInt(), 0) },
      placeholder = { Text("••••••••") },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      visualTransformation =
        if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      trailingIcon = {
        IconButton(onClick = viewModel::togglePasswordVisibility) {
          Icon(
            imageVector =
              if (state.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (state.showPassword) "Ocultar senha" else "Mostrar senha",
            tint = PortalColors.LightTextMuted,
          )
        }
      },
    )

    Spacer(Modifier.height(10.dp))

    // Lembrar-me
    Row(
      modifier = Modifier.fillMaxWidth().clickable { viewModel.onRememberChange(!state.rememberCredentials) },
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = state.rememberCredentials,
        onCheckedChange = viewModel::onRememberChange,
      )
      Text(
        "Lembrar-me",
        style = MaterialTheme.typography.bodySmall,
        color = PortalColors.LightText,
      )
    }

    state.error?.let { LoginError(it) }

    Spacer(Modifier.height(14.dp))

    Button(
      onClick = viewModel::submitLogin,
      enabled = !state.loading,
      modifier = Modifier.fillMaxWidth().height(48.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = PortalColors.LightPrimary),
    ) {
      Text("Entrar", fontWeight = FontWeight.SemiBold)
    }
  } else {
    // Troca de senha
    Text(
      "Por segurança, crie uma nova senha para continuar. Você irá para o painel em seguida.",
      style = MaterialTheme.typography.bodySmall,
      color = PortalColors.Brown,
    )
    Spacer(Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
    ) {
      TextButton(onClick = viewModel::backToLogin) {
        Text("← Sair e voltar ao login", color = PortalColors.LightPrimary)
      }
    }

    Spacer(Modifier.height(12.dp))

    FieldLabel("Senha atual")
    OutlinedTextField(
      value = state.currentPassword,
      onValueChange = viewModel::onCurrentPasswordChange,
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("••••••••") },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )

    Spacer(Modifier.height(14.dp))

    FieldLabel("Nova senha")
    OutlinedTextField(
      value = state.newPassword,
      onValueChange = viewModel::onNewPasswordChange,
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Crie sua nova senha") },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )
    Text(
      "Mínimo 8 caracteres. Use uma senha diferente da atual.",
      style = MaterialTheme.typography.bodySmall,
      color = PortalColors.LightTextMuted,
    )

    Spacer(Modifier.height(14.dp))

    FieldLabel("Confirmar nova senha")
    OutlinedTextField(
      value = state.confirmPassword,
      onValueChange = viewModel::onConfirmPasswordChange,
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Repita a nova senha") },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    )

    state.error?.let { LoginError(it) }

    Spacer(Modifier.height(14.dp))

    Button(
      onClick = viewModel::submitChangePassword,
      enabled = !state.loading,
      modifier = Modifier.fillMaxWidth().height(48.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = PortalColors.LightPrimary),
    ) {
      Text("Salvar nova senha", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun FieldLabel(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    color = PortalColors.LightText,
  )
}

@Composable
private fun LoginError(message: String) {
  Spacer(Modifier.height(10.dp))
  Text(
    text = message,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0x1FEF4444))
        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(10.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
    fontSize = 13.sp,
    color = Color(0xFFDC2626),
  )
}

/** Animação de "shake" dos campos quando o login falha (igual à web). */
@Composable
private fun rememberShake(errorCount: Int): Float {
  val offset = remember { Animatable(0f) }
  LaunchedEffect(errorCount) {
    if (errorCount == 0) return@LaunchedEffect
    listOf(-12f, 12f, -8f, 8f, -4f, 4f, 0f).forEach { target ->
      offset.animateTo(target, tween(durationMillis = 50))
    }
  }
  return offset.value
}
