package ru.school.healthmonitor.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Пакет данных для передачи от родителя к учителю (JSON-обмен). */
@Serializable
data class ChildPackage(
    val classInviteCode: String,
    val child: Child,
    val submissions: List<AnketaSubmission>,
    val exportedAt: Long
)

class Repository private constructor(private val ctx: Context) {

    private val file = File(ctx.filesDir, "state.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun load(): AppState = try {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText())
        else seedInitial()
    } catch (_: Throwable) { seedInitial() }

    private fun seedInitial(): AppState {
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
    fun deleteClass(classId: String) = update { st ->
        val childIds = st.children.filter { it.classId == classId }.map { it.id }.toSet()
        st.copy(
            classes = st.classes.filterNot { it.id == classId },
            children = st.children.filterNot { it.classId == classId },
            submissions = st.submissions.filterNot { it.childId in childIds },
            teachers = st.teachers.map { t -> t.copy(classIds = t.classIds - classId) }
        )
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
    fun deleteChild(childId: String) = update { st ->
        st.copy(
            children = st.children.filterNot { it.id == childId },
            submissions = st.submissions.filterNot { it.childId == childId }
        )
    }
    fun childrenOfClass(classId: String): List<Child> =
        _state.value.children.filter { it.classId == classId }
    fun childById(id: String): Child? = _state.value.children.firstOrNull { it.id == id }

    // ─── Учителя ────────────────────────────────────────────────
    fun addTeacher(login: String, password: String, fullName: String,
                   role: TeacherRole, classIds: List<String>): TeacherAccount {
        val t = TeacherAccount(UUID.randomUUID().toString(), login, password, fullName, role, classIds)
        update { it.copy(teachers = it.teachers + t) }
        return t
    }
    fun deleteTeacher(id: String) = update { it.copy(teachers = it.teachers.filterNot { t -> t.id == id }) }
    fun authTeacher(login: String, pass: String): TeacherAccount? =
        _state.value.teachers.firstOrNull { it.login == login && it.password == pass }
    fun teacherById(id: String): TeacherAccount? = _state.value.teachers.firstOrNull { it.id == id }

    // ─── Ответы ─────────────────────────────────────────────────
    fun saveSubmission(sub: AnketaSubmission) = update { st ->
        val filtered = st.submissions.filterNot { it.childId == sub.childId && it.anketaId == sub.anketaId }
        st.copy(submissions = filtered + sub)
    }
    fun deleteSubmission(childId: String, anketaId: String) = update { st ->
        st.copy(submissions = st.submissions.filterNot { it.childId == childId && it.anketaId == anketaId })
    }
    fun submissionFor(childId: String, anketaId: String): AnketaSubmission? =
        _state.value.submissions.firstOrNull { it.childId == childId && it.anketaId == anketaId }
    fun submissionsForClass(classId: String, anketaId: String): List<AnketaSubmission> {
        val childIds = childrenOfClass(classId).map { it.id }.toSet()
        return _state.value.submissions.filter { it.anketaId == anketaId && it.childId in childIds }
    }

    // ─── Обмен родитель ↔ учитель через JSON ────────────────────
    fun exportChildPackage(childId: String): File? {
        val c = childById(childId) ?: return null
        val sc = classById(c.classId) ?: return null
        val subs = _state.value.submissions.filter { it.childId == childId }
        val pkg = ChildPackage(sc.inviteCode, c, subs, System.currentTimeMillis())
        val dir = File(ctx.filesDir, "exports").also { it.mkdirs() }
        val out = File(dir, "child_${c.name1}${c.name2}${c.name3}_${System.currentTimeMillis()}.hmpkg.json")
        out.writeText(json.encodeToString(ChildPackage.serializer(), pkg))
        return out
    }

    fun importChildPackage(uri: Uri): ImportResult {
        return try {
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return ImportResult.Error("Не удалось открыть файл")
            val pkg = json.decodeFromString(ChildPackage.serializer(), text)
            val cls = classByCode(pkg.classInviteCode)
                ?: return ImportResult.Error("Класс с кодом ${pkg.classInviteCode} не найден в вашей базе")

            update { st ->
                val existing = st.children.firstOrNull { c ->
                    c.classId == cls.id && c.name1 == pkg.child.name1 && c.name2 == pkg.child.name2 &&
                            c.name3 == pkg.child.name3 && c.bdate == pkg.child.bdate
                }
                val childToUse = existing ?: pkg.child.copy(
                    id = UUID.randomUUID().toString(),
                    classId = cls.id
                )
                val newChildren = if (existing == null) st.children + childToUse else st.children
                val remapped = pkg.submissions.map { it.copy(childId = childToUse.id) }
                val filteredExisting = st.submissions.filterNot { s ->
                    s.childId == childToUse.id && remapped.any { it.anketaId == s.anketaId }
                }
                st.copy(children = newChildren, submissions = filteredExisting + remapped)
            }
            ImportResult.Ok(pkg.child.displayName, pkg.submissions.size)
        } catch (t: Throwable) {
            ImportResult.Error("Ошибка чтения: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    sealed class ImportResult {
        data class Ok(val childName: String, val submissionsCount: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    // ─── Сброс ─────────────────────────────────────────────────
    fun wipeAll() {
        _state.value = seedInitial()
    }

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
