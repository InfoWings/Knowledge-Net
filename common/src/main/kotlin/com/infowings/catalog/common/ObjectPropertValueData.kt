package com.infowings.catalog.common

data class Range(val left: Int, val right: Int)

/*
  У скалярного поля может быть одно значение.
  Скалярное - в смысле не ссылка на объект/субъект/элемент
  Термин взят из раздела "Нефукнциональные требования"

  В этом смысле - кортеж, задающий параметры графика функции - скалярное значение
   И это будет CompoundValue

  typeName - имя типа. Оно нужно для того, чтобы при поиске отличать одинаковые значения, принадлежание
  разным типам.

  Например, чтобы при поиске объектов, в которых упонинается температура 16 градусов по Цельсию,
  в выдачу не попадали ключи или болты на 16. И чтобы кривые Безье отличались от любых других кортежей

 */
sealed class ScalarValue {
    class IntegerValue(val value: Int) : ScalarValue()
    class StringValue(val value: String) : ScalarValue()
    class CompoundValue(val value: Any) : ScalarValue()
}

sealed class ObjectValueData {
    data class Scalar(val value: ScalarValue?, val range: Range?, val precision: Int?) : ObjectValueData()
    data class Reference(val value: ReferenceValueData) : ObjectValueData()
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
    val value: ObjectValueData,
    val objectPropertyId: String,
    val aspectPropertyId: String,
    val parentValueId: String?,
    val measureId: String?
)