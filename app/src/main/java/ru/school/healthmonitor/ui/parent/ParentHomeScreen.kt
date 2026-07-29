package ru.school.healthmonitor.ui.parent

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

@Composable
fun ParentHomeScreen(nav: NavController, childId: String) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsState()

    val child = state.children.firstOrNull { it.id == childId }
    val sc = child?.let { repo.classById(it.classId) }

    AppScaffold("Анкеты ребёнка", nav) { pv ->
        if (child == null) {
            Text("Ребёнок не найден", Modifier.padding(pv).padding(16.dp))
            return@AppScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard("Ребёнок") {
                Text("${child.displayName}  ·  ${if (child.sex == 1) "м" else "ж"}")
                if (sc != null) Text("${sc.school} · ${sc.letter}",
                    style = MaterialTheme.typography.bodySmall)
            }

            val done = AnketaCatalog.parentAnketas.count {
                state.submissions.any { s -> s.childId == child.id && s.anketaId == it.id }
            }
            Text("Заполнено $done из ${AnketaCatalog.parentAnketas.size} анкет",
                style = MaterialTheme.typography.titleMedium)

            AnketaCatalog.parentAnketas.forEach { anketa ->
                val filled = state.submissions.any { it.childId == child.id && it.anketaId == anketa.id }
                ElevatedCard(
                    onClick = { nav.navigate("anketa/${child.id}/${anketa.id}") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filled) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        else Icon(Icons.Outlined.RadioButtonUnchecked, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("№${anketa.id}. ${anketa.title}",
                                style = MaterialTheme.typography.titleSmall)
                            Text(anketa.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
