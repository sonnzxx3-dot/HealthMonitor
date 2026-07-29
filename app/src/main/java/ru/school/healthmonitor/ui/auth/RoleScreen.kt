package ru.school.healthmonitor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.ui.common.AppScaffold

@Composable
fun RoleScreen(nav: NavController) {
    AppScaffold("Мониторинг здоровья школьника") { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Выберите, кто вы",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { nav.navigate("parent/login") },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("Родитель / ученик", style = MaterialTheme.typography.titleMedium) }
            Button(
                onClick = { nav.navigate("teacher/login") },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("Учитель / медработник / администратор", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.weight(1f))
            Text(
                "Данные хранятся на устройстве. Выгрузка формируется в XLSX по официальным макетам и PDF.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
