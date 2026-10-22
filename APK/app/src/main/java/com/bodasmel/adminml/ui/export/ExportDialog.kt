package com.bodasmel.adminml.ui.export

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bodasmel.adminml.AppContainer
import com.bodasmel.adminml.domain.GuestFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportDialog(container: AppContainer, onDismiss: () -> Unit) {
  val guestsState = container.guestsStore.state.value
  val messagesState = container.messagesStore.state.value

  var includeGuests by remember { mutableStateOf(guestsState.filtered.isNotEmpty()) }
  var includeMessages by remember { mutableStateOf(messagesState.filtered.isNotEmpty()) }
  var status by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Exportar Dados") },
    text = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = includeGuests, onCheckedChange = { includeGuests = it })
          Text("Convidados (${guestsState.filtered.size} registros)")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = includeMessages, onCheckedChange = { includeMessages = it })
          Text("Mural (${messagesState.filtered.size} mensagens)")
        }
        status?.let {
          Text(
            text = it,
            modifier = Modifier.padding(top = 8.dp),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Button(
          enabled = !busy && (includeGuests || includeMessages),
          onClick = {
            busy = true
            scope.launch {
              val message = exportPdf(context, container, includeGuests, includeMessages)
              status = message
              busy = false
            }
          },
        ) { Text("PDF") }

        Button(
          enabled = !busy && (includeGuests || includeMessages),
          onClick = {
            busy = true
            scope.launch {
              val message = exportXlsx(context, container, includeGuests, includeMessages)
              status = message
              busy = false
            }
          },
        ) { Text("Excel") }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

private suspend fun exportXlsx(
  context: android.content.Context,
  container: AppContainer,
  includeGuests: Boolean,
  includeMessages: Boolean,
): String =
  withContext(Dispatchers.IO) {
    val guestsState = container.guestsStore.state.value
    val messagesState = container.messagesStore.state.value
    val sheets = mutableListOf<XlsxWriter.Sheet>()

    if (includeGuests && guestsState.filtered.isNotEmpty()) {
      sheets +=
        XlsxWriter.Sheet(
          name = "Convidados",
          headers = Exporter.GUEST_HEADERS,
          rows = Exporter.guestRows(guestsState.filtered),
          widths = Exporter.GUEST_WIDTHS,
        )
    }
    if (includeMessages && messagesState.filtered.isNotEmpty()) {
      sheets +=
        XlsxWriter.Sheet(
          name = "Mural",
          headers = Exporter.MESSAGE_HEADERS,
          rows = Exporter.messageRows(messagesState.filtered),
          widths = Exporter.MESSAGE_WIDTHS,
        )
    }

    if (sheets.isEmpty()) return@withContext "Nenhum registro para exportar."

    val bytes = XlsxWriter.build(sheets)
    val (xlsxName, _) = Exporter.fileNames(includeGuests, includeMessages, 0, 0)
    val uri = Exporter.saveToDownloads(context, xlsxName, XLSX_MIME, bytes)
      ?: return@withContext "Não foi possível gravar o arquivo."

    runCatching {
      context.startActivity(
        Exporter.openIntent(uri, XLSX_MIME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }
    "Excel exportado: ${guestsState.filtered.size + messagesState.filtered.size} registro(s)!"
  }

private suspend fun exportPdf(
  context: android.content.Context,
  container: AppContainer,
  includeGuests: Boolean,
  includeMessages: Boolean,
): String =
  withContext(Dispatchers.IO) {
    val guestsState = container.guestsStore.state.value
    val messagesState = container.messagesStore.state.value
    val tables = mutableListOf<PdfExporter.Table>()

    if (includeGuests && guestsState.filtered.isNotEmpty()) {
      val statusLabel =
        GuestFilters.STATUS_OPTIONS.firstOrNull { it.first == guestsState.statusFilter }?.second
          ?: "Todos"
      val categoryLabel =
        GuestFilters.CATEGORY_OPTIONS.firstOrNull { it.first == guestsState.categoryFilter }?.second
          ?: "Todas as categorias"
      tables +=
        PdfExporter.Table(
          title = "Convidados — Bodas M&L",
          subtitle =
            Exporter.filterLabelGuests(statusLabel, categoryLabel, guestsState.filtered.size),
          footerInfo = Exporter.exportedAtLabel(),
          headers = Exporter.GUEST_HEADERS,
          rows = Exporter.guestRows(guestsState.filtered),
          columnWidthsMm = listOf(12f, 45f, 18f, 15f, 40f, 55f, 35f, 32f, 20f),
          headerColor = 0xFF059669.toInt(),
        )
    }

    if (includeMessages && messagesState.filtered.isNotEmpty()) {
      val filterLabel =
        when (messagesState.filter) {
          MessageRecordStatus.PENDENTE -> "Pendentes"
          MessageRecordStatus.APROVADO -> "Aprovadas"
          MessageRecordStatus.REJEITADO -> "Rejeitadas"
          else -> "Todas"
        }
      tables +=
        PdfExporter.Table(
          title = "Mensagens do Mural — Bodas M&L",
          subtitle =
            Exporter.filterLabelMessages(filterLabel, messagesState.filtered.size),
          footerInfo = Exporter.exportedAtLabel(),
          headers = Exporter.MESSAGE_HEADERS,
          rows = Exporter.messageRows(messagesState.filtered),
          columnWidthsMm = listOf(12f, 35f, 155f, 38f, 25f),
          headerColor = 0xFF6366F1.toInt(),
        )
    }

    if (tables.isEmpty()) return@withContext "Nenhum registro para exportar."

    val bytes = PdfExporter.build(tables, Exporter.exportedAtLabel())
    val (_, pdfName) = Exporter.fileNames(includeGuests, includeMessages, 0, 0)
    val uri = Exporter.saveToDownloads(context, pdfName, PDF_MIME, bytes)
      ?: return@withContext "Não foi possível gravar o arquivo."

    runCatching {
      context.startActivity(Exporter.openIntent(uri, PDF_MIME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    "PDF exportado: ${guestsState.filtered.size + messagesState.filtered.size} registro(s)!"
  }

private const val XLSX_MIME =
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val PDF_MIME = "application/pdf"

private object MessageRecordStatus {
  const val PENDENTE = "Pendente"
  const val APROVADO = "Aprovado"
  const val REJEITADO = "Rejeitado"
}
