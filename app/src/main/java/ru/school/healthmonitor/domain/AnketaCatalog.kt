package ru.school.healthmonitor.domain

import ru.school.healthmonitor.domain.FieldType.*
import ru.school.healthmonitor.domain.FillerType.*

/**
 * Полный каталог анкет.
 *
 * Замечание. Тексты вопросов и варианты ответов взяты из ваших docx-файлов
 * (первые страницы). Для нескольких длинных анкет здесь оставлен базовый
 * набор ключевых полей — расширяется в этом файле без правок остального кода:
 * добавили Field в fields — форма и выгрузка подхватят автоматически.
 */
object AnketaCatalog {

    private fun yesNo(code: String, q: String, required: Boolean = true) = Field(
        code, q, CHOICE, listOf(FieldOption("1", "Да"), FieldOption("2", "Нет")), required
    )

    private fun sexField() = Field(
        "sex", "Пол", CHOICE,
        listOf(FieldOption("1", "мужской"), FieldOption("2", "женский")), required = true
    )

    // ─── №1 Физическое здоровье и развитие (медработник, табличная) ─────────
    private val a1 = Anketa(
        id = "1",
        title = "Физическое здоровье и развитие",
        subtitle = "заполняется медицинским работником",
        filler = MEDIC,
        tabular = true,
        fields = listOf(
            Field("mdate", "Дата измерения (дд.мм.гггг)", DATE, required = true),
            Field("height", "Рост (см)", INT, required = true),
            Field("weight", "Вес (кг)", DECIMAL, required = true),
            Field("chest", "Окружность грудной клетки в покое (см)", INT),
            Field("waist", "Окружность талии (см)", INT),
            Field("hips", "Окружность бедер (см)", INT),
            Field("bl_up", "Систолическое АД (мм рт. ст.)", INT),
            Field("bl_lo", "Диастолическое АД (мм рт. ст.)", INT),
            Field("heart", "ЧСС (уд/мин)", INT),
            Field("h_gr", "Группа здоровья", CHOICE, (1..5).map { FieldOption("$it", "$it") }),
            Field("dis", "Есть нарушения здоровья?", CHOICE, listOf(
                FieldOption("1", "есть"), FieldOption("2", "нет"), FieldOption("3", "нет данных")
            ))
        )
    )

    // ─── №2 Пропуски по болезни (медработник, табличная, помесячно) ─────────
    private val a2 = Anketa(
        id = "2",
        title = "Текущая заболеваемость: пропуски по болезни",
        subtitle = "заполняется медработником, за учебный год",
        filler = MEDIC,
        tabular = true,
        fields = listOf(
            Field("height", "Рост (см)", INT),
            Field("weight", "Вес (кг)", DECIMAL),
            Field("sep", "Пропуски сентябрь (дн., -1 если не числился)", INT, allowNegative = true),
            Field("oct", "Пропуски октябрь", INT, allowNegative = true),
            Field("nov", "Пропуски ноябрь", INT, allowNegative = true),
            Field("dec", "Пропуски декабрь", INT, allowNegative = true),
            Field("jan", "Пропуски январь", INT, allowNegative = true),
            Field("feb", "Пропуски февраль", INT, allowNegative = true),
            Field("mar", "Пропуски март", INT, allowNegative = true),
            Field("apr", "Пропуски апрель", INT, allowNegative = true),
            Field("may", "Пропуски май", INT, allowNegative = true)
        )
    )

