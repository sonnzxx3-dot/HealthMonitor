package ru.school.healthmonitor.domain

/** Тип поля анкеты. */
enum class FieldType {
    INT,        // целое число
    DECIMAL,    // дробное число
    TEXT,       // короткий текст
    DATE,       // дд.мм.гггг
    CHOICE,     // одиночный выбор (options)
    MULTI       // множественный выбор
}

data class FieldOption(val code: String, val label: String)

data class Field(
    val code: String,           // код поля из макета (height, weight, run30, q1 и т.д.)
    val label: String,          // текст вопроса / подпись
    val type: FieldType,
    val options: List<FieldOption> = emptyList(),
    val required: Boolean = false,
    val hint: String? = null,
    val allowNegative: Boolean = false
)

data class Anketa(
    val id: String,             // "1".."11"
    val title: String,
    val subtitle: String,       // кем заполняется
    val filler: FillerType,     // кто заполняет
    val tabular: Boolean,       // табличная (строка = ребёнок) vs индивидуальная
    val fields: List<Field>
)

enum class FillerType {
    PARENT,           // родитель совместно с ребёнком
    CHILD,            // сам ученик
    HOMEROOM,         // классный руководитель
    MEDIC,            // медработник
    PE                // учитель физкультуры
}
