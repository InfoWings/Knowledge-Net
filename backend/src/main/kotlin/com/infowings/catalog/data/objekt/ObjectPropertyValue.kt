package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import java.math.BigDecimal

/* Бекенд-структура для представления ссылочного значения.
 * Здесь id недостаточно, лучше иметь дело с vertex
 */
sealed class LinkValueVertex {
    abstract val vertex: OVertex
    abstract fun toData(): LinkValueData

    class SubjectValue(override val vertex: SubjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Subject(vertex.id)
    }

    class ObjectValue(override val vertex: ObjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Object(vertex.id)
    }

    class DomainElementValue(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.DomainElement(vertex.id)
    }
}

/* Это бекендный аналог структуры ObjectValueData

   Часть подтипов совсем такие же, как в ObjectValue, но Decimal и Link отличаются
 */
sealed class ObjectValue {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.IntegerValue(value, precision)
    }

    data class StringValue(val value: String) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.StringValue(value)
    }

    data class CompoundValue(val value: Any) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.CompoundValue(value)
    }

    data class RangeValue(val range: Range) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.RangeValue(range)
    }

    data class DecimalValue(val value: BigDecimal) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.DecimalValue(value.toString())
    }

    data class Link(val value: LinkValueVertex) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.Link(value.toData())
    }

    abstract fun toObjectValueData(): ObjectValueData
}

/**
 * Object property value data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectPropertyValue(
    val id: ORID?,
    val value: ObjectValue,
    val objectProperty: ObjectPropertyVertex,
    val aspectProperty: AspectPropertyVertex,
    val parentValue: ObjectPropertyValueVertex?,
    val measure: OVertex?
) {
    fun toObjectPropertyValueData() = ObjectPropertyValueData(
        id?.toString(),
        value.toObjectValueData(),
        objectProperty.id,
        aspectProperty.id,
        parentValue?.id,
        measure?.id
    )
}
