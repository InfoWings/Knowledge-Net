package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.objekt.PropertyCreateResponse
import com.infowings.catalog.common.objekt.PropertyDeleteResponse
import com.infowings.catalog.common.objekt.PropertyUpdateResponse
import com.infowings.catalog.common.objekt.Reference
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.description
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OEdge
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

data class PropertyCreateResult(
    private val propertyVertex: ObjectPropertyVertex,
    private val objectVertex: ObjectVertex,
    private val rootValueVertex: ObjectPropertyValueVertex
) {

    fun toResponse() = PropertyCreateResponse(
        propertyVertex.id,
        Reference(objectVertex.id, objectVertex.version),
        Reference(rootValueVertex.id, rootValueVertex.version),
        propertyVertex.name,
        propertyVertex.description,
        propertyVertex.version
    )
}

data class PropertyUpdateResult(private val propertyVertex: ObjectPropertyVertex, private val objectVertex: ObjectVertex) {

    fun toResponse() = PropertyUpdateResponse(
        propertyVertex.id,
        Reference(objectVertex.id, objectVertex.version),
        propertyVertex.name,
        propertyVertex.description,
        propertyVertex.version
    )
}

data class PropertyDeleteResult(private val propertyVertex: ObjectPropertyVertex, private val objectVertex: ObjectVertex) {

    fun toResponse() = PropertyDeleteResponse(
        propertyVertex.id,
        Reference(objectVertex.id, objectVertex.version),
        propertyVertex.name,
        propertyVertex.description,
        propertyVertex.version
    )
}

data class PropertyWriteInfo(
    val name: String?,
    val description: String?,
    val objekt: ObjectVertex,
    val aspect: AspectVertex
)

data class ValueWriteInfo(
    val value: ObjectValue,
    val description: String?,
    val objectProperty: ObjectPropertyVertex,
    val aspectProperty: AspectPropertyVertex?,
    val parentValue: ObjectPropertyValueVertex?,
    val measure: OVertex?
)

data class DeleteInfo(
        val vertex: OVertex,
        val incoming: List<OEdge>,
        val outgoing: List<OEdge>)
