package com.bodasmel.adminml.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Armazenamento das credenciais sensíveis (token do PocketBase e client token do
 * Gotify). Usa EncryptedSharedPreferences (Android Keystore) e, se o keystore do
 * aparelho falhar, cai para um SharedPreferences simples — nunca derruba o app.
 */
class SecureStore(context: Context) {

  private val prefs: SharedPreferences =
    runCatching {
        val masterKey =
          MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
          context,
          FILE_NAME,
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
      }
      .getOrElse { error ->
        Log.e("AdminML", "Keystore indisponível, usando SharedPreferences simples", error)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
      }

  var pbToken: String?
    get() = prefs.getString(KEY_PB_TOKEN, null)
    set(value) = prefs.edit().putString(KEY_PB_TOKEN, value).apply()

  var pbUserJson: String?
    get() = prefs.getString(KEY_PB_USER, null)
    set(value) = prefs.edit().putString(KEY_PB_USER, value).apply()

  /** Client token do Gotify (usado no header X-Gotify-Key do /stream). */
  var gotifyClientToken: String?
    get() = prefs.getString(KEY_GOTIFY_CLIENT_TOKEN, null)
    set(value) = prefs.edit().putString(KEY_GOTIFY_CLIENT_TOKEN, value).apply()

  /** Último id de mensagem do Gotify já processada (evita notificação duplicada). */
  var gotifyLastMessageId: Long
    get() = prefs.getLong(KEY_GOTIFY_LAST_ID, 0L)
    set(value) = prefs.edit().putLong(KEY_GOTIFY_LAST_ID, value).apply()

  // ------------------------------------------------------------- credenciais lembradas

  /** E-mail do último login (para preencher a tela de login). */
  var rememberedEmail: String?
    get() = prefs.getString(KEY_REMEMBER_EMAIL, null)
    set(value) = prefs.edit().putString(KEY_REMEMBER_EMAIL, value).apply()

  /** Senha do último login (criptografada no Keystore). */
  var rememberedPassword: String?
    get() = prefs.getString(KEY_REMEMBER_PASSWORD, null)
    set(value) = prefs.edit().putString(KEY_REMEMBER_PASSWORD, value).apply()

  /** Se o app deve lembrar e-mail e senha ("Lembrar-me"). */
  var rememberCredentials: Boolean
    get() = prefs.getBoolean(KEY_REMEMBER_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ENABLED, value).apply()

  /** Apaga somente as credenciais lembradas (mantém a sessão). */
  fun clearRememberedCredentials() {
    prefs.edit().remove(KEY_REMEMBER_EMAIL).remove(KEY_REMEMBER_PASSWORD).apply()
  }

  fun clearSession() {
    prefs.edit().remove(KEY_PB_TOKEN).remove(KEY_PB_USER).apply()
  }

  private companion object {
    const val FILE_NAME = "adminml_secure"
    const val KEY_PB_TOKEN = "pb_token"
    const val KEY_PB_USER = "pb_user"
    const val KEY_GOTIFY_CLIENT_TOKEN = "gotify_client_token"
    const val KEY_GOTIFY_LAST_ID = "gotify_last_message_id"
    const val KEY_REMEMBER_EMAIL = "remember_email"
    const val KEY_REMEMBER_PASSWORD = "remember_password"
    const val KEY_REMEMBER_ENABLED = "remember_enabled"
  }
}
