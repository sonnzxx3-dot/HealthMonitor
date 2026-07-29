package ru.school.healthmonitor

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.school.healthmonitor.ui.anketa.AnketaFormScreen
import ru.school.healthmonitor.ui.anketa.TableAnketaScreen
import ru.school.healthmonitor.ui.auth.RoleScreen
import ru.school.healthmonitor.ui.auth.ParentLoginScreen
import ru.school.healthmonitor.ui.auth.TeacherLoginScreen
import ru.school.healthmonitor.ui.parent.ParentHomeScreen
import ru.school.healthmonitor.ui.parent.ChildFormScreen
import ru.school.healthmonitor.ui.teacher.TeacherHomeScreen
import ru.school.healthmonitor.ui.teacher.ClassRosterScreen
import ru.school.healthmonitor.ui.teacher.ExportScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        dynamicLightColorScheme(ctx)
    else
        lightColorScheme(primary = Color(0xFF1565C0))
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "role") {
                composable("role") { RoleScreen(nav) }
                composable("parent/login") { ParentLoginScreen(nav) }
                composable("teacher/login") { TeacherLoginScreen(nav) }

                composable("parent/home/{childId}") { entry ->
                    ParentHomeScreen(nav, entry.arguments?.getString("childId") ?: "")
                }
                composable("parent/newchild/{classId}") { entry ->
                    ChildFormScreen(nav, entry.arguments?.getString("classId") ?: "")
                }
                composable("anketa/{childId}/{anketaId}") { entry ->
                    AnketaFormScreen(
                        nav,
                        entry.arguments?.getString("childId") ?: "",
                        entry.arguments?.getString("anketaId") ?: "",
                        submittedByLabel = "parent"
                    )
                }

                composable("teacher/home/{teacherId}") { entry ->
                    TeacherHomeScreen(nav, entry.arguments?.getString("teacherId") ?: "")
                }
                composable("teacher/roster/{teacherId}/{classId}") { entry ->
                    ClassRosterScreen(
                        nav,
                        entry.arguments?.getString("teacherId") ?: "",
                        entry.arguments?.getString("classId") ?: ""
                    )
                }
                composable("teacher/table/{teacherId}/{classId}/{anketaId}") { entry ->
                    TableAnketaScreen(
                        nav,
                        entry.arguments?.getString("teacherId") ?: "",
                        entry.arguments?.getString("classId") ?: "",
                        entry.arguments?.getString("anketaId") ?: ""
                    )
                }
                composable("teacher/export/{teacherId}/{classId}") { entry ->
                    ExportScreen(
                        nav,
                        entry.arguments?.getString("teacherId") ?: "",
                        entry.arguments?.getString("classId") ?: ""
                    )
                }
            }
        }
    }
}
