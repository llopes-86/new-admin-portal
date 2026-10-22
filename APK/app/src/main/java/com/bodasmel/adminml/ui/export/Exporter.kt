package com.bodasmel.adminml.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.bodasmel.adminml.core.Format
import com.bodasmel.adminml.data.MessageRecord
import com.bodasmel.adminml.data.RsvpRecord
import com.bodasmel.adminml.domain.ParseUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Monta as linhas de exportação (paridade com `public/js/features/export.js`)
 * e grava o arquivo em Downloads.
 */
object Exporter {

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

  val GUEST_HEADERS =
    listOf("#", "Nome / Responsável", "Tipo", "Idade", "Nome Da Criança", "Restrições", "Contato", "Data", "Status")
  val GUEST_WIDTHS = listOf(6, 22, 10, 8, 22, 22, 18, 12, 10)

  val MESSAGE_HEADERS = listOf("#", "Nome", "Mensagem", "Data", "Status")
  val MESSAGE_WIDTHS = listOf(6, 20, 50, 14, 10)

  fun guestRows(items: List<RsvpRecord>): List<List<String>> =
    items.map { item ->
      val restricoes = ParseUtils.parseRestricoes(item.restricoes)
      val tipo = if (item.adulto == "1") "Adulto" else if (item.criancas == "1") "Criança" else "-"
      listOf(
        item.sheetRowId?.toInt()?.toString() ?: "-",
        item.responsavel ?: item.nome ?: "-",
        tipo,
        item.idade ?: "-",
        item.nomeCrianca ?: "-",
        if (restricoes.isNotEmpty()) restricoes.joinToString(", ") else (item.restricoes ?: "-"),
        item.contato ?: "-",
        Format.dateTime(item.created),
        item.normalizedStatus,
      )
    }

  fun messageRows(items: List<MessageRecord>): List<List<String>> =
    items.map { message ->
      listOf(
        message.sheetRowId?.toInt()?.toString() ?: "-",
        message.nome ?: "Anônimo",
        message.mensagem ?: "-",
        Format.dateTime(message.created),
        message.normalizedStatus,
      )
    }

  fun exportedAtLabel(): String {
    val now = LocalDateTime.now()
    return "Exportado em: ${now.format(dateFormatter)} às ${now.format(timeFormatter)}"
  }

  fun filterLabelGuests(statusLabel: String, categoryLabel: String, count: Int): String =
    "Filtros: $statusLabel | $categoryLabel | $count registros"

  fun filterLabelMessages(filterLabel: String, count: Int): String = "Filtro: $filterLabel | $count mensagens"

  /** Grava bytes em Downloads e devolve o Uri para abrir/compartilhar. */
  fun saveToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      saveViaMediaStore(context, fileName, mimeType, bytes)
    } else {
      saveLegacy(context, fileName, bytes)
    }
  }

  private fun saveViaMediaStore(
    context: Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
  ): Uri? =
    runCatching {
        val resolver = context.contentResolver
        val values =
          ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
          }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) return null
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
      }
      .getOrNull()

  @Suppress("DEPRECATION")
  private fun saveLegacy(context: Context, fileName: String, bytes: ByteArray): Uri? =
    runCatching {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }
      .getOrNull()

  fun openIntent(uri: Uri, mimeType: String): Intent =
    Intent(Intent.ACTION_VIEW)
      .setDataAndType(uri, mimeType)
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

  fun fileNames(guests: Boolean, messages: Boolean, guestCount: Int, messageCount: Int): Pair<String, String> {
    val stamp = Format.stamp()
    return when {
      guests && messages -> "dados-bodas-ml-${stamp}.xlsx" to "dados-bodas-ml-${stamp}.pdf"
      guests -> "convidados-bodas-ml-$stamp.xlsx" to "convidados-bodas-ml-$stamp.pdf"
      else -> "mensagens-mural-ml-$stamp.xlsx" to "mensagens-mural-ml-$stamp.pdf"
    }
  }
}
