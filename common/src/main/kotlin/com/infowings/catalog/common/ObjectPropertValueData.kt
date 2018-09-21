package com.infowings.catalog.common

/* Базовое представление диапазона */
data class Range(val left: Int, val right: Int)

/* Представление разных вариантов значений, пригодное для использования на фронте,
 * но не для сериализации в json
 */
sealed class ObjectValueData {
    abstract fun assignableTo(baseType: BaseType): Boolean
    abstract fun link(): Link?

    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = baseType.name == BaseType.Integer.name
        override fun link(): Link? = null
    }

    data class BooleanValue(val value: Boolean) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = baseType.name == BaseType.Boolean.name
        override fun link(): Link? = null
    }

    data class StringValue(val value: String) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = baseType.name == BaseType.Text.name
        override fun link(): Link? = null
    }

    data class RangeValue(val range: Range) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = baseType.name == BaseType.Range.name
        override fun link(): Link? = null
    }

    data class DecimalValue(val valueRepr: String) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = baseType.name == BaseType.Decimal.name
        override fun link(): Link? = null
    }

    data class Link(val value: LinkValueData) : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = when (value) {
            is LinkValueData.DomainElement -> baseType.name == BaseType.Text.name
            else -> baseType.name == BaseType.Reference.name
        }

        override fun link(): Link? = this
    }

    object NullValue : ObjectValueData() {
        override fun assignableTo(baseType: BaseType) = true
        override fun link(): Link? = null
    }
}


/* Представление ссылочного значения в виде, пригодном для использования на фронте
 * id - строковое предсталвение vertex id объекта/субъекта/значения домена
 * typeGroup - маркер с информацией о том, куда этот id показывает
 */
sealed class LinkValueData(open val id: String) {
    abstract fun isObjectValue(): Boolean

    data class Subject(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class Object(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class ObjectProperty(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class ObjectValue(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = true
    }

    data class DomainElement(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class RefBookItem(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class Aspect(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }

    data class AspectProperty(override val id: String) : LinkValueData(id) {
        override fun isObjectValue() = false
    }
}