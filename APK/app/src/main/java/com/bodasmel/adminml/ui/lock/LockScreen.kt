package com.bodasmel.adminml.ui.lock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.bodasmel.adminml.R
import com.bodasmel.adminml.core.AndroidBiometricGate
import com.bodasmel.adminml.core.BiometricGate
import com.bodasmel.adminml.core.BiometricStatus
import com.bodasmel.adminml.core.SessionManager
import com.bodasmel.adminml.core.UnlockResult
import com.bodasmel.adminml.theme.InterFamily
import com.bodasmel.adminml.theme.PlayfairFamily
import com.bodasmel.adminml.theme.PortalColors
import com.bodasmel.adminml.ui.components.PortalInput
import kotlinx.coroutines.launch

/**
 * Tela de bloqueio exibida ao voltar ao app depois de sair.
 *
 * A biometria é oferecida automaticamente apenas quando a Activity já está em
 * RESUMED. Cancelar ou falhar NÃO troca para a senha: o usuário permanece no
 * modo biometria com a opção de tentar de novo (ou escolher a senha).
 */
@Composable
fun LockScreen(
  session: SessionManager,
  biometricEnabled: Boolean = true,
  gate: BiometricGate = AndroidBiometricGate,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val lifecycleOwner = LocalLifecycleOwner.current
  val user = session.currentUser
  val scope = rememberCoroutineScope()

  var status by remember(context, gate) { mutableStateOf(gate.status(context)) }
  var usePassword by rememberSaveable { mutableStateOf(false) }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var biometricHint by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var prompting by remember { mutableStateOf(false) }
  var autoPromptDone by remember { mutableStateOf(false) }

  val biometricMode =
    biometricEnabled &&
      !usePassword &&
      (status == BiometricStatus.AVAILABLE || status == BiometricStatus.UNAVAILABLE)

  fun requestBiometrics() {
    if (prompting) return
    val current = gate.status(context)
    status = current
    when (current) {
      BiometricStatus.AVAILABLE -> Unit
      BiometricStatus.UNAVAILABLE -> {
        biometricHint = "Biometria indisponível agora. Tente novamente."
        return
      }
      else -> {
        usePassword = true
        biometricHint = null
        return
      }
    }
    val host = activity
    if (host == null) {
      usePassword = true
      passwordError = "Não foi possível abrir a biometria. Use a senha."
      return
    }
    biometricHint = null
    prompting = true
    gate.authenticate(
      activity = host,
      title = "Desbloquear Admin M&L",
      subtitle = user?.email,
      negativeButton = "Cancelar",
      onSuccess = {
        prompting = false
        if (!session.unlockWithBiometrics()) {
          usePassword = true
          passwordError = "Sessão expirada. Entre com a senha."
        }
      },
      onError = {
        prompting = false
        biometricHint = it
      },
      onCancel = { prompting = false },
    )
  }

  // Recheca o suporte biométrico sempre que a tela volta a ficar ativa
  // (o usuário pode ter cadastrado uma digital nos ajustes do aparelho).
  LaunchedEffect(gate, context) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      status = gate.status(context)
    }
  }

  // Só dispara o prompt quando a Activity já está em RESUMED.
  LaunchedEffect(biometricMode, biometricEnabled, status) {
    if (!biometricMode || status != BiometricStatus.AVAILABLE) return@LaunchedEffect
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      if (!autoPromptDone && !usePassword) {
        autoPromptDone = true
        requestBiometrics()
      }
    }
  }

  // Ao voltar do segundo plano, permite um novo prompt automático — mas não
  // quando a pausa foi causada pelo próprio BiometricPrompt (evita loop).
  val promptingState = rememberUpdatedState(prompting)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        if (!promptingState.value) autoPromptDone = false
        prompting = false
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(modifier = modifier.fillMaxSize()) {
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
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier =
          Modifier
            .widthIn(max = 400.dp)
            .shadow(24.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(PortalColors.LoginCardBg)
            .border(1.dp, PortalColors.LoginGoldBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .size(96.dp)
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

        Spacer(Modifier.height(16.dp))
        Text(
          text = "Sessão bloqueada",
          fontFamily = PlayfairFamily,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = PortalColors.LightText,
        )
        user?.email?.let {
          Text(
            text = it,
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = PortalColors.Brown,
            modifier = Modifier.padding(top = 4.dp),
          )
        }

        Spacer(Modifier.height(24.dp))

        if (biometricMode) {
          if (prompting) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = PortalColors.Gold)
          }
          Text(
            text =
              biometricHint
                ?: if (status == BiometricStatus.UNAVAILABLE) {
                  "Biometria indisponível agora. Tente novamente."
                } else {
                  "Confirme sua biometria para continuar."
                },
            modifier = Modifier.padding(top = if (prompting) 12.dp else 0.dp),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = PortalColors.Brown,
          )
          TextButton(onClick = { requestBiometrics() }, enabled = !prompting) {
            Text(
              text = "Tentar biometria novamente",
              fontFamily = InterFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = PortalColors.Gold,
            )
          }
          TextButton(onClick = { usePassword = true }) {
            Text(
              text = "Usar senha",
              fontFamily = InterFamily,
              fontSize = 13.sp,
              color = PortalColors.Brown,
            )
          }
        } else {
          Text(
            text = "Senha",
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PortalColors.Brown,
          )
          PortalInput(
            value = password,
            onValueChange = {
              password = it
              passwordError = null
            },
            placeholder = "Sua senha",
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          )

          passwordError?.let {
            Text(
              text = it,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              fontFamily = InterFamily,
              fontSize = 13.sp,
              color = Color(0xFFDC2626),
            )
          }

          if (biometricEnabled && status != BiometricStatus.AVAILABLE) {
            biometricStatusMessage(status)?.let {
              Text(
                text = it,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = PortalColors.Brown,
              )
            }
            if (status == BiometricStatus.NONE_ENROLLED) {
              TextButton(onClick = { context.openBiometricEnrollment() }) {
                Text(
                  text = "Cadastrar biometria",
                  fontFamily = InterFamily,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = PortalColors.Gold,
                )
              }
            }
          }

          Button(
            onClick = {
              busy = true
              scope.launch {
                when (val result = session.unlockWithPassword(password)) {
                  is UnlockResult.Success -> password = ""
                  is UnlockResult.Error -> passwordError = result.message
                }
                busy = false
              }
            },
            enabled = !busy && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = PortalColors.Gold,
                contentColor = Color.White,
                disabledContainerColor = PortalColors.Gold.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
              ),
          ) {
            if (busy) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
              Text("Desbloquear", fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          if (biometricEnabled && status == BiometricStatus.AVAILABLE) {
            TextButton(
              onClick = {
                usePassword = false
                autoPromptDone = true
                requestBiometrics()
              }
            ) {
              Text(
                text = "Usar biometria",
                fontFamily = InterFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PortalColors.Gold,
              )
            }
          }
        }

        TextButton(onClick = { session.logout() }, modifier = Modifier.padding(top = 4.dp)) {
          Text(
            text = "Sair da conta",
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = PortalColors.Terracotta,
          )
        }
      }
    }
  }
}

private fun biometricStatusMessage(status: BiometricStatus): String? =
  when (status) {
    BiometricStatus.AVAILABLE -> null
    BiometricStatus.NONE_ENROLLED ->
      "Nenhuma biometria cadastrada neste aparelho. Cadastre uma digital ou rosto para desbloquear sem digitar a senha."
    BiometricStatus.NO_HARDWARE -> "Este aparelho não tem leitor biométrico. Use a senha."
    BiometricStatus.UNSUPPORTED -> "A biometria exige Android 9 ou superior. Use a senha."
    BiometricStatus.UNAVAILABLE -> "Biometria indisponível agora. Tente novamente."
  }

private fun Context.openBiometricEnrollment() {
  val intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(
          Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
          BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
      }
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
  runCatching { startActivity(intent) }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
