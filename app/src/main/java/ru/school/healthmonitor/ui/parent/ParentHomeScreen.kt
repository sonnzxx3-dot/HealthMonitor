package ru.school.healthmonitor.ui.parent

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.ConfirmDialog
import ru.school.healthmonitor.ui.common.ProgressStrip
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun ParentHomeScreen(nav: NavController, childId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val scope = rememberCoroutineScope()

    val child = state.children.firstOrNull { it.id == childId }
    val sc = child?.let { repo.classById(it.classId) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbar.showSnackbar(it); snackMsg = null }
    }

    if (child == null) {
        AppScaffold("Ребёнок не найден", nav) { pv ->
            Text("Возможно, карточка удалена. Вернитесь и войдите заново.",
                Modifier.padding(pv).padding(16.dp))
        }
        return
    }

    val done = AnketaCatalog.parentAnketas.count { a ->
        state.submissions.any { it.childId == child.id && it.anketaId == a.id }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text("Анкеты", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard {
                Text(child.displayName, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("Пол: ${if (child.sex == 1) "мужской" else "женский"}",
                    style = MaterialTheme.typography.bodyMedium)
                if (sc != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("${sc.school}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Класс: ${sc.letter}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                ProgressStrip(done, AnketaCatalog.parentAnketas.size)
            }

            SectionCard("Отправить данные учителю") {
                Text("Одной кнопкой создаётся файл со всеми ответами и открывается выбор мессенджера / почты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            sending = true
                            val file = withContext(Dispatchers.IO) { repo.exportChildPackage(child.id) }
                            sending = false
                            if (file == null) {
                                snackMsg = "Не удалось подготовить файл"
                            } else {
                                val uri = FileProvider.getUriForFile(
                                    ctx, "${ctx.packageName}.fileprovider", file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Анкеты: ${child.displayName}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Отправить учителю"))
                            }
                        }
                    },
                    enabled = !sending && done > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (sending) "Готовлю…" else "Отправить учителю")
                }
                if (done == 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("Заполните хотя бы одну анкету, чтобы отправить.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "Анкеты",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                fontWeight = FontWeight.SemiBold
            )
            AnketaCatalog.parentAnketas.forEach { anketa ->
                val filled = state.submissions.any {
                    it.childId == child.id && it.anketaId == anketa.id && it.submittedBy != "draft"
                }
                val hasDraft = state.submissions.any {
                    it.childId == child.id && it.anketaId == anketa.id
                } && !filled
                ElevatedCard(
                    onClick = { nav.navigate("anketa/${child.id}/${anketa.id}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filled) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Outlined.RadioButtonUnchecked, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("№${anketa.id}. ${anketa.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium)
                            Text(anketa.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (hasDraft) {
                                Text("• черновик сохранён",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    ConfirmDialog(
        show = showDeleteDialog,
        title = "Удалить карточку ребёнка?",
        text = "${child.displayName}: все анкеты будут стёрты. Действие нельзя отменить.",
        confirmLabel = "Удалить",
        danger = true,
        onConfirm = {
            repo.deleteChild(child.id)
            nav.popBackStack(nav.graph.startDestinationId, false)
        },
        onDismiss = { showDeleteDialog = false }
    )
}
