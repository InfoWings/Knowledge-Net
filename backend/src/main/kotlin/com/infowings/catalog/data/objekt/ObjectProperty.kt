package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

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
)

data class PropertyWriteInfo(
    val name: String,
    val cardinality: PropertyCardinality,
    val objekt: ObjectVertex,
    val aspect: AspectVertex
)

data class ValueWriteInfo(
    val value: ObjectValue,
    val objectProperty: ObjectPropertyVertex,
    val aspectProperty: AspectPropertyVertex?,
    val parentValue: ObjectPropertyValueVertex?,
    val measure: OVertex?
)
