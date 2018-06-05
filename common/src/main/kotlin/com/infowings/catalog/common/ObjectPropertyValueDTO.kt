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
    INTEGER,
    STRING,
    RANGE,
    SUBJECT,
    OBJECT,
    DOMAIN_ELEMENT,
}

/* json-представление скалярного значения. */
@Serializable
data class ValueDTO(
    val tag: String,
    val stringValue: String?,
    val intValue: Int?,
    val range: Range?,
    val precision: Int?,
    val vertexId: String?
)

/* заполнители */
fun stringValueDto(value: String) = ValueDTO(ValueDTOTags.STRING.name, value, null, null, null, null)

fun integerValueDto(value: Int, precision: Int?) =
    ValueDTO(ValueDTOTags.INTEGER.name, null, value, null, precision, null)

fun rangeValueDto(value: Range) = ValueDTO(ValueDTOTags.RANGE.name, null, null, value, null, null)
fun subjectValueDto(id: String) = ValueDTO(ValueDTOTags.SUBJECT.name, null, null, null, null, id)
fun objectValueDto(id: String) = ValueDTO(ValueDTOTags.OBJECT.name, null, null, null, null, id)
fun domainElementValueDto(id: String) = ValueDTO(ValueDTOTags.DOMAIN_ELEMENT.name, null, null, null, null, id)

/* Конвертеры */
fun ObjectValueData.toDTO(): ValueDTO = when (this) {
    is ObjectValueData.IntegerValue ->
        integerValueDto(value, precision)
    is ObjectValueData.StringValue ->
        stringValueDto(value)
    is ObjectValueData.RangeValue ->
        rangeValueDto(range)
    is ObjectValueData.Link ->
        when (this.value) {
            is LinkValueData.Subject -> subjectValueDto(this.value.id)
            is LinkValueData.Object -> objectValueDto(this.value.id)
            is LinkValueData.DomainElement -> domainElementValueDto(this.value.id)
        }
    else ->
        throw IllegalStateException("no scalar value")
}

fun ValueDTO.idStrict(): String = vertexId ?: throw IllegalStateException("id is absent")
fun ValueDTO.intStrict(): Int = intValue ?: throw IllegalStateException("int value is absent")
fun ValueDTO.stringStrict(): String = stringValue ?: throw IllegalStateException("str value is absent")
fun ValueDTO.rangeStrict(): Range = range ?: throw IllegalStateException("range value is absent")

fun ValueDTO.toData(): ObjectValueData = when (ValueDTOTags.valueOf(tag)) {
    ValueDTOTags.INTEGER -> {
        ObjectValueData.IntegerValue(intStrict(), precision)
    }
    ValueDTOTags.STRING -> {
        ObjectValueData.StringValue(stringStrict())
    }
    ValueDTOTags.RANGE -> {
        ObjectValueData.RangeValue(rangeStrict())
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
