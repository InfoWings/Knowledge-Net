package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.Range
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
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

    class Subject(override val vertex: SubjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Subject(vertex.id)
    }

    class Object(override val vertex: ObjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Object(vertex.id)
    }

    class ObjectProperty(override val vertex: ObjectPropertyVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.ObjectProperty(vertex.id)
    }

    class ObjectValue(override val vertex: ObjectPropertyValueVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.ObjectValue(vertex.id)
    }

    class DomainElement(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.DomainElement(vertex.id)
    }

    class Aspect(override val vertex: AspectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Aspect(vertex.id)
    }

    class AspectProperty(override val vertex: AspectPropertyVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.AspectProperty(vertex.id)
    }
}

/* Это бекендный аналог структуры ObjectValueData

   Часть подтипов совсем такие же, как в Object, но Decimal и Link отличаются
 */
sealed class ObjectValue {
    data class IntegerValue(val value: Int, val precision: Int?) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.IntegerValue(value, precision)
    }

    data class BooleanValue(val value: Boolean) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.BooleanValue(value)
    }

    data class StringValue(val value: String) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.StringValue(value)
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

    object NullValue : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.NullValue
    }

    abstract fun toObjectValueData(): ObjectValueData
}

/**
 * Object property value data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectPropertyValue(
    val id: ORID,
    val value: ObjectValue,
    val objectProperty: ObjectPropertyVertex,
    val aspectProperty: AspectPropertyVertex?,
    val parentValue: ObjectPropertyValueVertex?,
    val measure: OVertex?
)