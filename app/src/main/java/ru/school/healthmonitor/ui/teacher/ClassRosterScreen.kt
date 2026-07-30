package ru.school.healthmonitor.ui.teacher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.EmptyState
import ru.school.healthmonitor.ui.common.ProgressStrip
import ru.school.healthmonitor.ui.common.SectionCard
import ru.school.healthmonitor.util.rememberQr

@Composable
fun ClassRosterScreen(nav: NavController, teacherId: String, classId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val teacher = state.teachers.firstOrNull { it.id == teacherId } ?: return
    val sc = repo.classById(classId) ?: return
    val kids = state.children.filter { it.classId == classId }
    val anketas = AnketaCatalog.forTeacherRole(teacher.role)
    var showQr by remember { mutableStateOf(false) }

    AppScaffold("${sc.letter}", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard {
                Text(sc.school, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Регион: ${sc.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Код приглашения родителей: ${sc.inviteCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text("Учеников: ${kids.size}",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showQr = !showQr }) {
                    Text(if (showQr) "Скрыть QR-код" else "Показать QR-код для родителей")
                }
                if (showQr) {
                    val qr = rememberQr(sc.inviteCode, 480)
                    if (qr != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = "QR-код класса ${sc.inviteCode}",
                                modifier = Modifier.size(220.dp)
                            )
                        }
                        Text("Родители сканируют этот код камерой при входе.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SectionCard("Анкеты") {
                anketas.forEach { a ->
                    val donePerAnketa = kids.count { c ->
                        state.submissions.any {
                            it.childId == c.id && it.anketaId == a.id && it.finalized
                        }
                    }
                    ElevatedCard(
                        onClick = {
                            if (a.tabular) nav.navigate("teacher/table/$teacherId/$classId/${a.id}")
                            else if (kids.isNotEmpty()) nav.navigate("anketa/${kids.first().id}/${a.id}")
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("№${a.id}. ${a.title}${if (a.tabular) "  (таблица)" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            ProgressStrip(donePerAnketa, kids.size.coerceAtLeast(1))
                        }
                    }
                }
            }

            SectionCard("Ученики") {
                if (kids.isEmpty()) {
                    EmptyState(
                        icon = "👥",
                        title = "Пока никого нет",
                        description = "Дайте родителям код ${sc.inviteCode}. Они зайдут в приложении и добавят детей сами."
                    )
                } else {
                    kids.forEach { c ->
                        val done = state.submissions.count {
                            it.childId == c.id && it.finalized
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text("${if (c.sex == 1) "м" else "ж"} · $done/${AnketaCatalog.all.size} анкет",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            Button(
                onClick = { nav.navigate("teacher/export/$teacherId/$classId") },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp)
            ) { Text("Выгрузка результатов", fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
