package com.infowings.catalog.common

import kotlinx.serialization.Serializable

/*
 DTO-классы реализуют своего рода туннель для передачи Data-классов от фронтенда к бекенду и обратно.
 Другая функция DTO-классов - json-представление данных в REST API для сторонних клиентов.
 Бизнес-логика с этими классами не работает. После приема DTO конвертируется в Data-классы, перед отправкой
 Data-классы конвертируются в DTO.

 Если мы решили поменять формат представления данных в REST API, мы меняем DTO структуры и конвертеры
 в/из Data-структуры, вое остальное остается неизменным.

 Если хотим поменять что-то в представлении данных внутри приложения, не меняя внешних интерфейсов, сопровождаем
 изменения представления данных изменением конвертеров.

 Если хотим для передачи использовать какой-то другой формат (protobuf, например), DTO-классы меняем на какие-то
 другие аналогичные структуры и делаем свою версию конвертеров в Data и из Data

 Если хотим в разных случаях использовать разные представления для передачи данных (например, выдавать protobuf или
 json в зависимости от флага, указанного в REST-запросе), реализуем соответствующие структуры и конвертеры в Data
 и из Data и используем подходящий вариант в нужной ситуации
*/

/*
  Теги для компактного JSON-представления
  Скалярные значения будем представлять на одном уровне со ссылочными

  Верояность конфликта тегов (то есть вероятность того, что захочется иметь скалярный тип с именем
   SUBJECT/OBJECT/DOMAIN_ELEMENT) невысока.
  Ею модно пренебречь ради того, чтобы не создавать в json-структуре дополнительный уровень вложенности для ссылочных
  типов

 */
enum class ValueDTOTags {
    NULL,
    INTEGER,
    BOOLEAN,
    STRING,
    DECIMAL,
    RANGE,
    SUBJECT,
    OBJECT,
    OBJECT_PROPERTY,
    OBJECT_VALUE,
    ASPECT,
    ASPECT_PROPERTY,
    DOMAIN_ELEMENT,
    REF_BOOK_ITEM
}

public enum class RangeFlagConstants(val bitmask: Int) {
    LEFT_INF(1),
    RIGHT_INF(2)
}

/* json-представление скалярного значения. */
@Serializable
data class ValueDTO(
    val tag: String,
    val stringValue: String? = null,
    val intValue: Int? = null,
    val intUpb: Int? = null,
    val decUpbRepr: String? = null,
    val range: Range? = null,
    val rangeFlags: Int? = null,
    val precision: Int? = null,
    val vertexId: String? = null,
    val booleanValue: Boolean? = null
) {
    companion object {
        private fun link(tag: String, id: String) = ValueDTO(tag, vertexId = id)

        fun string(value: String) = ValueDTO(ValueDTOTags.STRING.name, stringValue = value)
        fun nullValue() = ValueDTO(ValueDTOTags.NULL.name)
        fun boolean(value: Boolean) = ValueDTO(ValueDTOTags.BOOLEAN.name, booleanValue = value)
        fun decimalRange(valueRepr: String, upbRepr: String, rangeFlags: Int?) = ValueDTO(ValueDTOTags.DECIMAL.name, stringValue = valueRepr,
            decUpbRepr = upbRepr, rangeFlags = rangeFlags)
        fun integerRange(lwb: Int, upb: Int, precision: Int?) = ValueDTO(ValueDTOTags.INTEGER.name, intValue = lwb, intUpb = upb, precision = precision)
        fun subject(id: String) = link(ValueDTOTags.SUBJECT.name, id)
        fun objekt(id: String) = link(ValueDTOTags.OBJECT.name, id)
        fun objectProperty(id: String) = link(ValueDTOTags.OBJECT_PROPERTY.name, id)
        fun objectValue(id: String) = link(ValueDTOTags.OBJECT_VALUE.name, id)
        fun domainElement(id: String) = link(ValueDTOTags.DOMAIN_ELEMENT.name, id)
        fun refBookItem(id: String) = link(ValueDTOTags.REF_BOOK_ITEM.name, id)
        fun aspect(id: String) = link(ValueDTOTags.ASPECT.name, id)
        fun aspectProperty(id: String) = link(ValueDTOTags.ASPECT_PROPERTY.name, id)
        fun range(value: Range) = ValueDTO(ValueDTOTags.RANGE.name, range = value)
    }
}

