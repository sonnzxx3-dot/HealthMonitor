package ru.school.healthmonitor.ui.teacher

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.export.Exporter
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard
import java.io.File

@Composable
fun ExportScreen(nav: NavController, teacherId: String, classId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val sc = repo.classById(classId) ?: return
    val kids = state.children.filter { it.classId == classId }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var lastFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    fun share(files: List<File>, mime: String) {
        if (files.isEmpty()) return
        val uris: ArrayList<Uri> = files.mapTo(ArrayList()) {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Отправить"))
    }

    AppScaffold("Выгрузка · ${sc.letter}", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("XLSX по официальным макетам") {
                Text("Формируется 11 файлов — по одному на каждую анкету. " +
                        "Строка 1 — коды полей, строка 2 — русские подписи, " +
                        "далее по строке на каждого из ${kids.size} детей.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            val files = withContext(Dispatchers.IO) {
                                Exporter.exportClassAll(ctx, sc)
                            }
                            lastFiles = files
                            busy = false
                            share(files, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Формируется…" else "Сформировать все XLSX и отправить") }

                Spacer(Modifier.height(4.dp))
                Text("Отдельные анкеты:", style = MaterialTheme.typography.bodySmall)
                AnketaCatalog.all.forEach { a ->
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                val f = withContext(Dispatchers.IO) { Exporter.exportAnketaXlsx(ctx, sc, a) }
                                busy = false
                                share(listOf(f), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    ) { Text("№${a.id}. ${a.title}") }
                }
            }

            SectionCard("PDF по каждому ребёнку") {
                Text("Заполненная анкета в привычном виде «вопрос → ответ», удобно для архива.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                kids.forEach { child ->
                    AnketaCatalog.all.forEach { a ->
                        val exists = state.submissions.any { it.childId == child.id && it.anketaId == a.id }
                        if (exists) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        val f = withContext(Dispatchers.IO) {
                                            Exporter.exportChildAnketaPdf(ctx, child, a)
                                        }
                                        busy = false
                                        share(listOf(f), "application/pdf")
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            ) { Text("PDF · ${child.displayName} · №${a.id}") }
                        }
                    }
                }
                if (kids.isEmpty() || state.submissions.none { s -> kids.any { it.id == s.childId } }) {
                    Text("Пока нет заполненных анкет.", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (lastFiles.isNotEmpty()) {
                SectionCard("Последние файлы") {
                    lastFiles.forEach { Text(it.name, style = MaterialTheme.typography.bodySmall) }
                    Text("Хранятся в приватной папке приложения.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
