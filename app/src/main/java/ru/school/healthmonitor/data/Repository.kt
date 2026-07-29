package ru.school.healthmonitor.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class Repository private constructor(context: Context) {

    private val file = File(context.filesDir, "state.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun load(): AppState = try {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText())
        else seedInitial()
    } catch (_: Throwable) { seedInitial() }

    private fun seedInitial(): AppState {
        // Демо-класс + демо-аккаунт учителя, чтобы можно было сразу пощупать.
        val classId = UUID.randomUUID().toString()
        val demo = AppState(
            classes = listOf(
                SchoolClass(
                    id = classId,
                    region = "Кемеровская область – Кузбасс",
                    school = "Кемерово: МАОУ СОШ № 36",
                    grade = 5,
                    letter = "5А",
                    inviteCode = "36KEM-5A"
                )
            ),
            teachers = listOf(
                TeacherAccount(
                    id = UUID.randomUUID().toString(),
                    login = "admin",
                    password = "admin",
                    fullName = "Администратор школы",
                    role = TeacherRole.ADMIN,
                    classIds = listOf(classId)
                )
            )
        )
        save(demo)
        return demo
    }

    private fun save(s: AppState) {
        file.writeText(json.encodeToString(AppState.serializer(), s))
    }

    private fun update(mutator: (AppState) -> AppState) {
        val next = mutator(_state.value)
        _state.value = next
        save(next)
    }

    // ─── Классы ─────────────────────────────────────────────────
    fun addClass(region: String, school: String, grade: Int, letter: String, code: String): SchoolClass {
        val sc = SchoolClass(UUID.randomUUID().toString(), region, school, grade, letter, code)
        update { it.copy(classes = it.classes + sc) }
        return sc
    }
    fun classByCode(code: String): SchoolClass? =
        _state.value.classes.firstOrNull { it.inviteCode.equals(code.trim(), ignoreCase = true) }
    fun classById(id: String): SchoolClass? = _state.value.classes.firstOrNull { it.id == id }

    // ─── Дети ───────────────────────────────────────────────────
    fun addChild(classId: String, n1: String, n2: String, n3: String, bdate: String, sex: Int): Child {
        val c = Child(UUID.randomUUID().toString(), classId, n1, n2, n3, bdate, sex)
        update { it.copy(children = it.children + c) }
        return c
    }
    fun childrenOfClass(classId: String): List<Child> =
        _state.value.children.filter { it.classId == classId }
    fun childById(id: String): Child? = _state.value.children.firstOrNull { it.id == id }

    // ─── Учителя ────────────────────────────────────────────────
    fun addTeacher(t: TeacherAccount) = update { it.copy(teachers = it.teachers + t) }
    fun authTeacher(login: String, pass: String): TeacherAccount? =
        _state.value.teachers.firstOrNull { it.login == login && it.password == pass }

    // ─── Ответы ─────────────────────────────────────────────────
    fun saveSubmission(sub: AnketaSubmission) = update { st ->
        val filtered = st.submissions.filterNot { it.childId == sub.childId && it.anketaId == sub.anketaId }
        st.copy(submissions = filtered + sub)
    }
    fun submissionFor(childId: String, anketaId: String): AnketaSubmission? =
        _state.value.submissions.firstOrNull { it.childId == childId && it.anketaId == anketaId }
    fun submissionsForClass(classId: String, anketaId: String): List<AnketaSubmission> {
        val childIds = childrenOfClass(classId).map { it.id }.toSet()
        return _state.value.submissions.filter { it.anketaId == anketaId && it.childId in childIds }
    }

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