    // ─── №3 Двигательная подготовленность (физрук, табличная) ───────────────
    private val a3 = Anketa(
        id = "3",
        title = "Двигательная подготовленность",
        subtitle = "заполняется учителем физкультуры",
        filler = PE,
        tabular = true,
        fields = listOf(
            Field("height", "Рост (см)", INT),
            Field("weight", "Вес (кг)", DECIMAL),
            Field("ph_gr", "Физкультурная группа", CHOICE, listOf(
                FieldOption("1", "Основная"),
                FieldOption("2", "Подготовительная"),
                FieldOption("3", "Специальная"),
                FieldOption("4", "Нет данных")
            )),
            Field("run30", "Бег 30 м (с)", DECIMAL),
            Field("ljump", "Прыжок в длину с места (см)", INT),
            Field("run6", "6-минутный бег (м)", INT),
            Field("lift", "Подъём туловища за 1 мин (раз)", INT),
            Field("bend", "Наклон вперёд (см, м.б. отриц.)", INT, allowNegative = true),
            Field("run310", "Челночный бег 3×10 м (с)", DECIMAL),
            Field("romb", "Стойка в линию (проба Ромберга) (с)", INT),
            Field("lhand", "Кистевая динамометрия, левая (кг)", DECIMAL),
            Field("rhand", "Кистевая динамометрия, правая (кг)", DECIMAL)
        )
    )

    // ─── №4 Физическая активность (родитель+ребёнок) ────────────────────────
    private val a4 = Anketa(
        id = "4",
        title = "Физическая активность",
        subtitle = "заполняется обучающимся совместно с родителем",
        filler = PARENT,
        tabular = false,
        fields = listOf(
            Field("mdate", "Дата измерения (дд.мм.гггг)", DATE),
            Field("height", "Рост (см)", INT),
            Field("weight", "Вес (кг)", DECIMAL),
            Field("q1_h", "Часов высокоинтенсивной активности за неделю", INT,
                hint = "футбол, интенсивный бег, езда на велосипеде и т.п."),
            Field("q1_m", "…минут", INT),
            Field("q2_h", "Часов среднеинтенсивной активности за неделю", INT,
                hint = "быстрая ходьба, лёгкий бег, плавание в спокойном темпе"),
            Field("q2_m", "…минут", INT),
            Field("q3_h", "Часов низкоинтенсивной активности за неделю", INT,
                hint = "неспешные прогулки, лёгкая уборка"),
            Field("q3_m", "…минут", INT),
            Field("q4", "Уроков физкультуры в неделю", INT),
            Field("q5", "Занятия в спортивной секции?", CHOICE, listOf(
                FieldOption("1", "Да, регулярно"),
                FieldOption("2", "Да, иногда"),
                FieldOption("3", "Нет")
            ))
        )
    )

    // ─── №5 Питание и распорядок дня (родитель+ребёнок) ─────────────────────
    private val a5 = Anketa(
        id = "5",
        title = "Питание и распорядок дня",
        subtitle = "заполняется обучающимся совместно с родителем",
        filler = PARENT,
        tabular = false,
        fields = listOf(
            Field("q1", "Сколько раз в день ест ребёнок", CHOICE, listOf(
                FieldOption("1", "1–2 раза"),
                FieldOption("2", "3 раза"),
                FieldOption("3", "4 раза"),
                FieldOption("4", "5 и более раз")
            )),
            Field("q2", "Завтракает ли дома?", CHOICE, listOf(
                FieldOption("1", "Каждый день"),
                FieldOption("2", "Иногда"),
                FieldOption("3", "Никогда")
            )),
            Field("q3", "Ест ли горячий обед?", CHOICE, listOf(
                FieldOption("1", "Каждый день"),
                FieldOption("2", "Несколько раз в неделю"),
                FieldOption("3", "Редко или никогда")
            )),
            Field("q4", "Ест ли фрукты/овощи ежедневно?", CHOICE, listOf(
                FieldOption("1", "Да"), FieldOption("2", "Иногда"), FieldOption("3", "Нет")
            )),
            Field("q5", "Пьёт ли сладкие газированные напитки?", CHOICE, listOf(
                FieldOption("1", "Ежедневно"),
                FieldOption("2", "Несколько раз в неделю"),
                FieldOption("3", "Редко"),
                FieldOption("4", "Никогда")
            ))
        )
    )

