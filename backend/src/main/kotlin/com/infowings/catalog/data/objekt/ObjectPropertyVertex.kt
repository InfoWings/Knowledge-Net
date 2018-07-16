package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toObjectPropertyVertex() = ObjectPropertyVertex(this)

class ObjectPropertyVertex(private val vertex: OVertex) : HistoryAware, DeletableVertex, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "name" to asStringOrEmpty(name),
            "cardinality" to asStringOrEmpty(cardinality)
        ),
        links = mapOf(
            "aspect" to listOfNotNull(aspect?.identity),
            "object" to listOfNotNull(objekt?.identity),
            "values" to values.map { it.identity }
        )
    )

    var name: String?
        get() = vertex[ATTR_NAME]
        set(value) {
            vertex[ATTR_NAME] = value
        }

    val cardinality: PropertyCardinality
        get() {
            val rootsCount = values.filter { it.parentValue == null }.size
            return when (rootsCount) {
                0 -> PropertyCardinality.ZERO
                1 -> PropertyCardinality.ONE
                else -> PropertyCardinality.INFINITY
            }
        }

    val objekt: ObjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_OBJECT_PROPERTY_EDGE).firstOrNull()?.toObjectVertex()

    val aspect: AspectVertex?
        get() = vertex.getVertices(ODirection.OUT, ASPECT_OBJECT_PROPERTY_EDGE).firstOrNull()?.toAspectVertex()

    val values: List<ObjectPropertyValueVertex>
        get() = vertex.getVertices(
            ODirection.IN,
            OBJECT_VALUE_OBJECT_PROPERTY_EDGE
        ).map { it.toObjectPropertyValueVertex() }.filterNot { it.deleted }

}

sealed class ObjectPropertyException(message: String) : Exception(message)

class ObjectPropertyIsLinkedException(valueIds: List<String>, propertyId: String?) :
    ObjectPropertyException("linked values: $valueIds, linked property: $propertyId")
