package com.bodasmel.adminml.ui.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Gerador de XLSX mínimo (OOXML) — evita Apache POI, que é pesado no Android.
 * Cada planilha é uma aba com cabeçalho em negrito/fundo verde e larguras de coluna.
 */
object XlsxWriter {

  data class Sheet(val name: String, val headers: List<String>, val rows: List<List<String>>, val widths: List<Int>)

  fun build(sheets: List<Sheet>): ByteArray {
    require(sheets.isNotEmpty()) { "Nenhuma planilha para gerar." }

    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
      zip.putEntry("[Content_Types].xml", contentTypes(sheets.size))
      zip.putEntry("_rels/.rels", rootRels())
      zip.putEntry("xl/workbook.xml", workbook(sheets))
      zip.putEntry("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
      zip.putEntry("xl/styles.xml", styles())
      sheets.forEachIndexed { index, sheet ->
        zip.putEntry("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
      }
    }
    return out.toByteArray()
  }

  private fun ZipOutputStream.putEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
  }

  private fun contentTypes(sheetCount: Int): String {
    val overrides =
      (1..sheetCount).joinToString("") {
        """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
      }
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$overrides
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
  }

  private fun rootRels(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private fun workbook(sheets: List<Sheet>): String {
    val entries =
      sheets.mapIndexed { index, sheet ->
        """<sheet name="${escape(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
      }.joinToString("")
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$entries</sheets>
</workbook>"""
  }

  private fun workbookRels(sheetCount: Int): String {
    val entries =
      (1..sheetCount).joinToString("") {
        """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>"""
      } +
        """<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$entries
</Relationships>"""
  }

  private fun styles(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF059669"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
</styleSheet>"""

  private fun sheetXml(sheet: Sheet): String {
    val cols =
      sheet.widths.mapIndexed { index, width ->
        """<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>"""
      }.joinToString("")

    val headerCells =
      sheet.headers.mapIndexed { index, header ->
        cell(reference(index, 1), header, style = 1)
      }.joinToString("")

    val bodyRows =
      sheet.rows.mapIndexed { rowIndex, row ->
        val cells =
          row.mapIndexed { columnIndex, value ->
            cell(reference(columnIndex, rowIndex + 2), value, style = 0)
          }.joinToString("")
        """<row r="${rowIndex + 2}">$cells</row>"""
      }.joinToString("")

    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols>$cols</cols>
<sheetData><row r="1">$headerCells</row>$bodyRows</sheetData>
</worksheet>"""
  }

  private fun cell(reference: String, value: String, style: Int): String {
    val text = escape(value)
    return """<c r="$reference" t="inlineStr" s="$style"><is><t xml:space="preserve">$text</t></is></c>"""
  }

  private fun reference(columnIndex: Int, rowNumber: Int): String {
    var index = columnIndex
    val builder = StringBuilder()
    do {
      builder.insert(0, ('A' + (index % 26)))
      index = index / 26 - 1
    } while (index >= 0)
    return "$builder$rowNumber"
  }

  private fun escape(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
      .replace("\n", " ")
}
