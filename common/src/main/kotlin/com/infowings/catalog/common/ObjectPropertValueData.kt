package com.infowings.catalog.common

/* Базовое представление диапазона */
data class Range(val left: Int, val right: Int)

/* Представление разных вариантов значений, пригодное для использования на фронте,
 * но не для сериализации в json
 */
sealed class ObjectValueData {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValueData()
    data class StringValue(val value: String) : ObjectValueData()
    data class CompoundValue(val value: Any) : ObjectValueData()
    data class RangeValue(val range: Range) : ObjectValueData()
    data class DecimalValue(val valueRepr: String) : ObjectValueData()
    data class Link(val value: LinkValueData) : ObjectValueData()
}


enum class LinkTypeGroup {
    SUBJECT, OBJECT, DOMAIN_ELEMENT
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

/*
  Теги для компактного JSON-представления
  Скалярные значения будем представлять на одном уровне со ссылочными

  Верояность конфликта тегов (то есть вероятность того, что захочется иметь скалярный тип с именем
   SUBJECT/OBJECT/DOMAIN_ELEMENT) невысока.
  Ею модно пренебречь ради того, чтобы не создавать в json-структуре дополнительный уровень вложенности для ссылочных
  типов

 */
enum class ValueDTOTags {
    INTEGER,
    STRING,
    COMPOUND,
    RANGE,
    SUBJECT,
    OBJECT,
    DOMAIN_ELEMENT,
}

/* json-представление скалярного значения. */
data class ScalarDTO(
    val stringValue: String?,
    val intValue: Int?,
    val data: Any?,
    val range: Range?,
    val precision: Int?
)

/*  json-представление произвольного значения */
data class ValueDTO(val tag: String, val vertexId: String?, val scalar: ScalarDTO?)

/* заполнители */
fun stringValueDto(value: String) = ValueDTO(ValueDTOTags.STRING.name, null, ScalarDTO(value, null, null, null, null))

fun integerValueDto(value: Int, precision: Int?) =
    ValueDTO(ValueDTOTags.INTEGER.name, null, ScalarDTO(null, value, null, null, precision))

fun compoundValueDto(value: Any) = ValueDTO(ValueDTOTags.COMPOUND.name, null, ScalarDTO(null, null, value, null, null))
fun referenceValueDto(tag: ValueDTOTags, id: String) = ValueDTO(tag.name, id, null)
fun rangeValueDto(value: Range) = ValueDTO(ValueDTOTags.RANGE.name, null, ScalarDTO(null, null, value, null, null))

/* Конвертеры */
fun LinkValueData.toDTOTag() = when (this) {
    is LinkValueData.Subject -> ValueDTOTags.SUBJECT
    is LinkValueData.Object -> ValueDTOTags.OBJECT
    is LinkValueData.DomainElement -> ValueDTOTags.DOMAIN_ELEMENT
}

fun ObjectValueData.toDTO(): ValueDTO = when (this) {
    is ObjectValueData.IntegerValue ->
        integerValueDto(value, precision)
    is ObjectValueData.StringValue ->
        stringValueDto(value)
    is ObjectValueData.CompoundValue ->
        compoundValueDto(value)
    is ObjectValueData.RangeValue ->
        rangeValueDto(range)
    is ObjectValueData.Link ->
        referenceValueDto(value.toDTOTag(), this.value.id)
    else ->
        throw IllegalStateException("no scalar value")
}

fun ValueDTO.scalarStrict(): ScalarDTO = scalar ?: throw IllegalStateException("scalar value data are absent")
fun ValueDTO.idStrict(): String = vertexId ?: throw IllegalStateException("id is absent")

fun ScalarDTO.intStrict(): Int = intValue ?: throw IllegalStateException("int value is absent")
fun ScalarDTO.stringStrict(): String = stringValue ?: throw IllegalStateException("str value is absent")
fun ScalarDTO.compoundStrict(): Any = data ?: throw IllegalStateException("data value is absent")
fun ScalarDTO.rangeStrict(): Range = range ?: throw IllegalStateException("range value is absent")

fun ValueDTO.toData(): ObjectValueData = when (ValueDTOTags.valueOf(tag)) {
    ValueDTOTags.INTEGER -> {
        val scalar = scalarStrict()
        ObjectValueData.IntegerValue(scalar.intStrict(), scalar.precision)
    }
    ValueDTOTags.STRING -> {
        val scalar = scalarStrict()
        ObjectValueData.StringValue(scalar.stringStrict())
    }
    ValueDTOTags.RANGE -> {
        val scalar = scalarStrict()
        ObjectValueData.RangeValue(scalar.rangeStrict())
    }
    ValueDTOTags.COMPOUND -> {
        val scalar = scalarStrict()
        ObjectValueData.CompoundValue(scalar.compoundStrict())
    }
    ValueDTOTags.OBJECT ->
        ObjectValueData.Link(LinkValueData.Object(idStrict()))
    ValueDTOTags.SUBJECT ->
        ObjectValueData.Link(LinkValueData.Subject(idStrict()))
    ValueDTOTags.DOMAIN_ELEMENT ->
        ObjectValueData.Link(LinkValueData.DomainElement(idStrict()))
}

data class ObjectPropertyValueDTO(
    val id: String?,
    val valueDto: ValueDTO,
    val objectPropertyId: String,
    val aspectPropertyId: String,
    val parentValueId: String?,
    val measureId: String?
)

fun ObjectPropertyValueData.toDTO() = ObjectPropertyValueDTO(
    id = this.id,
    valueDto = this.value.toDTO(),
    objectPropertyId = this.objectPropertyId,
    aspectPropertyId = this.aspectPropertyId,
    parentValueId = this.parentValueId,
    measureId = this.measureId
)

fun ObjectPropertyValueDTO.toData() = ObjectPropertyValueData(
    id = this.id,
    value = this.valueDto.toData(),
    objectPropertyId = this.objectPropertyId,
    aspectPropertyId = this.aspectPropertyId,
    parentValueId = this.parentValueId,
    measureId = this.measureId
)