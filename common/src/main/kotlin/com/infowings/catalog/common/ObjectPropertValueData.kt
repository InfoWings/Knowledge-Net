package com.infowings.catalog.common

/* Базовое представление диапазона */
data class Range(val left: Int, val right: Int)

/* Представление разных вариантов значений, пригодное для использования на фронте,
 * но не для сериализации в json
 */
sealed class ObjectValueData {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValueData()
    data class StringValue(val value: String) : ObjectValueData()
    data class RangeValue(val range: Range) : ObjectValueData()
    data class DecimalValue(val valueRepr: String) : ObjectValueData()
    data class Link(val value: LinkValueData) : ObjectValueData()
}


/* Представление ссылочного значения в виде, пригодном для использования на фронте
 * id - строковое предсталвение vertex id объекта/субъекта/значения домена
 * typeGroup - маркер с информацией о том, куда этот id показывает
 */
sealed class LinkValueData(open val id: String) {
    class Subject(override val id: String) : LinkValueData(id)
    class Object(override val id: String) : LinkValueData(id)
    class DomainElement(override val id: String) : LinkValueData(id)
}

data class ObjectPropertyValueData(
    val id: String?,
    /* Простые типа - это что-то типа челых или перечислений
    Сложные - что-то типа графиков функций. Кортежи, списки и т.п.

    Пока задачу кодирования отдаем фронту. Возможно, это не идеально эффективно (будет двойная сериализация), но более
    надежно.
    Альтернатива - где-то в бекенде разьираться с двумя зоопарками типов (простыми и составными), но тогда
     ObjectPropertyValueData должен стать дженериком с двумя параметрами и не факт, чтро стандартная котлиновская
     json-сериализация с этим справится.
     */
    val value: ObjectValueData,
    val objectPropertyId: String,
    val aspectPropertyId: String,
    val parentValueId: String?,
    val measureId: String?
)
