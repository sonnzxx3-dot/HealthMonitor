package ru.school.healthmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.school.healthmonitor.ui.admin.AdminScreen
import ru.school.healthmonitor.ui.admin.NewClassScreen
import ru.school.healthmonitor.ui.admin.NewTeacherScreen
import ru.school.healthmonitor.ui.anketa.AnketaFormScreen
import ru.school.healthmonitor.ui.anketa.TableAnketaScreen
import ru.school.healthmonitor.ui.auth.ParentLoginScreen
import ru.school.healthmonitor.ui.auth.RoleScreen
import ru.school.healthmonitor.ui.auth.TeacherLoginScreen
import ru.school.healthmonitor.ui.parent.ChildFormScreen
import ru.school.healthmonitor.ui.parent.ParentHomeScreen
import ru.school.healthmonitor.ui.teacher.ClassRosterScreen
import ru.school.healthmonitor.ui.teacher.ExportScreen
import ru.school.healthmonitor.ui.teacher.TeacherHomeScreen
import ru.school.healthmonitor.ui.theme.HealthMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    HealthMonitorTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "role") {
                composable("role") { RoleScreen(nav) }
                composable("parent/login") { ParentLoginScreen(nav) }
                composable("teacher/login") { TeacherLoginScreen(nav) }

                composable("parent/home/{childId}") { e ->
                    ParentHomeScreen(nav, e.arguments?.getString("childId").orEmpty())
                }
                composable("parent/newchild/{classId}") { e ->
                    ChildFormScreen(nav, e.arguments?.getString("classId").orEmpty())
                }
                composable("anketa/{childId}/{anketaId}") { e ->
                    AnketaFormScreen(
                        nav,
                        e.arguments?.getString("childId").orEmpty(),
                        e.arguments?.getString("anketaId").orEmpty()
                    )
                }

                composable("teacher/home/{teacherId}") { e ->
                    TeacherHomeScreen(nav, e.arguments?.getString("teacherId").orEmpty())
                }
                composable("teacher/roster/{teacherId}/{classId}") { e ->
                    ClassRosterScreen(
                        nav,
                        e.arguments?.getString("teacherId").orEmpty(),
                        e.arguments?.getString("classId").orEmpty()
                    )
                }
                composable("teacher/table/{teacherId}/{classId}/{anketaId}") { e ->
                    TableAnketaScreen(
                        nav,
                        e.arguments?.getString("teacherId").orEmpty(),
                        e.arguments?.getString("classId").orEmpty(),
                        e.arguments?.getString("anketaId").orEmpty()
                    )
                }
                composable("teacher/export/{teacherId}/{classId}") { e ->
                    ExportScreen(
                        nav,
                        e.arguments?.getString("teacherId").orEmpty(),
                        e.arguments?.getString("classId").orEmpty()
                    )
                }
                composable("admin/{teacherId}") { e ->
                    AdminScreen(nav, e.arguments?.getString("teacherId").orEmpty())
                }
                composable("admin/{teacherId}/newclass") { e ->
                    NewClassScreen(nav, e.arguments?.getString("teacherId").orEmpty())
                }
                composable("admin/{teacherId}/newteacher") { e ->
                    NewTeacherScreen(nav, e.arguments?.getString("teacherId").orEmpty())
                }
            }
        }
    }
}
