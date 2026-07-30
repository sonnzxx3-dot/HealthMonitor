package ru.school.healthmonitor.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.data.TeacherRole
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.EmptyState
import ru.school.healthmonitor.ui.common.ProgressStrip
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun TeacherHomeScreen(nav: NavController, teacherId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()

    val teacher = state.teachers.firstOrNull { it.id == teacherId }
    if (teacher == null) {
        AppScaffold("Кабинет", nav) { pv ->
            EmptyState(
                icon = "🔒",
                title = "Аккаунт не найден",
                description = "Возможно, аккаунт удалён. Войдите заново."
            )
        }
        return
    }

    val roleLabel = when (teacher.role) {
        TeacherRole.ADMIN -> "Администратор школы"
        TeacherRole.HOMEROOM -> "Классный руководитель"
        TeacherRole.MEDIC -> "Медицинский работник"
        TeacherRole.PE -> "Учитель физкультуры"
    }
    val availableAnketas = AnketaCatalog.forTeacherRole(teacher.role)

    AppScaffold(teacher.fullName, nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard {
                Text(roleLabel, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Доступ к анкетам: ${availableAnketas.joinToString { "№${it.id}" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard("Классы") {
                if (teacher.classIds.isEmpty()) {
                    Text("Классы не назначены. Обратитесь к администратору.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    teacher.classIds.forEach { cid ->
                        val sc = repo.classById(cid) ?: return@forEach
                        val kids = repo.childrenOfClass(cid)
                        val totalCells = kids.size * AnketaCatalog.all.size
                        val filledCells = state.submissions.count {
                            it.finalized &&
                                    kids.any { c -> c.id == it.childId }
                        }
                        FilledTonalButton(
                            onClick = { nav.navigate("teacher/roster/${teacher.id}/$cid") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(4.dp)) {
                                Text("${sc.letter}  ·  ${sc.school}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(4.dp))
                                Text("${kids.size} учеников · код ${sc.inviteCode}",
                                    style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                ProgressStrip(filledCells, totalCells.coerceAtLeast(1))
                            }
                        }
                    }
                }
            }

            if (teacher.role == TeacherRole.ADMIN) {
                SectionCard("Администрирование") {
                    Text("Управление классами, аккаунтами учителей и данными.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { nav.navigate("admin/${teacher.id}") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Открыть панель администратора") }
                }
            }

            SectionCard("Помощь") {
                Text(
                    "Родители заполняют анкеты у себя и отправляют кнопкой «Отправить учителю». " +
                            "Вы получаете файл в мессенджере или почте — в разделе «Выгрузка» класса " +
                            "нажмите «Загрузить данные родителя» и выберите файл.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
