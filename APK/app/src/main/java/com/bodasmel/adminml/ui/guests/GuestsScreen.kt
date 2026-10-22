package com.bodasmel.adminml.ui.guests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.core.Format
import com.bodasmel.adminml.data.RsvpRecord
import com.bodasmel.adminml.domain.GuestFilters
import com.bodasmel.adminml.domain.ParseUtils
import com.bodasmel.adminml.theme.PortalColors
import com.bodasmel.adminml.theme.StatusPalette
import com.bodasmel.adminml.ui.components.FilterDropdown
import com.bodasmel.adminml.ui.components.PaginationBar
import com.bodasmel.adminml.ui.components.StatusBadge
import kotlin.math.abs

@Composable
fun GuestsScreen(container: AppContainer) {
  val store = container.guestsStore
  val state by store.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
  val isWide = LocalConfiguration.current.screenWidthDp >= 840

  var showApproveAllDialog by remember { mutableStateOf(false) }

  LaunchedEffect(state.message) {
    val message = state.message ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    store.consumeMessage()
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (state.loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      state.error?.let { error ->
        Text(
          text = error,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // -------- Aprovar Todos --------
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
              text =
                "${state.pendentes.size} pendente${if (state.pendentes.size != 1) "s" else ""}",
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
                  modifier = Modifier.width(16.dp).height(16.dp),
                  strokeWidth = 2.dp,
                  color = Color.White,
                )
              } else {
                Text("Aprovar Todos")
              }
            }
          }
        }
      }

      // -------- Toolbar --------
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
          value = state.search,
          onValueChange = store::onSearchChange,
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text("Buscar por nome, responsável ou telefone…") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterDropdown(
            label = "Status",
            options = GuestFilters.STATUS_OPTIONS,
            selected = state.statusFilter,
            counts = state.statusCounts,
            onSelect = store::onStatusFilterChange,
            modifier = Modifier.weight(1f),
          )
          FilterDropdown(
            label = "Categoria",
            options = GuestFilters.CATEGORY_OPTIONS,
            selected = state.categoryFilter,
            counts = state.categoryCounts,
            onSelect = store::onCategoryFilterChange,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text = state.statsText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
      }

      // -------- Lista (swipe horizontal pagina, como no portal) --------
      Box(
        modifier =
          Modifier.fillMaxSize().pointerInput(state.totalPages, state.currentPage) {
            var dragTotal = 0f
            detectHorizontalDragGestures(
              onDragStart = { dragTotal = 0f },
              onDragEnd = {
                if (abs(dragTotal) > 120f) {
                  if (dragTotal < 0) store.nextPage() else store.previousPage()
                }
              },
              onHorizontalDrag = { change, amount -> dragTotal += amount; change.consume() },
            )
          }
      ) {
        if (state.filtered.isEmpty() && !state.loading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum convidado encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          if (isWide) {
            GuestsTable(state.pageItems, dark) { store.toggleStatus(it) }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(state.pageItems, key = { it.id }) { item ->
                GuestCard(item = item, dark = dark, onToggle = { store.toggleStatus(item) })
              }
            }
          }
        }
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
      text = { Text("Confirmar ${state.pendentes.size} convidados pendentes?") },
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

