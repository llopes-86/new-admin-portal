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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodasmel.adminml.BuildConfig
import com.bodasmel.adminml.R
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.theme.InterFamily
import com.bodasmel.adminml.theme.PlayfairFamily
import com.bodasmel.adminml.theme.PortalColors
import com.bodasmel.adminml.ui.components.PortalInput
import kotlin.math.roundToInt

/**
 * Tela de login com paridade visual com o portal web.
 *
 * Referência: `public/index.html` + `public/css/admin.css` (bloco "LOGIN PAGE")
 * e `public/css/components/forms.css`. Os mesmos breakpoints da web são
 * respeitados: <=480px (celular), 481–1024px (tablet) e >1024px (desktop).
 */
@Composable
fun LoginScreen(
  session: SessionManager,
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel = viewModel { LoginViewModel(session) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val shake = rememberShake(state.errorCount)

  val widthDp = LocalConfiguration.current.screenWidthDp
  val compact = widthDp <= 480
  val medium = widthDp in 481..1024

  val cardPadH = when {
    compact -> 20.dp
    medium -> 24.dp
    else -> 40.dp
  }
  val cardPadV = when {
    compact -> 28.dp
    medium -> 32.dp
    else -> 40.dp
  }
  val cardRadius = if (compact) 14.dp else 20.dp
  val logoSize = if (compact) 100.dp else 160.dp
  val logoGap = if (compact) 14.dp else 18.dp
  val titleSize = if (compact) 20.sp else 26.sp
  val headerGap = when {
    compact -> 24.dp
    medium -> 28.dp
    else -> 32.dp
  }
  val formGap = if (compact) 16.dp else 20.dp
  val outerPad = if (compact) 12.dp else 20.dp

  Box(modifier = modifier.fillMaxSize()) {
    // Fundo: mesmo tecido do portal web (`.login-page`)
    Image(
      painter = painterResource(R.drawable.africa_day_bg),
      contentDescription = null,
      modifier =
        Modifier
          .fillMaxSize()
          // `background-position: 30% center` no breakpoint de 480px da web
          .graphicsLayer {
            if (compact) {
              // equivale ao `background-position: 30% center` da web no celular
              scaleX = 1.25f
              scaleY = 1.25f
              translationX = -size.width * 0.10f
            }
          },
      contentScale = ContentScale.Crop,
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .imePadding()
          .padding(horizontal = outerPad, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      LoginCard(
        cardRadius = cardRadius,
        cardPadH = cardPadH,
        cardPadV = cardPadV,
      ) {
        LoginContent(
          state = state,
          viewModel = viewModel,
          shake = shake,
          logoSize = logoSize,
          logoGap = logoGap,
          titleSize = titleSize,
          headerGap = headerGap,
          formGap = formGap,
        )

        // `.login-version` — rodapé dentro do card (como na web)
        Spacer(Modifier.height(24.dp))
        Text(
          text = "v${BuildConfig.VERSION_NAME}",
          fontFamily = InterFamily,
          fontSize = 12.sp,
          color = PortalColors.Brown.copy(alpha = 0.75f),
          letterSpacing = 0.4.sp,
        )
      }
    }

    // Overlay "Acessando painel" (igual ao .login-overlay da web)
    AnimatedVisibility(
      visible = state.loading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(PortalColors.NightBlue.copy(alpha = 0.55f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .widthIn(max = 300.dp)
              .padding(horizontal = 28.dp, vertical = 32.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(PortalColors.LoginOverlayCardBg)
              .border(1.dp, PortalColors.LoginGoldBorder, RoundedCornerShape(16.dp))
              .shadow(24.dp, RoundedCornerShape(16.dp), clip = false),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 2.dp,
            color = PortalColors.Gold,
          )
          Spacer(Modifier.height(14.dp))
          Text(
            text = "Acessando painel",
            fontFamily = InterFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortalColors.LightText,
          )
          Text(
            text = "Verificando suas credenciais…",
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = PortalColors.Brown.copy(alpha = 0.85f),
          )
        }
      }
    }
  }
}

/** Card de login (mesmo papel do .login-card da web). */
@Composable
private fun LoginCard(
  cardRadius: Dp,
  cardPadH: Dp,
  cardPadV: Dp,
  content: @Composable () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .widthIn(max = 400.dp)
        .shadow(24.dp, RoundedCornerShape(cardRadius), clip = false)
        .clip(RoundedCornerShape(cardRadius))
        .background(PortalColors.LoginCardBg)
        .border(1.dp, PortalColors.LoginGoldBorder, RoundedCornerShape(cardRadius))
        .padding(horizontal = cardPadH, vertical = cardPadV),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = { content() },
  )
}

@Composable
private fun LoginContent(
  state: LoginUiState,
  viewModel: LoginViewModel,
  shake: Float,
  logoSize: Dp,
  logoGap: Dp,
  titleSize: TextUnit,
  headerGap: Dp,
  formGap: Dp,
) {
  // Cabeçalho: Sankofa em círculo branco + títulos (`.login-header`)
  Box(
    modifier =
      Modifier
        .size(logoSize)
        .clip(CircleShape)
        .background(Color.White),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(R.drawable.sankofa),
      contentDescription = "Sankofa",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
  Spacer(Modifier.height(logoGap))
  Text(
    text = "Portal Admin",
    fontFamily = PlayfairFamily,
    fontSize = titleSize,
    fontWeight = FontWeight.Bold,
    color = PortalColors.LightText,
    letterSpacing = (-0.3).sp,
  )
  Text(
    text = "Bodas de Esmeralda — M&L",
    fontFamily = InterFamily,
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = PortalColors.Brown,
    modifier = Modifier.padding(top = 4.dp),
  )

  Spacer(Modifier.height(headerGap))

  if (state.mode == LoginMode.LOGIN) {
    // E-mail
    FieldLabel("E-mail")
    PortalInput(
      value = state.email,
      onValueChange = viewModel::onEmailChange,
      placeholder = "seu@email.com",
      modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.roundToInt(), 0) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )

    Spacer(Modifier.height(formGap))

    // Senha
    FieldLabel("Senha")
    PortalInput(
      value = state.password,
      onValueChange = viewModel::onPasswordChange,
      placeholder = "••••••••",
      modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.roundToInt(), 0) },
      visualTransformation =
        if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      trailing = {
        PasswordToggle(
          visible = state.showPassword,
          onClick = viewModel::togglePasswordVisibility,
        )
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
        colors =
          CheckboxDefaults.colors(
            checkedColor = PortalColors.Gold,
            uncheckedColor = PortalColors.Brown,
            checkmarkColor = Color.White,
          ),
      )
      Text(
        "Lembrar-me",
        fontFamily = InterFamily,
        fontSize = 13.sp,
        color = PortalColors.Brown,
      )
    }

    state.error?.let { LoginError(it) }

    Spacer(Modifier.height(formGap))

    PortalPrimaryButton(
      text = "Entrar",
      onClick = viewModel::submitLogin,
      enabled = !state.loading,
    )
  } else {
    // Troca de senha
    Text(
      text = "Por segurança, crie uma nova senha para continuar. Você irá para o painel em seguida.",
      fontFamily = InterFamily,
      fontSize = 14.sp,
      color = PortalColors.Brown,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
      Text(
        text = "← Sair e voltar ao login",
        fontFamily = InterFamily,
        fontSize = 13.sp,
        color = PortalColors.Gold,
        modifier = Modifier.clickable { viewModel.backToLogin() },
      )
    }

    Spacer(Modifier.height(formGap))

    FieldLabel("Senha atual")
    PortalInput(
      value = state.currentPassword,
      onValueChange = viewModel::onCurrentPasswordChange,
      placeholder = "••••••••",
      modifier = Modifier.fillMaxWidth(),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )

    Spacer(Modifier.height(formGap))

    FieldLabel("Nova senha")
    PortalInput(
      value = state.newPassword,
      onValueChange = viewModel::onNewPasswordChange,
      placeholder = "Crie sua nova senha",
      modifier = Modifier.fillMaxWidth(),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )
    Text(
      text = "Mínimo 8 caracteres. Use uma senha diferente da atual.",
      fontFamily = InterFamily,
      fontSize = 12.sp,
      color = PortalColors.Brown.copy(alpha = 0.9f),
      modifier = Modifier.padding(top = 6.dp),
    )

    Spacer(Modifier.height(formGap))

    FieldLabel("Confirmar nova senha")
    PortalInput(
      value = state.confirmPassword,
      onValueChange = viewModel::onConfirmPasswordChange,
      placeholder = "Repita a nova senha",
      modifier = Modifier.fillMaxWidth(),
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    )

    state.error?.let { LoginError(it) }

    Spacer(Modifier.height(formGap))

    PortalPrimaryButton(
      text = "Salvar nova senha",
      onClick = viewModel::submitChangePassword,
      enabled = !state.loading,
    )
  }
}

