package ru.school.healthmonitor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionCard("Код приглашения класса") {
                Text("Код выдаёт классный руководитель. Демо: 36KEM-5A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); error = null },
                    label = { Text("Код") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val cls = repo.classByCode(code)
                        if (cls == null) error = "Код не найден. Уточните у классного руководителя."
                        else currentClassId = cls.id
                    },
                    enabled = code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Войти в класс", fontWeight = FontWeight.Medium) }
            }

            val cid = currentClassId
            if (cid != null) {
                val cls = repo.classById(cid)!!
                val kids = state.children.filter { it.classId == cid }
                SectionCard("Класс: ${cls.letter}") {
                    Text(cls.school, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    if (kids.isEmpty()) {
                        Text("В классе пока нет зарегистрированных детей — добавьте своего:",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Продолжить с ребёнком:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        kids.forEach { child ->
                            OutlinedButton(
                                onClick = { nav.navigate("parent/home/${child.id}") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    .height(52.dp)
                            ) { Text(child.displayName) }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { nav.navigate("parent/newchild/$cid") },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить ребёнка")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
