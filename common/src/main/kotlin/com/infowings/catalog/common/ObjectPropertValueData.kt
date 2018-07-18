package com.infowings.catalog.common

/* Базовое представление диапазона */
data class Range(val left: Int, val right: Int)

/* Представление разных вариантов значений, пригодное для использования на фронте,
 * но не для сериализации в json
 */
sealed class ObjectValueData {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValueData()
    data class BooleanValue(val value: Boolean) : ObjectValueData()
    data class StringValue(val value: String) : ObjectValueData()
    data class RangeValue(val range: Range) : ObjectValueData()
    data class DecimalValue(val valueRepr: String) : ObjectValueData()
    data class Link(val value: LinkValueData) : ObjectValueData()
    object NullValue : ObjectValueData()
}


/* Представление ссылочного значения в виде, пригодном для использования на фронте
 * id - строковое предсталвение vertex id объекта/субъекта/значения домена
 * typeGroup - маркер с информацией о том, куда этот id показывает
 */
sealed class LinkValueData(open val id: String) {
    class Subject(override val id: String) : LinkValueData(id)
    class Object(override val id: String) : LinkValueData(id)
    class ObjectProperty(override val id: String) : LinkValueData(id)
    class ObjectValue(override val id: String) : LinkValueData(id)
    class DomainElement(override val id: String) : LinkValueData(id)
    class Aspect(override val id: String) : LinkValueData(id)
    class AspectProperty(override val id: String) : LinkValueData(id)
}