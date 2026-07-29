package ru.school.healthmonitor.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun ClassRosterScreen(nav: NavController, teacherId: String, classId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val teacher = state.teachers.firstOrNull { it.id == teacherId } ?: return
    val sc = repo.classById(classId) ?: return
    val kids = state.children.filter { it.classId == classId }
    val anketas = AnketaCatalog.forTeacherRole(teacher.role)

    AppScaffold("${sc.letter} · ${sc.school}", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("Класс") {
                Text("Код приглашения родителей: ${sc.inviteCode}",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Учеников в классе: ${kids.size}",
                    style = MaterialTheme.typography.bodySmall)
            }

            SectionCard("Анкеты") {
                anketas.forEach { a ->
                    OutlinedButton(
                        onClick = {
                            if (a.tabular) nav.navigate("teacher/table/$teacherId/$classId/${a.id}")
                            else if (kids.isNotEmpty()) nav.navigate("anketa/${kids.first().id}/${a.id}")
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text("№${a.id}. ${a.title}${if (a.tabular) "  (табличная)" else ""}")
                    }
                }
            }

            SectionCard("Ученики") {
                if (kids.isEmpty()) Text("Пока пусто — родители зарегистрируются по коду ${sc.inviteCode}.")
                else kids.forEach { c ->
                    val done = state.submissions.count { it.childId == c.id }
                    Text("${c.displayName}  ·  заполнено анкет: $done/11")
                }
            }

            Button(
                onClick = { nav.navigate("teacher/export/$teacherId/$classId") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Выгрузка результатов") }
        }
    }
}
