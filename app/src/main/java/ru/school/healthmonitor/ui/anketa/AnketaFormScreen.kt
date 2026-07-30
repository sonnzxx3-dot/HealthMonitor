package ru.school.healthmonitor.ui.anketa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import ru.school.healthmonitor.data.AnketaSubmission
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.domain.Field
import ru.school.healthmonitor.domain.FieldType
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.ConfirmDialog
import ru.school.healthmonitor.ui.common.ProgressStrip
import ru.school.healthmonitor.ui.common.SectionCard

/**
 * Динамическая форма любой анкеты с автосохранением и валидацией.
 * Автосохранение включается через 800 мс после последнего изменения — черновик
 * остаётся, если пользователь закроет экран. Финальная кнопка «Сохранить и
 * отправить» проверяет обязательные поля и диапазоны.
 */
@Composable
fun AnketaFormScreen(
    nav: NavController,
    childId: String,
    anketaId: String
) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val anketa = remember(anketaId) { AnketaCatalog.byId(anketaId) }
    val existing = remember(childId, anketaId) { repo.submissionFor(childId, anketaId) }
    val kb = LocalSoftwareKeyboardController.current

    val values = remember {
        mutableStateMapOf<String, String>().apply { existing?.values?.let { putAll(it) } }
    }
    val errors = remember { mutableStateMapOf<String, String>() }
    var dirty by remember { mutableStateOf(false) }
    var autosaved by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Автосохранение: сохраняем черновик через 800 мс после последней правки.
    LaunchedEffect(dirty) {
        if (dirty) {
            delay(800)
            saveSilently(repo, childId, anketaId, values, autoDraft = true)
            autosaved = true
            dirty = false
            delay(1500)
            autosaved = false
        }
    }

    val filledCount = anketa.fields.count { values[it.code]?.isNotBlank() == true }

    AppScaffold(
        title = "№${anketa.id}. ${anketa.title}",
        nav = nav,
        actions = {
            if (existing != null) {
                IconButton(onClick = { showResetDialog = true }) {
                    Text("↺", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(anketa.subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            ProgressStrip(filledCount, anketa.fields.size)

            if (autosaved) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Черновик сохранён", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(4.dp))
            anketa.fields.forEachIndexed { idx, f ->
                RenderField(
                    f = f,
                    idx = idx + 1,
                    total = anketa.fields.size,
                    values = values,
                    errors = errors,
                    onChanged = { dirty = true; errors.remove(f.code) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    errors.clear()
                    for (f in anketa.fields) {
                        val v = values[f.code].orEmpty().trim()
                        if (f.required && v.isBlank()) errors[f.code] = "Обязательное поле"
                        else if (v.isNotBlank()) validateField(f, v)?.let { errors[f.code] = it }
                    }
                    if (errors.isEmpty()) {
                        kb?.hide()
                        saveSilently(repo, childId, anketaId, values, autoDraft = false)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (existing == null) "Сохранить и отправить" else "Обновить",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "Обязательные поля помечены звёздочкой. Данные автосохраняются во время заполнения.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    ConfirmDialog(
        show = showResetDialog,
        title = "Очистить ответы?",
        text = "Все ответы этой анкеты будут удалены. Это действие нельзя отменить.",
        confirmLabel = "Очистить",
        danger = true,
        onConfirm = {
            values.clear()
            errors.clear()
            saveSilently(repo, childId, anketaId, values, autoDraft = false)
        },
        onDismiss = { showResetDialog = false }
    )
}

private fun saveSilently(
    repo: Repository, childId: String, anketaId: String,
    values: Map<String, String>, autoDraft: Boolean
) {
    val cleaned = values.filterValues { it.isNotBlank() }
    if (cleaned.isEmpty() && !autoDraft) {
        // не сохраняем пустую сабмишн
        return
    }
    repo.saveSubmission(
        AnketaSubmission(
            childId = childId, anketaId = anketaId, values = cleaned,
            submittedAt = System.currentTimeMillis(),
            submittedBy = if (autoDraft) "draft" else "final"
        )
    )
}

@Composable
private fun RenderField(
    f: Field,
    idx: Int,
    total: Int,
    values: MutableMap<String, String>,
    errors: MutableMap<String, String>,
    onChanged: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$idx/$total", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                buildString {
                    append(f.label)
                    if (f.required) append(" *")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
        f.hint?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        when (f.type) {
            FieldType.INT, FieldType.DECIMAL -> {
                OutlinedTextField(
                    value = values[f.code].orEmpty(),
                    onValueChange = { values[f.code] = it; onChanged() },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (f.type == FieldType.DECIMAL) KeyboardType.Decimal else KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    isError = errors[f.code] != null,
                    supportingText = errors[f.code]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FieldType.TEXT, FieldType.DATE -> {
                OutlinedTextField(
                    value = values[f.code].orEmpty(),
                    onValueChange = { values[f.code] = it; onChanged() },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = errors[f.code] != null,
                    supportingText = errors[f.code]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FieldType.CHOICE -> {
                Column {
                    f.options.forEach { opt ->
                        val selected = values[f.code] == opt.code
                        Surface(
                            onClick = { values[f.code] = opt.code; onChanged() },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Spacer(Modifier.width(6.dp))
                                Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    errors[f.code]?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            FieldType.MULTI -> {
                Column {
                    val current = values[f.code].orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
                    f.options.forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = opt.code in current,
                                onCheckedChange = { on ->
                                    if (on) current.add(opt.code) else current.remove(opt.code)
                                    values[f.code] = current.joinToString(",")
                                    onChanged()
                                }
                            )
                            Text(opt.label)
                        }
                    }
                }
            }
        }
    }
}

private fun validateField(f: Field, v: String): String? {
    if (f.type == FieldType.INT) {
        val n = v.toIntOrNull() ?: return "Введите целое число"
        if (!f.allowNegative && n < 0) return "Отрицательные не допускаются"
    }
    if (f.type == FieldType.DECIMAL) {
        val d = v.replace(",", ".").toDoubleOrNull() ?: return "Введите число (можно дробное)"
        if (!f.allowNegative && d < 0) return "Отрицательные не допускаются"
    }
    if (f.type == FieldType.DATE) {
        if (!v.matches(Regex("""\d{2}\.\d{2}\.\d{4}"""))) return "Формат дд.мм.гггг"
    }
    return null
}
