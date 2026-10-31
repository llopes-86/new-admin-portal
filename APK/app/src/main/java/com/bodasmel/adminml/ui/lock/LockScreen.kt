package com.bodasmel.adminml.ui.lock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Tenta a biometria na primeira composição e cai para a senha quando o sensor
 * não existe, é cancelado ou o desbloqueio falha.
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
  val biometricStatus = remember(context, gate) { gate.status(context) }
  val biometricsAvailable = biometricStatus == BiometricStatus.AVAILABLE

  val user = session.currentUser
  val scope = rememberCoroutineScope()
  var passwordMode by rememberSaveable { mutableStateOf(!biometricEnabled || !biometricsAvailable) }
  var password by rememberSaveable { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  LaunchedEffect(biometricEnabled, biometricsAvailable, biometricStatus) {
    if (!biometricEnabled || !biometricsAvailable) {
      passwordMode = true
      return@LaunchedEffect
    }
    val host = activity
    if (host == null) {
      passwordMode = true
      return@LaunchedEffect
    }
    gate.authenticate(
      activity = host,
      onSuccess = { if (!session.unlockWithBiometrics()) passwordMode = true },
      onError = {
        error = it
        passwordMode = true
      },
      onCancel = { passwordMode = true },
    )
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

        if (passwordMode) {
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
              error = null
            },
            placeholder = "Sua senha",
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          )

          error?.let {
            Text(
              text = it,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              fontFamily = InterFamily,
              fontSize = 13.sp,
              color = Color(0xFFDC2626),
            )
          }

          Button(
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
        } else {
          CircularProgressIndicator(modifier = Modifier.size(36.dp), color = PortalColors.Gold)
          Text(
            text = "Confirme sua biometria para continuar.",
            modifier = Modifier.padding(top = 12.dp),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = PortalColors.Brown,
          )
          TextButton(onClick = { passwordMode = true }) {
            Text(
              text = "Entrar com senha",
              fontFamily = InterFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = PortalColors.Gold,
            )
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

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
