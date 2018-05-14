package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

sealed class ReferenceValueVertex(val typeGroup: ReferenceTypeGroup) {
    class SubjectValue(val vertex: SubjectVertex) : ReferenceValueVertex(ReferenceTypeGroup.SUBJECT)
    class ObjectValue(val vertex: ObjectVertex) : ReferenceValueVertex(ReferenceTypeGroup.OBJECT)
    class DomainElementValue(val vertex: ReferenceBookItemVertex) :
        ReferenceValueVertex(ReferenceTypeGroup.DOMAIN_ELEMENT)

    fun toReferenceValueData(): ReferenceValueData {
        val id = when (this) {
            is SubjectValue -> vertex.id
            is ObjectValue -> vertex.id
            is DomainElementValue -> vertex.id
        }

        return ReferenceValueData(typeGroup, id)
    }
}


/**
 * Object property value data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectPropertyValue(
    val id: ORID?,
    val scalarValue: ScalarValue?,
    val range: Range?,
    val precision: Int?,
    val objectProperty: ObjectPropertyVertex,
    val rootCharacteristic: AspectVertex,
    val parentValue: ObjectPropertyValueVertex?,
    val referenceValue: ReferenceValueVertex?,
    val measure: OVertex?
) {
    fun toObjectPropertyValueData() = ObjectPropertyValueData(
        id?.toString(),
        scalarValue,
        range,
        precision,
        objectProperty.id,
        rootCharacteristic.id,
        parentValue?.id,
        referenceValue?.toReferenceValueData(),
        measure?.id
    )
}
