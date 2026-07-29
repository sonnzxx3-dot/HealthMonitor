package ru.school.healthmonitor.ui.anketa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.SectionCard

/**
 * Табличная анкета: список детей класса, у каждого — быстрый переход
 * в форму той же анкеты (переиспользуем AnketaFormScreen).
 * Соответствует макетам № 1, 2, 3, 11.
 */
@Composable
fun TableAnketaScreen(
    nav: NavController,
    teacherId: String,
    classId: String,
    anketaId: String
) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()
    val anketa = AnketaCatalog.byId(anketaId)
    val sc = repo.classById(classId)
    val kids = state.children.filter { it.classId == classId }

    AppScaffold("№${anketa.id}. ${anketa.title}", nav) { pv ->
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard(sc?.let { "${it.school} · ${it.letter}" } ?: "Класс") {
                Text(anketa.subtitle, style = MaterialTheme.typography.bodySmall)
                val done = kids.count { c ->
                    state.submissions.any { it.childId == c.id && it.anketaId == anketaId }
                }
                Text("Заполнено $done из ${kids.size}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (kids.isEmpty()) {
                Text("В классе пока нет детей — попросите родителей зарегистрироваться по коду класса.")
            } else {
                kids.forEach { child ->
                    val filled = state.submissions.any { it.childId == child.id && it.anketaId == anketaId }
                    ElevatedCard(
                        onClick = { nav.navigate("anketa/${child.id}/$anketaId") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (filled) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            else Icon(Icons.Outlined.RadioButtonUnchecked, null)
                            Spacer(Modifier.width(12.dp))
                            Text(child.displayName)
                        }
                    }
                }
            }
        }
    }
}
