package ru.school.healthmonitor.ui.auth

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
fun ParentLoginScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()

    var code by remember { mutableStateOf("") }
    var currentClassId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AppScaffold("Вход для родителя", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("Код приглашения класса") {
                Text("Код выдаёт классный руководитель. Демо-код: 36KEM-5A", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it; error = null },
                    label = { Text("Код") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val cls = repo.classByCode(code)
                        if (cls == null) error = "Код не найден"
                        else currentClassId = cls.id
                    },
                    enabled = code.isNotBlank()
                ) { Text("Войти в класс") }
            }

            val cid = currentClassId
            if (cid != null) {
                val cls = repo.classById(cid)!!
                val kids = state.children.filter { it.classId == cid }
                SectionCard("Класс: ${cls.letter} — ${cls.school}") {
                    if (kids.isEmpty()) {
                        Text("В этом классе пока никто не зарегистрировал ребёнка.")
                    } else {
                        Text("Выберите ребёнка (для нескольких детей — добавьте каждого):", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        kids.forEach { child ->
                            OutlinedButton(
                                onClick = { nav.navigate("parent/home/${child.id}") },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(child.displayName) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { nav.navigate("parent/newchild/$cid") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Добавить ребёнка") }
                }
            }
        }
    }
}
