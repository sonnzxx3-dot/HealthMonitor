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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.school.healthmonitor.data.Repository
import ru.school.healthmonitor.domain.AnketaCatalog
import ru.school.healthmonitor.ui.common.AppScaffold
import ru.school.healthmonitor.ui.common.EmptyState
import ru.school.healthmonitor.ui.common.ProgressStrip
import ru.school.healthmonitor.ui.common.SectionCard

/**
 * Табличная анкета (№1, 2, 3, 11): список детей класса. По каждому — статус
 * заполнения и переход в полную форму той же анкеты. Строка = ребёнок,
 * как в исходных макетах.
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
    val anketa = remember(anketaId) { AnketaCatalog.byId(anketaId) }
    val sc = repo.classById(classId)
    val kids = state.children.filter { it.classId == classId }

    val done = kids.count { c ->
        state.submissions.any { it.childId == c.id && it.anketaId == anketaId && it.finalized }
    }

    AppScaffold("№${anketa.id}. ${anketa.title}", nav) { pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard(sc?.let { "${it.school} · ${it.letter}" } ?: "Класс") {
                Text(anketa.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                ProgressStrip(done, kids.size.coerceAtLeast(1))
                Spacer(Modifier.height(6.dp))
                Text("Нажмите на ученика, чтобы заполнить его показатели.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (kids.isEmpty()) {
                EmptyState(
                    icon = "👥",
                    title = "В классе нет учеников",
                    description = "Родители добавляют детей сами по коду класса${sc?.let { " (${it.inviteCode})" } ?: ""}."
                )
            } else {
                kids.forEach { child ->
                    val sub = state.submissions.firstOrNull {
                        it.childId == child.id && it.anketaId == anketaId
                    }
                    val finalized = sub?.finalized == true
                    val hasDraft = sub != null && !finalized && sub.hasData
                    ElevatedCard(
                        onClick = { nav.navigate("anketa/${child.id}/$anketaId") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (finalized) Icon(Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            else Icon(Icons.Outlined.RadioButtonUnchecked, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(child.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                if (hasDraft) Text("• черновик",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                            Text("›", style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
