package ru.school.healthmonitor.data

import kotlinx.serialization.Serializable

@Serializable
data class SchoolClass(
    val id: String,           // uuid
    val region: String,       // напр. "Кемеровская область – Кузбасс"
    val school: String,       // напр. "Кемерово: МАОУ СОШ № 36"
    val grade: Int,           // 5, 6, 7
    val letter: String,       // "А", "Б", "5А" — как в макете (class)
    val inviteCode: String    // напр. "36KEM-5A" — код приглашения родителей
)

@Serializable
data class Child(
    val id: String,           // uuid
    val classId: String,
    val name1: String,        // Ф — первая буква фамилии
    val name2: String,        // И — первая буква имени
    val name3: String,        // О — первая буква отчества
    val bdate: String,        // дд.мм.гггг
    val sex: Int              // 1 = м, 2 = ж
) {
    val displayName: String get() = "$name1$name2$name3 ($bdate)"
}

@Serializable
data class TeacherAccount(
    val id: String,
    val login: String,
    val password: String,     // MVP: plain (в проде — хеш)
    val fullName: String,
    val role: TeacherRole,
    val classIds: List<String> // назначенные классы
)

enum class TeacherRole { HOMEROOM, MEDIC, PE, ADMIN }

/** Ответы одной анкеты по одному ребёнку. Ключ = код поля из макета (height, weight, run30 и т.д.). */
@Serializable
data class AnketaSubmission(
    val childId: String,
    val anketaId: String,         // "1", "2" ... "11"
    val values: Map<String, String>,
    val submittedAt: Long,        // timestamp
    val finalized: Boolean = false // true — нажали «Сохранить и отправить»; false — автосохранённый черновик
) {
    /** Есть ли вообще заполненные данные. */
    val hasData: Boolean get() = values.values.any { it.isNotBlank() }
}

@Serializable
data class AppState(
    val classes: List<SchoolClass> = emptyList(),
    val children: List<Child> = emptyList(),
    val teachers: List<TeacherAccount> = emptyList(),
    val submissions: List<AnketaSubmission> = emptyList()
)
