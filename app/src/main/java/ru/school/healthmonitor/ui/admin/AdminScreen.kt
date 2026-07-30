package ru.school.healthmonitor.ui.admin

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.data.TeacherRole
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.ConfirmDialog
import ru.school.healthmonitor.ui.common.EmptyState
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun AdminScreen(nav: NavController, teacherId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val me = repo.teacherById(teacherId)
    if (me == null || me.role != TeacherRole.ADMIN) {
        AppScaffold("Нет доступа", nav) { pv ->
            EmptyState(icon = "🔒", title = "Только для администратора",
                description = "Обратитесь к администратору школы.")
        }
        return
    }

    var deleteClass by remember { mutableStateOf<String?>(null) }
    var deleteTeacher by remember { mutableStateOf<String?>(null) }
    var wipeDialog by remember { mutableStateOf(false) }

    AppScaffold("Администрирование", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("Классы (${state.classes.size})") {
                if (state.classes.isEmpty()) {
                    Text("Классов пока нет — создайте первый.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.classes.forEach { c ->
                    val kids = repo.childrenOfClass(c.id).size
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${c.letter}  ·  ${c.school}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium)
                            Text("код ${c.inviteCode}  ·  $kids учеников",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { deleteClass = c.id }) { Text("Удалить") }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav.navigate("admin/$teacherId/newclass") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Создать класс") }
            }

            SectionCard("Сотрудники (${state.teachers.size})") {
                state.teachers.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium)
                            Text("${roleLabel(t.role)}  ·  логин: ${t.login}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (t.id != teacherId) {
                            TextButton(onClick = { deleteTeacher = t.id }) { Text("Удалить") }
                        } else {
                            Text("вы", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { nav.navigate("admin/$teacherId/newteacher") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Добавить сотрудника") }
            }

            SectionCard("Опасная зона") {
                Text("Полный сброс данных на этом устройстве. Все классы, дети и ответы будут стёрты, вернётся исходный демо-класс.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { wipeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Сбросить все данные") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    ConfirmDialog(
        show = deleteClass != null,
        title = "Удалить класс?",
        text = "Класс, все ученики и их анкеты будут удалены безвозвратно.",
        confirmLabel = "Удалить", danger = true,
        onConfirm = { deleteClass?.let { repo.deleteClass(it) } },
        onDismiss = { deleteClass = null }
    )
    ConfirmDialog(
        show = deleteTeacher != null,
        title = "Удалить сотрудника?",
        text = "Аккаунт будет удалён. Данные, введённые этим сотрудником, останутся.",
        confirmLabel = "Удалить", danger = true,
        onConfirm = { deleteTeacher?.let { repo.deleteTeacher(it) } },
        onDismiss = { deleteTeacher = null }
    )
    ConfirmDialog(
        show = wipeDialog,
        title = "Полный сброс?",
        text = "Все локальные данные будут удалены. Это действие нельзя отменить.",
        confirmLabel = "Сбросить всё", danger = true,
        onConfirm = {
            repo.wipeAll()
            nav.popBackStack(nav.graph.startDestinationId, false)
        },
        onDismiss = { wipeDialog = false }
    )
}

private fun roleLabel(r: TeacherRole) = when (r) {
    TeacherRole.ADMIN -> "Администратор"
    TeacherRole.HOMEROOM -> "Классный руководитель"
    TeacherRole.MEDIC -> "Медработник"
    TeacherRole.PE -> "Учитель физкультуры"
}

@Composable
fun NewClassScreen(nav: NavController, teacherId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }

    var region by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var letter by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AppScaffold("Новый класс", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(region, { region = it },
                label = { Text("Регион") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            OutlinedTextField(school, { school = it },
                label = { Text("Школа") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(grade, { grade = it.filter { c -> c.isDigit() } },
                    label = { Text("Уровень (5, 6…)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f))
                OutlinedTextField(letter, { letter = it },
                    label = { Text("Обозначение (5А)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f))
            }
            OutlinedTextField(code, { code = it.uppercase() },
                label = { Text("Код приглашения") },
                supportingText = { Text("Родители вводят его при входе") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth())
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val g = grade.toIntOrNull()
                    when {
                        region.isBlank() -> err = "Укажите регион"
                        school.isBlank() -> err = "Укажите школу"
                        g == null -> err = "Уровень — число"
                        letter.isBlank() -> err = "Укажите обозначение"
                        code.isBlank() -> err = "Придумайте код"
                        repo.classByCode(code) != null -> err = "Такой код уже занят"
                        else -> {
                            repo.addClass(region, school, g, letter, code)
                            nav.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Создать") }
        }
    }
}

@Composable
fun NewTeacherScreen(nav: NavController, teacherId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()

    var login by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var fio by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(TeacherRole.HOMEROOM) }
    val selectedClasses = remember { mutableStateListOf<String>() }
    var err by remember { mutableStateOf<String?>(null) }

    AppScaffold("Новый сотрудник", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(fio, { fio = it },
                label = { Text("ФИО") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(login, { login = it.trim() },
                label = { Text("Логин") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pass, { pass = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth())

            Text("Роль", style = MaterialTheme.typography.titleSmall)
            TeacherRole.values().forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = role == r, onClick = { role = r })
                    Text(roleLabel(r))
                }
            }

            Text("Классы", style = MaterialTheme.typography.titleSmall)
            if (state.classes.isEmpty()) {
                Text("Сначала создайте хотя бы один класс",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.classes.forEach { c ->
                val on = c.id in selectedClasses
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = {
                            if (it) selectedClasses.add(c.id)
                            else selectedClasses.remove(c.id)
                        }
                    )
                    Text("${c.letter} · ${c.school}")
                }
            }

            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    when {
                        fio.isBlank() -> err = "Укажите ФИО"
                        login.isBlank() -> err = "Укажите логин"
                        pass.length < 4 -> err = "Пароль — минимум 4 символа"
                        state.teachers.any { it.login == login } -> err = "Такой логин уже занят"
                        else -> {
                            repo.addTeacher(login, pass, fio, role, selectedClasses.toList())
                            nav.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Добавить") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
