package ru.school.healthmonitor.export

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Минимальный writer XLSX без внешних зависимостей.
 * Формирует валидный .xlsx с одним листом «данные», в котором:
 *   строка 1 — коды полей (region, school, grade, class, name1, ...)
 *   строка 2 — русские подписи
 *   строки 3..N — данные детей (по одной строке на ребёнка)
 *
 * Это тот же формат, что в файлах вашего архива макетов.
 */
object XlsxWriter {

    data class Sheet(
        val headerCodes: List<String>,   // строка 1
        val headerLabels: List<String>,  // строка 2
        val rows: List<List<String>>     // строки данных
    )

    fun write(target: File, sheet: Sheet) {
        target.parentFile?.mkdirs()
        target.outputStream().use { write(it, sheet) }
    }

    fun write(out: OutputStream, sheet: Sheet) {
        val strings = mutableListOf<String>()
        val stringIndex = HashMap<String, Int>()
        fun s(v: String): Int = stringIndex.getOrPut(v) { strings.add(v); strings.size - 1 }

        val allRows: List<List<Int>> = buildList {
            add(sheet.headerCodes.map { s(it) })
            add(sheet.headerLabels.map { s(it) })
            addAll(sheet.rows.map { r -> r.map { s(it) } })
        }

        ZipOutputStream(out).use { zip ->
            zip.putEntry("[Content_Types].xml", contentTypes())
            zip.putEntry("_rels/.rels", rootRels())
            zip.putEntry("xl/_rels/workbook.xml.rels", workbookRels())
            zip.putEntry("xl/workbook.xml", workbookXml())
            zip.putEntry("xl/styles.xml", stylesXml())
            zip.putEntry("xl/sharedStrings.xml", sharedStringsXml(strings))
            zip.putEntry("xl/worksheets/sheet1.xml", sheetXml(allRows))
        }
    }

    private fun ZipOutputStream.putEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="данные" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf/></cellStyleXfs>
<cellXfs count="1"><xf/></cellXfs>
</styleSheet>"""

    private fun sharedStringsXml(strings: List<String>): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="""").append(strings.size).append("""" uniqueCount="""").append(strings.size).append("""">""")
        for (s in strings) {
            sb.append("<si><t xml:space=\"preserve\">").append(escape(s)).append("</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun sheetXml(rows: List<List<Int>>): String {
        val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIndex, row ->
            val rowNum = rIndex + 1
            sb.append("<row r=\"").append(rowNum).append("\">")
            row.forEachIndexed { cIndex, si ->
                val ref = colRef(cIndex) + rowNum
                sb.append("<c r=\"").append(ref).append("\" t=\"s\"><v>").append(si).append("</v></c>")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colRef(idx: Int): String {
        var n = idx
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
            if (n < 0) break
        }
        return sb.toString()
    }

    private fun escape(s: String): String = buildString(s.length) {
        for (ch in s) when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(ch)
        }
    }
}
