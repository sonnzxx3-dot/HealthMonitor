package ru.school.healthmonitor.ui.anketa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.AnketaSubmission
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.domain.Field
import ru.school.healthmonitor.domain.FieldType
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

@Composable
fun AnketaFormScreen(
    nav: NavController,
    childId: String,
    anketaId: String,
    submittedByLabel: String
) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val anketa = AnketaCatalog.byId(anketaId)
    val existing = repo.submissionFor(childId, anketaId)

    val values = remember { mutableStateMapOf<String, String>().apply { existing?.values?.let { putAll(it) } } }
    val errors = remember { mutableStateMapOf<String, String>() }
    var savedNote by remember { mutableStateOf<String?>(null) }

    AppScaffold("№${anketa.id}. ${anketa.title}", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(anketa.subtitle, style = MaterialTheme.typography.bodySmall)
            savedNote?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            anketa.fields.forEach { f -> RenderField(f, values, errors) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    errors.clear()
                    for (f in anketa.fields) {
                        val v = values[f.code].orEmpty().trim()
                        if (f.required && v.isBlank()) errors[f.code] = "Обязательное поле"
                        else if (v.isNotBlank()) validateNumeric(f, v)?.let { errors[f.code] = it }
                    }
                    if (errors.isEmpty()) {
                        repo.saveSubmission(
                            AnketaSubmission(
                                childId = childId,
                                anketaId = anketaId,
                                values = values.toMap(),
                                submittedAt = System.currentTimeMillis(),
                                submittedBy = submittedByLabel
                            )
                        )
                        savedNote = "✓ Анкета сохранена. Можно вернуться назад."
                    } else savedNote = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existing == null) "Сохранить и отправить" else "Обновить") }
        }
    }
}

@Composable
private fun RenderField(
    f: Field,
    values: MutableMap<String, String>,
    errors: MutableMap<String, String>
) {
    SectionCard(f.label + if (f.required) " *" else "") {
        f.hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
        }
        when (f.type) {
            FieldType.INT, FieldType.DECIMAL -> {
                OutlinedTextField(
                    value = values[f.code].orEmpty(),
                    onValueChange = { values[f.code] = it; errors.remove(f.code) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (f.type == FieldType.DECIMAL) KeyboardType.Decimal else KeyboardType.Number
                    ),
                    isError = errors[f.code] != null,
                    supportingText = errors[f.code]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FieldType.TEXT, FieldType.DATE -> {
                OutlinedTextField(
                    value = values[f.code].orEmpty(),
                    onValueChange = { values[f.code] = it; errors.remove(f.code) },
                    singleLine = true,
                    isError = errors[f.code] != null,
                    supportingText = errors[f.code]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FieldType.CHOICE -> {
                Column {
                    f.options.forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = values[f.code] == opt.code,
                                onClick = { values[f.code] = opt.code; errors.remove(f.code) }
                            )
                            Text(opt.label)
                        }
                    }
                    errors[f.code]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            FieldType.MULTI -> {
                Column {
                    val current = values[f.code].orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
                    f.options.forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = opt.code in current,
                                onCheckedChange = { on ->
                                    if (on) current.add(opt.code) else current.remove(opt.code)
                                    values[f.code] = current.joinToString(",")
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

private fun validateNumeric(f: Field, v: String): String? {
    if (f.type == FieldType.INT) {
        val parsed = v.toIntOrNull() ?: return "Введите целое число"
        if (!f.allowNegative && parsed < 0) return "Отрицательные не допускаются"
    }
    if (f.type == FieldType.DECIMAL) {
        val d = v.replace(",", ".").toDoubleOrNull() ?: return "Введите число (можно дробное)"
        if (!f.allowNegative && d < 0) return "Отрицательные не допускаются"
    }
    return null
}
