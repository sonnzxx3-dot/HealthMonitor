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
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun TeacherHomeScreen(nav: NavController, teacherId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()

    val teacher = state.teachers.firstOrNull { it.id == teacherId }
    if (teacher == null) {
        AppScaffold("Кабинет учителя", nav) { pv ->
            Text("Аккаунт не найден", Modifier.padding(pv).padding(16.dp))
        }
        return
    }

    AppScaffold("Кабинет: ${teacher.fullName}", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("Роль") { Text(teacher.role.name) }

            SectionCard("Классы") {
                if (teacher.classIds.isEmpty()) {
                    Text("Классы не назначены. Обратитесь к администратору.")
                } else {
                    teacher.classIds.forEach { cid ->
                        val sc = repo.classById(cid) ?: return@forEach
                        OutlinedButton(
                            onClick = { nav.navigate("teacher/roster/${teacher.id}/$cid") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) { Text("${sc.letter}  ·  ${sc.school}") }
                    }
                }
            }
        }
    }
}