/** Rótulo de campo (`.login-page .form-label`): 13px, 600, marrom. */
@Composable
private fun FieldLabel(text: String) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    fontFamily = InterFamily,
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    color = PortalColors.Brown,
  )
}

/** Botão principal (`.login-page .btn-primary`): ouro, raio 10px, 48px de altura. */
@Composable
private fun PortalPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(10.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = PortalColors.Gold,
        contentColor = Color.White,
        disabledContainerColor = PortalColors.Gold.copy(alpha = 0.5f),
        disabledContentColor = Color.White,
      ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
  ) {
    Text(text, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
  }
}

/** Botão do olho (`.password-toggle`): 18px, cor #6b5d52. */
@Composable
private fun PasswordToggle(visible: Boolean, onClick: () -> Unit) {
  Icon(
    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
    tint = PortalColors.LoginPlaceholder,
    modifier = Modifier.size(32.dp).clickable(onClick = onClick).padding(7.dp),
  )
}

/** Caixa de erro (`.login-error`). */
@Composable
private fun LoginError(message: String) {
  Spacer(Modifier.height(10.dp))
  Text(
    text = message,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(PortalColors.LoginErrorBg)
        .border(1.dp, PortalColors.LoginErrorBorder, RoundedCornerShape(10.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
    fontFamily = InterFamily,
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
