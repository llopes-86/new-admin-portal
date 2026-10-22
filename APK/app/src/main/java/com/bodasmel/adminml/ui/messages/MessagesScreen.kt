package com.bodasmel.adminml.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.core.Format
import com.bodasmel.adminml.data.MessageRecord
import com.bodasmel.adminml.theme.InterFamily
import com.bodasmel.adminml.theme.PortalColors
import com.bodasmel.adminml.theme.StatusPalette
import com.bodasmel.adminml.ui.components.FilterDropdown
import com.bodasmel.adminml.ui.components.PageIndicator
import com.bodasmel.adminml.ui.components.PaginationBar
import com.bodasmel.adminml.ui.components.StatusBadge
import com.bodasmel.adminml.ui.components.swipeToPaginate

private val FILTER_OPTIONS =
  listOf(
    "all" to "Todas",
    MessageRecord.STATUS_PENDENTE to "Pendentes",
    MessageRecord.STATUS_APROVADO to "Aprovadas",
    MessageRecord.STATUS_REJEITADO to "Rejeitadas",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesScreen(container: AppContainer) {
  val store = container.messagesStore
  val state by store.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val dark = MaterialTheme.colorScheme.background.let {
    (0.299 * it.red + 0.587 * it.green + 0.114 * it.blue) < 0.5
  }
  var showApproveAllDialog by remember { mutableStateOf(false) }

  LaunchedEffect(state.message) {
    val message = state.message ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    store.consumeMessage()
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

      state.error?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // ===== Conteúdo que rola junto com a lista (como no portal web) =====
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
          if (state.pendentes.isNotEmpty()) {
            Card(
              modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${state.pendentes.size} pendente${if (state.pendentes.size != 1) "s" else ""}",
                  modifier = Modifier.weight(1f),
                  style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                  onClick = { showApproveAllDialog = true },
                  enabled = !state.busy,
                  colors = ButtonDefaults.buttonColors(containerColor = PortalColors.Gold),
                ) {
                  if (state.busy) {
                    CircularProgressIndicator(
                      modifier = Modifier.height(16.dp).widthIn(max = 16.dp),
                      strokeWidth = 2.dp,
                      color = Color.White,
                    )
                  } else {
                    Text("Aprovar Todas")
                  }
                }
              }
            }
          }

          // -------- Filtro (rola junto com a lista, como na web) --------
          FilterDropdown(
            label = "Filtro",
            options = FILTER_OPTIONS,
            selected = state.filter,
            counts = state.counts,
            onSelect = store::onFilterChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          )

          Spacer(Modifier.height(8.dp))
          Text(
            text = state.statsText,
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
          Spacer(Modifier.height(12.dp))

          // -------- Lista com swipe para paginar (como na web) --------
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .swipeToPaginate(
                  currentPage = state.currentPage,
                  totalPages = state.totalPages,
                  onPage = store::goToPage,
                )
          ) {
            if (state.filtered.isEmpty() && !state.loading) {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text("Nenhuma mensagem encontrada", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            } else {
              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                state.pageItems.forEach { message ->
                  MessageCard(
                    message = message,
                    dark = dark,
                    onApprove = { store.setStatus(message, MessageRecord.STATUS_APROVADO) },
                    onReject = { store.setStatus(message, MessageRecord.STATUS_REJEITADO) },
                    onPending = { store.setStatus(message, MessageRecord.STATUS_PENDENTE) },
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(16.dp))
        }

        PageIndicator(currentPage = state.currentPage, totalPages = state.totalPages)
      }

      PaginationBar(
        currentPage = state.currentPage,
        totalPages = state.totalPages,
        onPage = store::goToPage,
      )
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
  }

  if (showApproveAllDialog) {
    AlertDialog(
      onDismissRequest = { showApproveAllDialog = false },
      title = { Text("Confirmar ação") },
      text = { Text("Aprovar ${state.pendentes.size} mensagens pendentes?") },
      confirmButton = {
        TextButton(
          onClick = {
            showApproveAllDialog = false
            store.approveAll()
          }
        ) { Text("Confirmar") }
      },
      dismissButton = { TextButton(onClick = { showApproveAllDialog = false }) { Text("Cancelar") } },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
  message: MessageRecord,
  dark: Boolean,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onPending: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = message.nome ?: "Anônimo",
          modifier = Modifier.weight(1f),
          fontFamily = InterFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = Format.dateTime(message.created),
          fontFamily = InterFamily,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Text(
        text = message.mensagem ?: "",
        fontFamily = InterFamily,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )

      StatusBadge(status = message.normalizedStatus, dark = dark)

      // Mesma disposicao da web (.btn-group com .btn-approve/.btn-reject/.btn-pending)
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
      ) {
        ActionButton(
          text = "Aprovar",
          color = PortalColors.LightSuccess,
          onClick = onApprove,
          modifier = Modifier.weight(1f).widthIn(min = 96.dp),
        )
        ActionButton(
          text = "Rejeitar",
          color = PortalColors.LightDanger,
          onClick = onReject,
          modifier = Modifier.weight(1f).widthIn(min = 96.dp),
        )
        ActionButton(
          text = "Pendente",
          color = PortalColors.LightWarning,
          onClick = onPending,
          modifier = Modifier.weight(1f).widthIn(min = 96.dp),
        )
      }
    }
  }
}

/** Botao de acao com o mesmo visual da web (cor solida, texto branco, raio 8dp). */
@Composable
private fun ActionButton(
  text: String,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    modifier = modifier.height(36.dp),
    shape = RoundedCornerShape(8.dp),
    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
  ) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
  }
}
