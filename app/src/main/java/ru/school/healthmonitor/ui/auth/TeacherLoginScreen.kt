package ru.school.healthmonitor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    AppScaffold("Вход для сотрудника", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionCard("Учётная запись") {
                Text("Демо: admin / admin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    login, { login = it.trim(); error = null },
                    label = { Text("Логин") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    pass, { pass = it; error = null },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val t = repo.authTeacher(login, pass)
                        if (t == null) error = "Неверный логин или пароль"
                        else nav.navigate("teacher/home/${t.id}") {
                            popUpTo("role")
                        }
                    },
                    enabled = login.isNotBlank() && pass.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Войти", fontWeight = FontWeight.Medium) }
            }
        }
    }
}
