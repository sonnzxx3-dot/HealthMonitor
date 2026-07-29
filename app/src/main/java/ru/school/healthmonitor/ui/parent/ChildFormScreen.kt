package ru.school.healthmonitor.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun ChildFormScreen(nav: NavController, classId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }

    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var n3 by remember { mutableStateOf("") }
    var bdate by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AppScaffold("Новый ребёнок", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("Псевдоним ребёнка") {
                Text("Указываем ТОЛЬКО первые буквы, чтобы система не хранила полные ФИО.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    letterField("Ф", n1) { n1 = it.take(1).uppercase() }
                    letterField("И", n2) { n2 = it.take(1).uppercase() }
                    letterField("О", n3) { n3 = it.take(1).uppercase() }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    bdate, { bdate = it },
                    label = { Text("Дата рождения (дд.мм.гггг)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Пол")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(sex == 1, { sex = 1 }, label = { Text("мужской") })
                    FilterChip(sex == 2, { sex = 2 }, label = { Text("женский") })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (n1.isBlank() || n2.isBlank() || n3.isBlank()) { error = "Заполните буквы ФИО"; return@Button }
                        if (!bdate.matches(Regex("""\d{2}\.\d{2}\.\d{4}"""))) { error = "Дата в формате дд.мм.гггг"; return@Button }
                        if (sex == null) { error = "Укажите пол"; return@Button }
                        val c = repo.addChild(classId, n1, n2, n3, bdate, sex!!)
                        nav.navigate("parent/home/${c.id}") {
                            popUpTo("role"); launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Создать") }
            }
        }
    }
}

@Composable
private fun letterField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(88.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters
        )
    )
}