    // ─── №6 Режим дня (родитель+ребёнок) ────────────────────────────────────
    private val a6 = Anketa(
        id = "6",
        title = "Режим дня",
        subtitle = "заполняется обучающимся совместно с родителем",
        filler = PARENT,
        tabular = false,
        fields = listOf(
            Field("wake", "Во сколько ребёнок обычно просыпается? (чч:мм)", TEXT),
            Field("sleep", "Во сколько ложится спать? (чч:мм)", TEXT),
            Field("sleep_h", "Сколько часов спит ночью", DECIMAL),
            Field("walk", "Сколько минут проводит на улице ежедневно", INT),
            Field("hw", "Сколько часов уходит на домашние задания", DECIMAL),
            Field("free", "Сколько часов свободного времени", DECIMAL)
        )
    )

    // ─── №7 Самочувствие (сам ученик) ───────────────────────────────────────
    private val a7 = Anketa(
        id = "7",
        title = "Самочувствие",
        subtitle = "отвечает обучающийся",
        filler = CHILD,
        tabular = false,
        fields = listOf(
            Field("q1", "Как ты чувствуешь себя обычно?", CHOICE, listOf(
                FieldOption("1", "Хорошо"),
                FieldOption("2", "Бывает по-разному"),
                FieldOption("3", "Часто плохо")
            )),
            Field("q2", "Как часто болит голова?", CHOICE, listOf(
                FieldOption("1", "Никогда или очень редко"),
                FieldOption("2", "Раз в месяц"),
                FieldOption("3", "Раз в неделю"),
                FieldOption("4", "Почти каждый день")
            )),
            Field("q3", "Как часто болит живот?", CHOICE, listOf(
                FieldOption("1", "Никогда/редко"),
                FieldOption("2", "Раз в месяц"),
                FieldOption("3", "Раз в неделю"),
                FieldOption("4", "Почти каждый день")
            )),
            Field("q4", "Быстро ли устаёшь на уроках?", CHOICE, listOf(
                FieldOption("1", "Нет"),
                FieldOption("2", "Иногда"),
                FieldOption("3", "Да, часто")
            ))
        )
    )

    // ─── №8 Психоэмоциональное состояние (сам ученик) ───────────────────────
    private val a8 = Anketa(
        id = "8",
        title = "Психоэмоциональное состояние",
        subtitle = "отвечает обучающийся",
        filler = CHILD,
        tabular = false,
        fields = listOf(
            Field("q1", "Настроение в течение дня?", CHOICE, listOf(
                FieldOption("1", "Обычно хорошее"),
                FieldOption("2", "Меняется"),
                FieldOption("3", "Чаще плохое")
            )),
            Field("q2", "Легко ли тебе засыпать вечером?", CHOICE, listOf(
                FieldOption("1", "Да"),
                FieldOption("2", "Иногда трудно"),
                FieldOption("3", "Часто трудно")
            )),
            Field("q3", "Чувствуешь ли тревогу перед школой?", CHOICE, listOf(
                FieldOption("1", "Нет"),
                FieldOption("2", "Иногда"),
                FieldOption("3", "Часто")
            )),
            Field("q4", "Есть ли друзья в классе?", CHOICE, listOf(
                FieldOption("1", "Да, много"),
                FieldOption("2", "Есть 1–2"),
                FieldOption("3", "Нет")
            ))
        )
    )

    // ─── №9 Использование электронных устройств (родитель+ребёнок) ──────────
    private val a9 = Anketa(
        id = "9",
        title = "Использование электронных устройств",
        subtitle = "заполняется обучающимся совместно с родителем",
        filler = PARENT,
        tabular = false,
        fields = listOf(
            Field("phone_h", "Часов в день со смартфоном", DECIMAL),
            Field("pc_h", "Часов в день за компьютером/планшетом", DECIMAL),
            Field("tv_h", "Часов в день у телевизора", DECIMAL),
            Field("games_h", "Часов в день в видеоиграх", DECIMAL),
            Field("soc_h", "Часов в день в соцсетях/мессенджерах", DECIMAL),
            Field("bed", "Пользуется экраном перед сном?", CHOICE, listOf(
                FieldOption("1", "Каждый вечер"),
                FieldOption("2", "Иногда"),
                FieldOption("3", "Нет")
            ))
        )
    )

