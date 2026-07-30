package ru.school.healthmonitor.ui.teacher

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val snackbar = remember { SnackbarHostState() }
    var snackText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackText) {
        snackText?.let { snackbar.showSnackbar(it); snackText = null }
    }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = withContext(Dispatchers.IO) { repo.importChildPackage(uri) }
                busy = false
                snackText = when (result) {
                    is Repository.ImportResult.Ok ->
                        "Загружено: ${result.childName}, анкет: ${result.submissionsCount}"
                    is Repository.ImportResult.Error -> "Ошибка: ${result.message}"
                }
            }
        }
    }

    fun share(files: List<File>, mime: String) {
        if (files.isEmpty()) return
        val uris: ArrayList<Uri> = files.mapTo(ArrayList()) {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
        }
        val intent = if (files.size == 1)
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        else Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Отправить"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text("Выгрузка · ${sc.letter}", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("Загрузить данные родителя") {
                Text(
                    "Родитель отправил файл .json — сохраните его в память телефона " +
                            "(в мессенджере: «Скачать»/«Сохранить»), затем нажмите кнопку и выберите файл.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { importer.launch(arrayOf("application/json", "*/*")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text(if (busy) "Обработка…" else "Загрузить файл от родителя") }
            }

            SectionCard("XLSX по официальным макетам") {
                Text(
                    "11 файлов — по одному на анкету. Строка 1 — коды полей, строка 2 — русские подписи, " +
                            "далее по строке на каждого из ${kids.size} детей.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            val files = withContext(Dispatchers.IO) { Exporter.exportClassAll(ctx, sc) }
                            busy = false
                            share(files, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        }
                    },
                    enabled = !busy && kids.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(if (busy) "Формируется…" else "Сформировать все 11 XLSX и отправить") }
                if (kids.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("В классе нет учеников — сначала попросите родителей зарегистрироваться и отправить свои анкеты.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(12.dp))
                Text("Отдельные анкеты", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
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
                        enabled = !busy && kids.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text("№${a.id}. ${a.title}") }
                }
            }

            SectionCard("Сводка по классу (PDF)") {
                Text("Кто из учеников какие анкеты заполнил — удобно отследить недостающее.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            val f = withContext(Dispatchers.IO) { Exporter.exportClassSummaryPdf(ctx, sc) }
                            busy = false
                            share(listOf(f), "application/pdf")
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text(if (busy) "Формируется…" else "Сформировать сводку") }
            }

            SectionCard("PDF по ребёнку") {
                Text("Заполненная анкета в виде «вопрос → ответ» — для архива и подписи родителя.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                val pairs = kids.flatMap { child ->
                    AnketaCatalog.all.filter { a ->
                        state.submissions.any { it.childId == child.id && it.anketaId == a.id && it.hasData }
                    }.map { child to it }
                }
                if (pairs.isEmpty()) {
                    Text("Пока нет заполненных анкет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    pairs.forEach { (child, a) ->
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
            Spacer(Modifier.height(24.dp))
        }
    }
}
