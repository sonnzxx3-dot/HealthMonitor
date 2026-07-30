package ru.school.healthmonitor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.ui.common.AppScaffold

@Composable
fun RoleScreen(nav: NavController) {
    AppScaffold("Мониторинг здоровья") { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Кто вы?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Приложение для сбора и передачи 11 анкет мониторинга здоровья школьника.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { nav.navigate("parent/login") },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Родитель или ученик",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.navigate("teacher/login") },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Сотрудник школы",
                    style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
            Text("Данные хранятся на устройстве. Обмен между родителем и учителем — через файл. Итоговая выгрузка — XLSX по официальным макетам и PDF.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
