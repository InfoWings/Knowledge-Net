package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

sealed class LinkValueVertex(private val typeGroup: LinkTypeGroup) {
    abstract val vertex: OVertex

    class SubjectValue(override val vertex: SubjectVertex) : LinkValueVertex(LinkTypeGroup.SUBJECT)
    class ObjectValue(override val vertex: ObjectVertex) : LinkValueVertex(LinkTypeGroup.OBJECT)
    class DomainElementValue(override val vertex: ReferenceBookItemVertex) :
        LinkValueVertex(LinkTypeGroup.DOMAIN_ELEMENT)

    fun toLinkValueData(): LinkValueData {
        return LinkValueData(typeGroup, vertex.id)
    }
}

sealed class ObjectValue {
    data class Scalar(val value: ScalarValue?, val range: Range?, val precision: Int?) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.Scalar(value, range, precision)
    }

    data class Link(val value: LinkValueVertex) : ObjectValue() {
        override fun toObjectValueData() = ObjectValueData.Link(value.toLinkValueData())
    }

    abstract fun toObjectValueData(): ObjectValueData
}

fun fromScalarData(data: ObjectValueData.Scalar): ObjectValue.Scalar =
    ObjectValue.Scalar(data.value, data.range, data.precision)


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