/* Конвертеры */
fun ObjectValueData.toDTO(): ValueDTO = when (this) {
    is ObjectValueData.IntegerValue -> ValueDTO.integerRange(value, upb, precision)
    is ObjectValueData.DecimalValue -> ValueDTO.decimalRange(valueRepr, upbRepr, rangeFlags)
    is ObjectValueData.BooleanValue -> ValueDTO.boolean(value)
    is ObjectValueData.StringValue -> ValueDTO.string(value)
    is ObjectValueData.RangeValue -> ValueDTO.range(range)
    is ObjectValueData.Link ->
        when (this.value) {
            is LinkValueData.Subject -> ValueDTO.subject(this.value.id)
            is LinkValueData.Object -> ValueDTO.objekt(this.value.id)
            is LinkValueData.ObjectProperty -> ValueDTO.objectProperty(this.value.id)
            is LinkValueData.ObjectValue -> ValueDTO.objectValue(this.value.id)
            is LinkValueData.DomainElement -> ValueDTO.domainElement(this.value.id)
            is LinkValueData.RefBookItem -> ValueDTO.refBookItem(this.value.id)
            is LinkValueData.Aspect -> ValueDTO.aspect(this.value.id)
            is LinkValueData.AspectProperty -> ValueDTO.aspectProperty(this.value.id)
        }
    is ObjectValueData.NullValue -> ValueDTO.nullValue()
}

fun ValueDTO.idStrict(): String = vertexId ?: throw IllegalStateException("id is absent")
fun ValueDTO.intStrict(): Int = intValue ?: throw IllegalStateException("int value is absent")
fun ValueDTO.stringStrict(): String = stringValue ?: throw IllegalStateException("str value is absent")
fun ValueDTO.rangeStrict(): Range = range ?: throw IllegalStateException("range value is absent")
fun ValueDTO.booleanStrict(): Boolean = booleanValue ?: throw IllegalStateException("boolean value is absent")
fun ValueDTO.decimalStrict(): String = stringValue ?: throw IllegalStateException("decimal value representation is absent")

fun ValueDTO.toData(): ObjectValueData = when (ValueDTOTags.valueOf(tag)) {
    ValueDTOTags.INTEGER -> ObjectValueData.IntegerValue(intStrict(), intUpb ?: intStrict(), precision)
    ValueDTOTags.STRING -> ObjectValueData.StringValue(stringStrict())
    ValueDTOTags.BOOLEAN -> ObjectValueData.BooleanValue(booleanStrict())
    ValueDTOTags.RANGE -> ObjectValueData.RangeValue(rangeStrict())
    ValueDTOTags.DECIMAL -> ObjectValueData.DecimalValue(decimalStrict(), decUpbRepr ?: decimalStrict(), rangeFlags?: 0)
    ValueDTOTags.OBJECT -> ObjectValueData.Link(LinkValueData.Object(idStrict()))
    ValueDTOTags.OBJECT_PROPERTY -> ObjectValueData.Link(LinkValueData.ObjectProperty(idStrict()))
    ValueDTOTags.OBJECT_VALUE -> ObjectValueData.Link(LinkValueData.ObjectValue(idStrict()))
    ValueDTOTags.SUBJECT -> ObjectValueData.Link(LinkValueData.Subject(idStrict()))
    ValueDTOTags.DOMAIN_ELEMENT -> ObjectValueData.Link(LinkValueData.DomainElement(idStrict()))
    ValueDTOTags.REF_BOOK_ITEM -> ObjectValueData.Link(LinkValueData.RefBookItem(idStrict()))
    ValueDTOTags.ASPECT -> ObjectValueData.Link(LinkValueData.Aspect(idStrict()))
    ValueDTOTags.ASPECT_PROPERTY -> ObjectValueData.Link(LinkValueData.AspectProperty(idStrict()))
    ValueDTOTags.NULL -> ObjectValueData.NullValue
}