    // ─── №10 Социально-демографическая (родитель) ───────────────────────────
    private val a10 = Anketa(
        id = "10",
        title = "Социально-демографическая",
        subtitle = "заполняется родителем или опекуном",
        filler = PARENT,
        tabular = false,
        fields = listOf(
            Field("mdate", "Дата измерения (дд.мм.гггг)", DATE),
            Field("height", "Рост (см)", INT),
            Field("weight", "Вес (кг)", DECIMAL),
            Field("family", "Состав семьи", CHOICE, listOf(
                FieldOption("1", "Полная"), FieldOption("2", "Неполная")
            ), required = true),
            Field("m_age", "Возраст матери (опекуна)", CHOICE, listOf(
                FieldOption("1", "до 29"),
                FieldOption("2", "30–34"),
                FieldOption("3", "35–39"),
                FieldOption("4", "40–44"),
                FieldOption("5", "45 и старше")
            )),
            Field("f_age", "Возраст отца", CHOICE, listOf(
                FieldOption("1", "до 29"),
                FieldOption("2", "30–34"),
                FieldOption("3", "35–39"),
                FieldOption("4", "40–44"),
                FieldOption("5", "45 и старше"),
                FieldOption("6", "нет данных")
            )),
            Field("m_edu", "Образование матери", CHOICE, listOf(
                FieldOption("1", "Основное общее"),
                FieldOption("2", "Среднее"),
                FieldOption("3", "Среднее профессиональное"),
                FieldOption("4", "Высшее")
            )),
            Field("f_edu", "Образование отца", CHOICE, listOf(
                FieldOption("1", "Основное общее"),
                FieldOption("2", "Среднее"),
                FieldOption("3", "Среднее профессиональное"),
                FieldOption("4", "Высшее"),
                FieldOption("5", "нет данных")
            )),
            Field("kids", "Число детей в семье", INT),
            Field("income", "Оценка достатка семьи", CHOICE, listOf(
                FieldOption("1", "Ниже среднего"),
                FieldOption("2", "Средний"),
                FieldOption("3", "Выше среднего")
            ))
        )
    )

    // ─── №11 Успеваемость (классный руководитель, табличная) ────────────────
    private val a11 = Anketa(
        id = "11",
        title = "Успеваемость",
        subtitle = "заполняется классным руководителем",
        filler = HOMEROOM,
        tabular = true,
        fields = listOf(
            Field("height", "Рост (см)", INT),
            Field("weight", "Вес (кг)", DECIMAL),
            Field("score", "Средний балл за учебный год", DECIMAL, required = true)
        )
    )

    val all: List<Anketa> = listOf(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
    fun byId(id: String): Anketa = all.first { it.id == id }

    /** Анкеты, доступные родителю (индивидуальные, включая «детские» 7, 8). */
    val parentAnketas: List<Anketa> = all.filter { !it.tabular }

    /** Анкеты сотрудника по его роли. */
    fun forTeacherRole(r: ru.school.healthmonitor.data.TeacherRole): List<Anketa> = when (r) {
        ru.school.healthmonitor.data.TeacherRole.MEDIC -> listOf(a1, a2)
        ru.school.healthmonitor.data.TeacherRole.PE -> listOf(a3)
        ru.school.healthmonitor.data.TeacherRole.HOMEROOM -> listOf(a11)
        ru.school.healthmonitor.data.TeacherRole.ADMIN -> all  // админу — всё
    }
}
