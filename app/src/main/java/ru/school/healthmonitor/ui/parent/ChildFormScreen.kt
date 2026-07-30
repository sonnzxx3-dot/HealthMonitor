package ru.school.healthmonitor.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionCard("Псевдоним") {
                Text("Указываем только первые буквы — полные ФИО в системе не хранятся.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    letterField("Ф", n1) { n1 = it.take(1).uppercase() }
                    letterField("И", n2) { n2 = it.take(1).uppercase() }
                    letterField("О", n3) { n3 = it.take(1).uppercase() }
                }
            }

            SectionCard("Дата рождения") {
                OutlinedTextField(
                    bdate,
                    onValueChange = { new ->
                        // авто-вставка точек
                        val digits = new.filter { it.isDigit() }.take(8)
                        bdate = buildString {
                            digits.forEachIndexed { i, c ->
                                if (i == 2 || i == 4) append('.')
                                append(c)
                            }
                        }
                    },
                    label = { Text("дд.мм.гггг") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard("Пол") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(sex == 1, { sex = 1 }, label = { Text("мужской") },
                        modifier = Modifier.weight(1f).height(48.dp))
                    FilterChip(sex == 2, { sex = 2 }, label = { Text("женский") },
                        modifier = Modifier.weight(1f).height(48.dp))
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    when {
                        n1.isBlank() || n2.isBlank() || n3.isBlank() -> error = "Заполните все три буквы"
                        !bdate.matches(Regex("""\d{2}\.\d{2}\.\d{4}""")) -> error = "Дата в формате дд.мм.гггг"
                        sex == null -> error = "Укажите пол"
                        else -> {
                            val c = repo.addChild(classId, n1, n2, n3, bdate, sex!!)
                            nav.popBackStack()
                            nav.navigate("parent/home/${c.id}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Создать", fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.letterField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.weight(1f),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next
        )
    )
}
