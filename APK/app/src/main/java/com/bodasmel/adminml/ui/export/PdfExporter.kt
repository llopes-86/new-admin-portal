package com.bodasmel.adminml.ui.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/**
 * Gerador de PDF (A4 paisagem) com o mesmo visual do `jsPDF + autotable` do portal:
 * título, linha de filtros, data de exportação, tabela com cabeçalho colorido,
 * linhas alternadas e rodapé com numeração de páginas.
 */
object PdfExporter {

  // Dimensões A4 em pontos (72 dpi)
  private const val PAGE_WIDTH = 842
  private const val PAGE_HEIGHT = 595

  private const val MARGIN_LEFT = 40f // ~14 mm
  private const val MARGIN_RIGHT = 40f
  private const val MARGIN_TOP = 90f // ~32 mm
  private const val MARGIN_BOTTOM = 60f

  data class Table(
    val title: String,
    val subtitle: String,
    val footerInfo: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val columnWidthsMm: List<Float>,
    val headerColor: Int,
  )

  fun build(tables: List<Table>, exportedAt: String): ByteArray {
    val document = PdfDocument()
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var pageCanvas: Canvas? = null
    var startY = 0f

    val titlePaint = Paint().apply { isAntiAlias = true; textSize = 14f; color = Color.rgb(30, 41, 59) }
    val subtitlePaint = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.rgb(100, 116, 139) }
    val headerPaint =
      Paint().apply {
        isAntiAlias = true
        textSize = 8f
        color = Color.WHITE
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
      }
    val cellPaint = Paint().apply { isAntiAlias = true; textSize = 8f; color = Color.rgb(30, 41, 59) }
    val footerPaint = Paint().apply { isAntiAlias = true; textSize = 8f; color = Color.rgb(148, 163, 184) }

    val available = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    fun drawFooter(canvas: Canvas, number: Int) {
      canvas.drawText(exportedAt, MARGIN_LEFT, PAGE_HEIGHT - 22f, footerPaint)
      footerPaint.textAlign = Paint.Align.RIGHT
      canvas.drawText("Página $number", PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 22f, footerPaint)
      footerPaint.textAlign = Paint.Align.LEFT
    }

    fun newPage() {
      page?.let { document.finishPage(it) }
      pageNumber += 1
      page = document.startPage(
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      )
      pageCanvas = page!!.getCanvas()
      drawFooter(pageCanvas!!, pageNumber)
      startY = MARGIN_TOP
    }

    tables.forEach { table ->
      newPage()
      pageCanvas!!.drawText(table.title, MARGIN_LEFT, 40f, titlePaint)
      pageCanvas!!.drawText(table.subtitle, MARGIN_LEFT, 58f, subtitlePaint)
      pageCanvas!!.drawText(table.footerInfo, MARGIN_LEFT, 72f, subtitlePaint)
      startY = MARGIN_TOP

      // Escala das larguras informadas para a área útil da página
      val totalMm = table.columnWidthsMm.sum().toFloat().coerceAtLeast(1f)
      val widths = table.columnWidthsMm.map { (it / totalMm) * available }
      val headerHeight = 22f

      fun drawHeader() {
        pageCanvas!!.drawRect(
          MARGIN_LEFT,
          startY,
          MARGIN_LEFT + available,
          startY + headerHeight,
          Paint().apply { color = table.headerColor },
        )
        var x = MARGIN_LEFT
        table.headers.forEachIndexed { index, header ->
          val w = widths.getOrElse(index) { 0f }
          drawCenteredText(pageCanvas!!, header, x, startY, w, headerHeight, headerPaint)
          x += w
        }
        startY += headerHeight
      }

      drawHeader()

      table.rows.forEachIndexed { rowIndex, row ->
        val cells =
          row.mapIndexed { columnIndex, value ->
            wrapText(value, widths.getOrElse(columnIndex) { 100f }, cellPaint)
          }
        val maxLines = cells.maxOfOrNull { it.size } ?: 1
        val rowHeight = maxLines * 10f + 8f

        if (startY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
          newPage()
          drawHeader()
        }

        if (rowIndex % 2 == 1) {
          pageCanvas!!.drawRect(
            MARGIN_LEFT,
            startY,
            MARGIN_LEFT + available,
            startY + rowHeight,
            Paint().apply { color = Color.rgb(245, 247, 250) },
          )
        }

        var x = MARGIN_LEFT
        cells.forEachIndexed { index, lines ->
          val w = widths.getOrElse(index) { 0f }
          lines.forEachIndexed { lineIndex, line ->
            pageCanvas!!.drawText(line, x + 3f, startY + 12f + lineIndex * 10f, cellPaint)
          }
          x += w
        }

        val border = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 0.6f }
        pageCanvas!!.drawLine(
          MARGIN_LEFT,
          startY + rowHeight,
          MARGIN_LEFT + available,
          startY + rowHeight,
          border,
        )
        startY += rowHeight
      }
    }

    page?.let { document.finishPage(it) }
    val bytes = ByteArrayOutputStream()
    document.writeTo(bytes)
    document.close()
    return bytes.toByteArray()
  }

  private fun drawCenteredText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: Paint,
  ) {
    val textWidth = paint.measureText(text)
    canvas.drawText(text, x + (width - textWidth) / 2f, y + height / 2f + 3f, paint)
  }

  /** Quebra o texto em linhas que cabem na largura da coluna. */
  private fun wrapText(text: String, columnWidth: Float, paint: Paint): List<String> {
    if (text.isBlank()) return listOf("")
    val maxWidth = columnWidth.coerceAtLeast(20f)
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = StringBuilder()

    fun flush() {
      if (current.isNotEmpty()) {
        lines.add(current.toString())
        current = StringBuilder()
      }
    }

    words.forEach { word ->
      val candidate = if (current.isEmpty()) word else "$current $word"
      if (paint.measureText(candidate) <= maxWidth - 6f) {
        current = StringBuilder(candidate)
      } else {
        flush()
        if (paint.measureText(word) <= maxWidth - 6f) {
          current = StringBuilder(word)
        } else {
          var chunk = ""
          word.forEach { char ->
            if (paint.measureText(chunk + char) > maxWidth - 6f) {
              lines.add(chunk)
              chunk = char.toString()
            } else {
              chunk += char
            }
          }
          current = StringBuilder(chunk)
        }
      }
    }
    flush()
    return lines.ifEmpty { listOf("") }
  }
}
