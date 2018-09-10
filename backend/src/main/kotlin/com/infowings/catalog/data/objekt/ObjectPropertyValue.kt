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

    class RefBookItem(override val vertex: ReferenceBookItemVertex) : LinkValueVertex() {
        override fun toData() = LinkValueData.RefBookItem(vertex.id)
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
) {
    fun calculateObjectValueData(): ObjectValueData {
        val measure = measure?.toMeasure() ?: when (aspectProperty) {
            null -> objectProperty.aspect ?: throw IllegalStateException("Object property does not contain aspect")
            else -> aspectProperty.associatedAspect
        }.measure

        val targetValue = value.toObjectValueData()

        return if (targetValue is ObjectValueData.DecimalValue && measure != null) {
            ObjectValueData.DecimalValue(measure.fromBase(DecimalNumber(BigDecimal(targetValue.valueRepr))).toString())
        } else {
            targetValue
        }
    }
}

data class ValueResult(
    private val valueVertex: ObjectPropertyValueVertex,
    val valueDto: ValueDTO,
    val measureName: String?,
    private val objectProperty: ObjectPropertyVertex,
    private val aspectProperty: AspectPropertyVertex?,
    private val parentValue: ObjectPropertyValueVertex?
) {

    fun toResponse() = ValueChangeResponse(
        valueVertex.id,
        valueDto,
        valueVertex.description,
        measureName,
        Reference(objectProperty.id, objectProperty.version),
        aspectProperty?.id,
        parentValue?.id?.let { id -> parentValue.version.let { version -> Reference(id, version) } },
        valueVertex.version
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