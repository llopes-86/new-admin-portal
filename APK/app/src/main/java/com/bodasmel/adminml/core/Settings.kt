package com.bodasmel.adminml.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "adminml_settings")

/** Tema do app — espelha o toggle claro/escuro do portal web. */
enum class ThemeMode { LIGHT, DARK }

data class AppSettings(
  val pbBaseUrl: String = DEFAULT_PB_BASE_URL,
  val gotifyUrl: String = DEFAULT_GOTIFY_URL,
  val themeMode: ThemeMode = ThemeMode.LIGHT,
) {
  companion object {
    /** Mesmo endpoint usado pelo portal web: https://portal.bodasmel.com.br/api/pb */
    const val DEFAULT_PB_BASE_URL = "https://portal.bodasmel.com.br/api/pb"

    /** Servidor Gotify de produção (EasyPanel). */
    const val DEFAULT_GOTIFY_URL = "https://websites-gotify.qlnxj1.easypanel.host"
  }
}

/** Preferências não sensíveis (URLs, tema). Segredos ficam no [SecureStore]. */
class SettingsStore(private val context: Context) {

  private object Keys {
    val pbBaseUrl = stringPreferencesKey("pb_base_url")
    val gotifyUrl = stringPreferencesKey("gotify_url")
    val themeMode = stringPreferencesKey("theme_mode")
  }

  val settings: Flow<AppSettings> =
    context.dataStore.data.map { prefs ->
      AppSettings(
        pbBaseUrl = prefs[Keys.pbBaseUrl] ?: AppSettings.DEFAULT_PB_BASE_URL,
        gotifyUrl = (prefs[Keys.gotifyUrl] ?: "").trim().trimEnd('/'),
        themeMode =
          prefs[Keys.themeMode]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.LIGHT,
      )
    }

  suspend fun setPbBaseUrl(value: String) {
    val clean = value.trim().trimEnd('/')
    context.dataStore.edit { it[Keys.pbBaseUrl] = if (clean.isEmpty()) AppSettings.DEFAULT_PB_BASE_URL else clean }
  }

  suspend fun setGotifyUrl(value: String) {
    context.dataStore.edit { it[Keys.gotifyUrl] = value.trim().trimEnd('/') }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.themeMode] = mode.name }
  }
}
