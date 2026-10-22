package com.bodasmel.adminml.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.core.ThemeMode
import com.bodasmel.adminml.push.GotifyService
import com.bodasmel.adminml.push.Notifications
import com.bodasmel.adminml.ui.export.ExportDialog
import com.bodasmel.adminml.ui.guests.GuestsScreen
import com.bodasmel.adminml.ui.messages.MessagesScreen
import com.bodasmel.adminml.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class HomeTab(val title: String) {
  GUESTS("Lista de convidados"),
  MESSAGES("Mensagens do mural"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  container: AppContainer,
  openGuests: Boolean = false,
  onRequestNotificationPermission: () -> Unit,
) {
  var tab by rememberSaveable { mutableStateOf(HomeTab.GUESTS) }
  var showSettings by rememberSaveable { mutableStateOf(false) }
  var showExport by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(openGuests) {
    if (openGuests) tab = HomeTab.GUESTS
  }

  val settings by container.settings.collectAsStateWithLifecycle()
  val session = container.session
  val user = session.currentUser
  val dark = settings.themeMode == ThemeMode.DARK

  if (showSettings) {
    SettingsScreen(
      container = container,
      onBack = { showSettings = false },
    )
    return
  }

  val screenWidth = LocalConfiguration.current.screenWidthDp
  val useRail = screenWidth >= 600

  val context = LocalContext.current
  LaunchedEffect(settings.gotifyUrl) {
    Notifications.createChannels(context)
    onRequestNotificationPermission()
    if (settings.gotifyUrl.isNotBlank() && container.secureStore.gotifyClientToken != null) {
      GotifyService.start(context)
    }
  }

  // Carrega as listagens quando entra no painel (a API exige autenticação).
  LaunchedEffect(Unit) {
    container.guestsStore.load()
    container.messagesStore.load()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            user?.email?.let {
              Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        },
        actions = {
          IconButton(onClick = { container.appScope.launch { container.settingsStore.setThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } }) {
            Icon(
              imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
              contentDescription = if (dark) "Mudar para tema claro" else "Mudar para tema escuro",
            )
          }
          IconButton(onClick = { showSettings = true }) {
            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    bottomBar = {
      if (!useRail) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
          NavigationBarItem(
            selected = tab == HomeTab.GUESTS,
            onClick = { tab = HomeTab.GUESTS },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Convidados") },
          )
          NavigationBarItem(
            selected = tab == HomeTab.MESSAGES,
            onClick = { tab = HomeTab.MESSAGES },
            icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
            label = { Text("Mural") },
          )
          NavigationBarItem(
            selected = false,
            onClick = { showExport = true },
            icon = { Icon(Icons.Filled.Download, contentDescription = null) },
            label = { Text("Exportar") },
          )
          NavigationBarItem(
            selected = false,
            onClick = { session.logout() },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
            label = { Text("Sair") },
          )
        }
      }
    },
  ) { padding ->
    Row(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (useRail) {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
          NavigationRailItem(
            selected = tab == HomeTab.GUESTS,
            onClick = { tab = HomeTab.GUESTS },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Convidados") },
          )
          NavigationRailItem(
            selected = tab == HomeTab.MESSAGES,
            onClick = { tab = HomeTab.MESSAGES },
            icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
            label = { Text("Mural") },
          )
          NavigationRailItem(
            selected = false,
            onClick = { showExport = true },
            icon = { Icon(Icons.Filled.Download, contentDescription = null) },
            label = { Text("Exportar") },
          )
          NavigationRailItem(
            selected = false,
            onClick = { session.logout() },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
            label = { Text("Sair") },
          )
        }
      }
      Box(modifier = Modifier.fillMaxSize()) {
        when (tab) {
          HomeTab.GUESTS -> GuestsScreen(container = container)
          HomeTab.MESSAGES -> MessagesScreen(container = container)
        }
      }
    }
  }

  if (showExport) {
    ExportDialog(container = container, onDismiss = { showExport = false })
  }
}

@Composable
internal fun UserAvatar(initial: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      fontWeight = FontWeight.Bold,
    )
  }
}
