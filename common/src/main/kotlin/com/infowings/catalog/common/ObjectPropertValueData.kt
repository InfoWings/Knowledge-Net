package com.infowings.catalog.common

data class Range(val left: Int, val right: Int)

enum class SimpleTypeGroup {
    INTEGER, STRING, COMPOUND
}

sealed class ScalarValue(val typeGroup: SimpleTypeGroup, val typeName: String) {
    class IntegerValue(val value: Int, typeName: String) : ScalarValue(SimpleTypeGroup.INTEGER, typeName)
    class StringValue(val value: String, typeName: String) : ScalarValue(SimpleTypeGroup.STRING, typeName)
    class CompoundValue(val value: Any, typeName: String) : ScalarValue(SimpleTypeGroup.COMPOUND, typeName)
}

enum class ReferenceTypeGroup {
    SUBJECT, OBJECT, DOMAIN_ELEMENT
}

data class ReferenceValueData(val typeGroup: ReferenceTypeGroup, val id: String)

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
    val scalarValue: ScalarValue?,
    val range: Range?,
    val precision: Int?,
    val objectPropertyId: String,
    val rootCharacteristicId: String,
    val parentValueId: String?,
    val referenceValue: ReferenceValueData?,
    val measureId: String?
)