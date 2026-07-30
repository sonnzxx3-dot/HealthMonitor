package ru.school.healthmonitor.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import ru.school.healthmonitor.data.Child
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.data.SchoolClass
import ru.school.healthmonitor.domain.Anketa
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.domain.FieldType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private val MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private val commonCodes = listOf(
        "region", "school", "grade", "class", "name1", "name2", "name3", "bdate", "sex"
    )
    private val commonLabels = listOf(
        "Регион", "Школа", "Класс (уровень)", "Класс (с литерой)",
        "Ф", "И", "О", "Дата рожд.", "Пол (1=м, 2=ж)"
    )

    /**
     * XLSX по одной анкете для одного класса. Формат — как в макетах:
     * строка 1 = коды полей, строка 2 = русские подписи, далее по строке на ребёнка.
     * В выгрузку идут любые сохранённые ответы (в т.ч. черновики) — данные есть данные.
     */
    fun exportAnketaXlsx(context: Context, sc: SchoolClass, anketa: Anketa): File {
        val repo = Repository.get(context)
        val children = repo.childrenOfClass(sc.id)

        val headerCodes = commonCodes + anketa.fields.map { it.code }
        val headerLabels = commonLabels + anketa.fields.map { it.label }

        val rows = children.map { child ->
            val values = repo.submissionFor(child.id, anketa.id)?.values ?: emptyMap()
            val common = listOf(
                sc.region, sc.school, sc.grade.toString(), sc.letter,
                child.name1, child.name2, child.name3, child.bdate, child.sex.toString()
            )
            common + anketa.fields.map { normalize(values[it.code]) }
        }

        val out = File(exportsDir(context), "Anketa_${anketa.id}_${safe(sc.letter)}_${stamp()}.xlsx")
        XlsxWriter.write(out, XlsxWriter.Sheet(headerCodes, headerLabels, rows))
        return out
    }

    /** PDF — одна заполненная анкета ребёнка «вопрос → ответ». */
    fun exportChildAnketaPdf(context: Context, child: Child, anketa: Anketa): File {
        val repo = Repository.get(context)
        val sc = repo.classById(child.classId)
        val values = repo.submissionFor(child.id, anketa.id)?.values ?: emptyMap()

        val doc = PdfDocument()
        val w = Writer(doc)

        w.line("Анкета № ${anketa.id}. ${anketa.title}", Fonts.title, 22f)
        w.line(anketa.subtitle, Fonts.muted, 18f)
        if (sc != null) w.wrap("${sc.school} · ${sc.letter} класс", Fonts.body, dy = 16f)
        w.wrap("Ребёнок: ${child.displayName}   Пол: ${if (child.sex == 1) "муж." else "жен."}", Fonts.body, dy = 22f)

        for (f in anketa.fields) {
            val raw = values[f.code].orEmpty()
            val readable = when (f.type) {
                FieldType.CHOICE -> f.options.firstOrNull { it.code == raw }?.label ?: raw
                FieldType.MULTI -> raw.split(",").filter { it.isNotBlank() }
                    .joinToString(", ") { code -> f.options.firstOrNull { it.code == code }?.label ?: code }
                else -> raw
            }
            w.wrap(f.label, Fonts.h2, dy = 16f)
            w.wrap(if (readable.isBlank()) "— (не заполнено)" else readable, Fonts.body, indent = 14f, dy = 14f)
            w.gap(4f)
        }
        w.gap(8f)
        w.line("Сформировано: ${nowHuman()}", Fonts.muted, 18f)
        w.line("Подпись родителя: ______________________", Fonts.body, 22f)

        w.finish()
        val out = File(exportsDir(context), "PDF_${anketa.id}_${safe(child.displayName)}_${stamp()}.pdf")
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    /**
     * Сводный PDF по классу: готовность каждого ребёнка по всем 11 анкетам.
     * Удобно классному руководителю понять, кто что не сдал.
     */
    fun exportClassSummaryPdf(context: Context, sc: SchoolClass): File {
        val repo = Repository.get(context)
        val children = repo.childrenOfClass(sc.id)

        val doc = PdfDocument()
        val w = Writer(doc)

        w.line("Сводка готовности анкет", Fonts.title, 22f)
        w.wrap("${sc.school} · ${sc.letter} класс · ${sc.region}", Fonts.body, dy = 16f)
        w.wrap("Учеников: ${children.size}", Fonts.muted, dy = 20f)

        if (children.isEmpty()) {
            w.wrap("В классе пока нет зарегистрированных учеников.", Fonts.body, dy = 16f)
        } else {
            children.forEach { child ->
                val filled = AnketaCatalog.all.filter { a ->
                    repo.submissionFor(child.id, a.id)?.values?.isNotEmpty() == true
                }.map { it.id }
                w.wrap(child.displayName, Fonts.h2, dy = 15f)
                val status = AnketaCatalog.all.joinToString("  ") { a ->
                    "№${a.id}${if (a.id in filled) "✓" else "·"}"
                }
                w.wrap("$status   (${filled.size}/${AnketaCatalog.all.size})", Fonts.body, indent = 10f, dy = 14f)
                w.gap(4f)
            }
        }
        w.gap(8f)
        w.line("Сформировано: ${nowHuman()}", Fonts.muted, 16f)

        w.finish()
        val out = File(exportsDir(context), "Svodka_${safe(sc.letter)}_${stamp()}.pdf")
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
        return out
    }

    /** Все 11 XLSX по классу. */
    fun exportClassAll(context: Context, sc: SchoolClass): List<File> =
        AnketaCatalog.all.map { exportAnketaXlsx(context, sc, it) }

    // ─── PDF-рендер ─────────────────────────────────────────────

    private object Fonts {
        val title = Paint().apply { textSize = 16f; isFakeBoldText = true; isAntiAlias = true }
        val h2 = Paint().apply { textSize = 12f; isFakeBoldText = true; isAntiAlias = true }
        val body = Paint().apply { textSize = 11f; isAntiAlias = true }
        val muted = Paint().apply { textSize = 10f; color = 0xFF666666.toInt(); isAntiAlias = true }
    }

    private class Writer(private val doc: PdfDocument) {
        private val pageW = 595; private val pageH = 842; private val margin = 40f
        private var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        private var y = margin + 8f

        private fun newPage() {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create())
            y = margin + 8f
        }

        fun gap(dy: Float) { y += dy }

        fun line(text: String, p: Paint, dy: Float = 16f) {
            if (y + dy > pageH - margin) newPage()
            page.canvas.drawText(text, margin, y, p)
            y += dy
        }

        /** Перенос по словам + принудительный разрыв слишком длинного слова. */
        fun wrap(text: String, p: Paint, indent: Float = 0f, dy: Float = 14f) {
            val maxW = pageW - 2 * margin - indent
            val x = margin + indent
            fun emit(s: String) {
                if (y + dy > pageH - margin) newPage()
                page.canvas.drawText(s, x, y, p)
                y += dy
            }
            var cur = StringBuilder()
            for (word in text.split(" ")) {
                var chunk = word
                // если само слово шире строки — режем по символам
                while (p.measureText(chunk) > maxW && chunk.length > 1) {
                    var cut = chunk.length
                    while (cut > 1 && p.measureText(chunk.substring(0, cut)) > maxW) cut--
                    if (cur.isNotEmpty()) { emit(cur.toString()); cur = StringBuilder() }
                    emit(chunk.substring(0, cut))
                    chunk = chunk.substring(cut)
                }
                val trial = if (cur.isEmpty()) chunk else "$cur $chunk"
                if (p.measureText(trial) > maxW && cur.isNotEmpty()) {
                    emit(cur.toString()); cur = StringBuilder(chunk)
                } else cur = StringBuilder(trial)
            }
            if (cur.isNotEmpty()) emit(cur.toString())
        }

        fun finish() { doc.finishPage(page) }
    }

    // ─── утилиты ────────────────────────────────────────────────

    /** Нормализуем дробные: запятую в точку — стороннние системы ждут точку. */
    private fun normalize(v: String?): String = v?.trim()?.replace(",", ".") ?: ""

    private fun exportsDir(context: Context): File =
        File(context.filesDir, "exports").also { it.mkdirs() }

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    private fun nowHuman() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date())
    private fun safe(s: String) = s.replace(Regex("[^A-Za-zА-Яа-я0-9_-]"), "_").take(40)
}