@Composable
private fun GuestCard(item: RsvpRecord, dark: Boolean, onToggle: () -> Unit) {
  val restricoes = ParseUtils.parseRestricoes(item.restricoes)
  val hasNecessidades = ParseUtils.hasNecessidades(restricoes)
  val outras = ParseUtils.outrasRestricoes(restricoes)
  val isCrianca = item.criancas == "1"
  val isAdulto = item.adulto == "1"

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          if (isCrianca && !item.responsavel.isNullOrBlank()) {
            Text(
              text = "Nome do responsável: ${item.responsavel}",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium,
            )
          } else if (isAdulto) {
            Text(
              text = "Nome do convidado(a): ${item.nome ?: "-"}",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium,
            )
          } else {
            Text(
              text = item.nome ?: "-",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
          Text(
            text = "#${item.sheetRowId?.toInt() ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusBadge(status = item.normalizedStatus, dark = dark, onClick = onToggle)
      }

      Spacer(Modifier.height(8.dp))

      if (isCrianca && !item.nomeCrianca.isNullOrBlank()) {
        GuestRow("Nome da criança / adolescente:", item.nomeCrianca)
      }
      if (!item.idade.isNullOrBlank()) GuestRow("Idade:", item.idade)
      if (isAdulto) GuestRow("Adulto", "")
      if (!item.contato.isNullOrBlank()) GuestRow("Contato:", item.contato)
      if (outras.isNotEmpty()) GuestRow("Restrições:", outras.joinToString(", "))
      if (hasNecessidades) {
        Text(
          text = "Necessidades Especiais: Sim",
          style = MaterialTheme.typography.bodySmall,
          color = StatusPalette.dangerFg(dark),
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(Modifier.height(8.dp))
      Text(
        text = "Data: ${Format.dateTime(item.created)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun GuestRow(label: String, value: String) {
  Row(modifier = Modifier.padding(vertical = 1.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
    )
    if (value.isNotEmpty()) {
      Text(text = " $value", style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun GuestsTable(items: List<RsvpRecord>, dark: Boolean, onToggle: (RsvpRecord) -> Unit) {
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
          .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      HeaderCell("#", Modifier.weight(0.06f))
      HeaderCell("Nome / Responsável", Modifier.weight(0.26f))
      HeaderCell("Tipo", Modifier.weight(0.10f))
      HeaderCell("Idade", Modifier.weight(0.08f))
      HeaderCell("Criança", Modifier.weight(0.16f))
      HeaderCell("Restrições", Modifier.weight(0.16f))
      HeaderCell("Contato", Modifier.weight(0.12f))
      HeaderCell("Data", Modifier.weight(0.14f))
      HeaderCell("Status", Modifier.weight(0.12f))
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(items, key = { it.id }) { item ->
        val restricoes = ParseUtils.parseRestricoes(item.restricoes)
        val hasNecessidades = ParseUtils.hasNecessidades(restricoes)
        val outras = ParseUtils.outrasRestricoes(restricoes)
        Row(
          modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Cell(item.sheetRowId?.toInt()?.toString() ?: "-", Modifier.weight(0.06f))
          Cell(item.responsavel ?: item.nome ?: "-", Modifier.weight(0.26f), bold = true)
          Cell(
            if (item.adulto == "1") "Adulto" else if (item.criancas == "1") "Criança" else "-",
            Modifier.weight(0.10f),
          )
          Cell(item.idade ?: "-", Modifier.weight(0.08f))
          Cell(
            if (item.criancas == "1" && !item.nomeCrianca.isNullOrBlank()) item.nomeCrianca else "-",
            Modifier.weight(0.16f),
          )
          Column(modifier = Modifier.weight(0.16f)) {
            if (outras.isNotEmpty()) {
              Text(outras.joinToString(", "), fontSize = 12.sp, maxLines = 3)
            }
            if (hasNecessidades) {
              Text(
                "Necessidades Especiais",
                fontSize = 12.sp,
                color = StatusPalette.dangerFg(dark),
                fontWeight = FontWeight.SemiBold,
              )
            }
            if (outras.isEmpty() && !hasNecessidades) {
              Text("-", fontSize = 12.sp)
            }
          }
          Cell(item.contato ?: "-", Modifier.weight(0.12f))
          Cell(Format.dateTime(item.created), Modifier.weight(0.14f))
          Box(modifier = Modifier.weight(0.12f)) {
            StatusBadge(status = item.normalizedStatus, dark = dark, onClick = { onToggle(item) })
          }
        }
        HorizontalDivider()
      }
    }
  }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onPrimaryContainer,
  )
}

@Composable
private fun Cell(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
  Text(
    text = text,
    modifier = modifier,
    fontSize = 12.sp,
    fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    maxLines = 3,
  )
}

@Composable
private fun HorizontalDivider() {
  androidx.compose.material3.HorizontalDivider(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}

private fun Color.luminanceIsDark(): Boolean =
  (0.299 * red + 0.587 * green + 0.114 * blue) < 0.5
