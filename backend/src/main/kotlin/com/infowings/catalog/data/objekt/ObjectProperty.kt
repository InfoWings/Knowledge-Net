package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID

/* Про ссылки на vertex-классы - см. комментарий в ObkectPropertyValue.kt */

/**
 * Object property data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectProperty(
    val id: ORID?,
    val name: String,
    val cardinality: PropertyCardinality,
    val objekt: ObjectVertex,
    val aspect: AspectVertex,
    val values: List<ObjectPropertyValueVertex>
) {
    fun toObjectPropertyData(): ObjectPropertyData {
        return ObjectPropertyData(id?.toString(), name, cardinality, objekt.id, aspect.id, values.map { it.id })
    }
}