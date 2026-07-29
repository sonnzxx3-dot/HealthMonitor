package ru.school.healthmonitor.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import ru.school.healthmonitor.data.Child
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.data.SchoolClass
import ru.school.healthmonitor.domain.Anketa
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.domain.Field
import ru.school.healthmonitor.domain.FieldType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    /**
     * Формирует XLSX по одной анкете для одного класса. Формат — как в макетах:
     * лист «данные», строка 1 = коды полей (region, school, grade, class, name1, ..., затем поля анкеты),
     * строка 2 = русские подписи, далее — по строке на ребёнка.
     */
    fun exportAnketaXlsx(context: Context, sc: SchoolClass, anketa: Anketa): File {
        val repo = Repository.get(context)
        val children = repo.childrenOfClass(sc.id)

        val commonCodes = listOf("region", "school", "grade", "class", "name1", "name2", "name3", "bdate", "sex")
        val commonLabels = listOf(
            "Регион", "Школа", "Класс (уровень)", "Класс (с литерой)",
            "Ф", "И", "О", "Дата рожд.", "Пол (1=м, 2=ж)"
        )
        val fieldCodes = anketa.fields.map { it.code }
        val fieldLabels = anketa.fields.map { it.label }

        val headerCodes = commonCodes + fieldCodes
        val headerLabels = commonLabels + fieldLabels

        val rows = children.map { child ->
            val submission = repo.submissionFor(child.id, anketa.id)
            val values = submission?.values ?: emptyMap()
            val common = listOf(
                sc.region, sc.school, sc.grade.toString(), sc.letter,
                child.name1, child.name2, child.name3, child.bdate, child.sex.toString()
            )
            val fieldValues = anketa.fields.map { values[it.code] ?: "" }
            common + fieldValues
        }

        val out = exportsDir(context).resolve(
            "Anketa_${anketa.id}_${safe(sc.letter)}_${stamp()}.xlsx"
        )
        XlsxWriter.write(out, XlsxWriter.Sheet(headerCodes, headerLabels, rows))
        return out
    }

    /**
     * PDF — одна заполненная анкета одного ребёнка «вопрос → ответ»,
     * удобно для архива школы и подписи родителя.
     */
    fun exportChildAnketaPdf(context: Context, child: Child, anketa: Anketa): File {
        val repo = Repository.get(context)
        val sc = repo.classById(child.classId)
        val values = repo.submissionFor(child.id, anketa.id)?.values ?: emptyMap()

        val doc = PdfDocument()
        val pageW = 595; val pageH = 842   // A4 @ 72 dpi
        val margin = 40f
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        var y = margin

        val title = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val h2 = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 11f }
        val muted = Paint().apply { textSize = 10f; color = 0xFF666666.toInt() }

        fun newPage() {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create())
            y = margin
        }
        fun line(text: String, p: Paint, dy: Float = 16f) {
            if (y + dy > pageH - margin) newPage()
            page.canvas.drawText(text, margin, y, p)
            y += dy
        }
        fun wrap(text: String, p: Paint, indent: Float = 0f, dy: Float = 14f) {
            val maxW = pageW - 2 * margin - indent
            val words = text.split(" ")
            var cur = StringBuilder()
            for (w in words) {
                val trial = if (cur.isEmpty()) w else "$cur $w"
                if (p.measureText(trial) > maxW) {
                    line(cur.toString(), p, dy)
                    cur = StringBuilder(w)
                } else cur = StringBuilder(trial)
            }
            if (cur.isNotEmpty()) line(cur.toString(), p, dy)
        }

        line("Анкета № ${anketa.id}. ${anketa.title}", title, 22f)
        line(anketa.subtitle, muted, 18f)
        if (sc != null) {
            line("${sc.school} · ${sc.letter} класс", body, 16f)
        }
        line("Ребёнок: ${child.displayName}  |  Пол: ${if (child.sex == 1) "м" else "ж"}", body, 22f)

        for (f in anketa.fields) {
            val raw = values[f.code].orEmpty()
            val readable = when (f.type) {
                FieldType.CHOICE, FieldType.MULTI ->
                    f.options.firstOrNull { it.code == raw }?.label ?: raw
                else -> raw
            }
            wrap(f.label, h2, dy = 16f)
            wrap(if (readable.isBlank()) "— (не заполнено)" else readable, body, indent = 12f, dy = 14f)
            y += 4f
        }

        line("Сформировано: ${nowHuman()}", muted, 20f)

        doc.finishPage(page)
        val out = exportsDir(context).resolve(
            "Anketa_${anketa.id}_${safe(child.displayName)}_${stamp()}.pdf"
        )
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    /** Экспорт всех 11 анкет по классу одним пакетом — возвращает список файлов. */
    fun exportClassAll(context: Context, sc: SchoolClass): List<File> =
        AnketaCatalog.all.map { exportAnketaXlsx(context, sc, it) }

    private fun exportsDir(context: Context): File =
        File(context.filesDir, "exports").also { it.mkdirs() }

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    private fun nowHuman() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date())
    private fun safe(s: String) = s.replace(Regex("[^A-Za-zА-Яа-я0-9_-]"), "_")
}
