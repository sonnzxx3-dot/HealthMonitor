package ru.school.healthmonitor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun TeacherLoginScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }

    var login by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AppScaffold("Вход для учителя", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("Учётная запись сотрудника") {
                Text("Демо-аккаунт: admin / admin", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(login, { login = it; error = null },
                    label = { Text("Логин") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(pass, { pass = it; error = null },
                    label = { Text("Пароль") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val t = repo.authTeacher(login.trim(), pass)
                        if (t == null) error = "Неверный логин или пароль"
                        else nav.navigate("teacher/home/${t.id}")
                    },
                    enabled = login.isNotBlank() && pass.isNotBlank()
                ) { Text("Войти") }
            }
        }
    }
}
