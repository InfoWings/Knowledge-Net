package com.infowings.catalog.common

data class Range(val left: Int, val right: Int)

sealed class ScalarValue {
    class IntegerValue(val value: Int) : ScalarValue()
    class StringValue(val value: String) : ScalarValue()
    class CompoundValue(val value: Any) : ScalarValue()
}

sealed class ObjectValueData {
    data class Scalar(val value: ScalarValue?, val range: Range?, val precision: Int?) : ObjectValueData()
    data class Link(val value: LinkValueData) : ObjectValueData()
}


enum class LinkTypeGroup {
    SUBJECT, OBJECT, DOMAIN_ELEMENT
}

data class LinkValueData(val typeGroup: LinkTypeGroup, val id: String)

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

  К каким типам будут в итоге относиться диапазон/точность - пока неясно, поэтому держим их сверху, на одном уровне
  со значениями
 */
enum class ValueDTOTags {
    INTEGER,
    STRING,
    COMPOUND,
    SUBJECT,
    OBJECT,
    DOMAIN_ELEMENT,
}

data class ScalarDTO(
    val stringValue: String?,
    val intValue: Int?,
    val data: Any?,
    val range: Range?,
    val precision: Int?
)

data class ValueDTO(val tag: String, val id: String?, val scalar: ScalarDTO?)

fun stringValueDto(value: String) = ValueDTO(ValueDTOTags.STRING.name, null, ScalarDTO(value, null, null, null, null))
fun integerValueDto(value: Int, range: Range?, precision: Int?) =
    ValueDTO(ValueDTOTags.INTEGER.name, null, ScalarDTO(null, value, null, range, precision))
fun compoundValueDto(value: Any) = ValueDTO(ValueDTOTags.COMPOUND.name, null, ScalarDTO(null, null, value, null, null))
fun referenceValueDto(tag: ValueDTOTags, id: String) = ValueDTO(tag.name, id, null)

fun LinkTypeGroup.toDTOTag() = when (this) {
    LinkTypeGroup.SUBJECT -> ValueDTOTags.SUBJECT
    LinkTypeGroup.OBJECT -> ValueDTOTags.OBJECT
    LinkTypeGroup.DOMAIN_ELEMENT -> ValueDTOTags.DOMAIN_ELEMENT
}

fun ObjectValueData.toDTO(): ValueDTO = when (this) {
    is ObjectValueData.Scalar -> {
        val scalar = this.value
        when (scalar) {
            is ScalarValue.IntegerValue ->
                integerValueDto(scalar.value, range, precision)
            is ScalarValue.StringValue ->
                stringValueDto(scalar.value)
            is ScalarValue.CompoundValue ->
                compoundValueDto(scalar.value)
            else ->
                throw IllegalStateException("no scalar value")
        }
    }
    is ObjectValueData.Link ->
        referenceValueDto(this.value.typeGroup.toDTOTag(), this.value.id)
}

fun ValueDTO.scalarStrict(): ScalarDTO = scalar ?: throw IllegalStateException("scalar value data are absent")
fun ValueDTO.idStrict(): String = id ?: throw IllegalStateException("id is absent")

fun ScalarDTO.intStrict(): Int = intValue ?: throw IllegalStateException("int value is absent")
fun ScalarDTO.stringStrict(): String = stringValue ?: throw IllegalStateException("str value is absent")
fun ScalarDTO.compoundStrict(): Any = data ?: throw IllegalStateException("data value is absent")

fun ValueDTO.toData(): ObjectValueData = when (ValueDTOTags.valueOf(tag)) {
    ValueDTOTags.INTEGER -> {
        val scalar = scalarStrict()
        ObjectValueData.Scalar(ScalarValue.IntegerValue(scalar.intStrict()), scalar.range, scalar.precision)
    }
    ValueDTOTags.STRING -> {
        val scalar = scalarStrict()
        ObjectValueData.Scalar(ScalarValue.StringValue(scalar.stringStrict()), null, null)
    }
    ValueDTOTags.COMPOUND -> {
        val scalar = scalarStrict()
        ObjectValueData.Scalar(
            ScalarValue.CompoundValue(scalar.compoundStrict()), null, null)
    }
    ValueDTOTags.OBJECT ->
        ObjectValueData.Link(LinkValueData(LinkTypeGroup.OBJECT, idStrict()))
    ValueDTOTags.SUBJECT ->
        ObjectValueData.Link(LinkValueData(LinkTypeGroup.SUBJECT, idStrict()))
    ValueDTOTags.DOMAIN_ELEMENT ->
        ObjectValueData.Link(LinkValueData(LinkTypeGroup.DOMAIN_ELEMENT, idStrict()))
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