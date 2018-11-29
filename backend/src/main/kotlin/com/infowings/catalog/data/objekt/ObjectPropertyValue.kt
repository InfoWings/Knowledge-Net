package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.Reference
import com.infowings.catalog.common.objekt.ValueChangeResponse
import com.infowings.catalog.common.objekt.ValueDeleteResponse
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.toMeasure
import com.infowings.catalog.storage.description
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
        override fun toData() = LinkValueData.Subject(vertex.id, vertex.guidSoft())
    }

    class Object(override val vertex: ObjectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Object(vertex.id, vertex.guidSoft())
    }

    class ObjectProperty(override val vertex: ObjectPropertyVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.ObjectProperty(vertex.id, vertex.guidSoft())
    }

    class ObjectValue(override val vertex: ObjectPropertyValueVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.ObjectValue(vertex.id, vertex.guidSoft())
    }

    class DomainElement(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData(): LinkValueData {
            return LinkValueData.DomainElement(vertex.id, vertex.guidSoft(), vertex.value, vertex.root?.id)
        }
    }

    class RefBookItem(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.RefBookItem(vertex.id, vertex.guidSoft())
    }

    class Aspect(override val vertex: AspectVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.Aspect(vertex.id, vertex.guidSoft())
    }

    class AspectProperty(override val vertex: AspectPropertyVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.AspectProperty(vertex.id, vertex.guidSoft())
    }
}

/* Это бекендный аналог структуры ObjectValueData

   Часть подтипов совсем такие же, как в Object, но Decimal и Link отличаются
 */
sealed class ObjectValue {
    data class IntegerValue(val value: Int, val upb: Int, val precision: Int?) : ObjectValue() {
        constructor(value: Int, precision: Int?) : this(value, value, precision)

        constructor(value: Int, upb: Int?, precision: Int?) : this(value, upb ?: value, precision)

        override fun toObjectValueData() = ObjectValueData.IntegerValue(value, upb, precision)
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

    data class DecimalValue(val value: BigDecimal, val upb: BigDecimal, val rangeFlags: Int) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.DecimalValue(value.toString(), upb.toString(), rangeFlags)

        companion object {
            fun instance(value: BigDecimal, upb: BigDecimal?, rangeFlags: Int) = DecimalValue(value, upb ?: value, rangeFlags)
        }
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
    val measure: OVertex?,
    val guid: String?
) {
    fun calculateObjectValueData(): ObjectValueData {

        val measure = measure?.toMeasure() ?: when (aspectProperty) {
            null -> objectProperty.aspect ?: throw IllegalStateException("Object property does not contain aspect")
            else -> aspectProperty.associatedAspect
        }.measure

        val targetValue: ObjectValueData = value.toObjectValueData()

        return if (targetValue is ObjectValueData.DecimalValue && measure != null) {
            ObjectValueData.DecimalValue(
                measure.fromBase(DecimalNumber(BigDecimal(targetValue.valueRepr))).toString(),
                measure.fromBase(DecimalNumber(BigDecimal(targetValue.upbRepr))).toString(),
                targetValue.rangeFlags
            )
        } else {
            targetValue
        }
    }
}

data class ValueResult(
    private val valueVertex: ObjectPropertyValueVertex,
    private val valueDto: ValueDTO,
    val measureName: String?,
    private val objectProperty: ObjectPropertyVertex,
    private val aspectProperty: AspectPropertyVertex?,
    private val parentValue: ObjectPropertyValueVertex?
) {

    private val guidValue = valueVertex.guid

    fun toResponse() = ValueChangeResponse(
        valueVertex.id,
        valueDto,
        valueVertex.description,
        measureName,
        Reference(objectProperty.id, objectProperty.version),
        aspectProperty?.id,
        parentValue?.id?.let { id -> parentValue.version.let { version -> Reference(id, version) } },
        valueVertex.version,
        guidValue
    )
}

data class ValueDeleteResult(
    private val deletedValues: List<ObjectPropertyValueVertex>,
    private val markedValues: List<ObjectPropertyValueVertex>,
    private val property: ObjectPropertyVertex,
    private val parentValue: ObjectPropertyValueVertex?
) {

    fun toResponse() = ValueDeleteResponse(
        deletedValues.map { it.id },
        markedValues.map { it.id },
        Reference(property.id, property.version),
        parentValue?.id?.let { parentId ->
            parentValue.version.let { parentVersion ->
                Reference(
                    parentId,
                    parentVersion
                )
            }
        }
    )
}