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

class ObjectPropertyVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "name" to asStringOrEmpty(name),
            "cardinality" to asStringOrEmpty(cardinality)
        ),
        links = emptyMap()
    )

    var name: String
        get() = vertex[ATTR_NAME]
        set(value) { vertex[ATTR_NAME] = value
        }

    var cardinality: PropertyCardinality
        get() = PropertyCardinality.valueOf(vertex["cardinality"])
        set(value) {
            vertex["cardinality"] = value.toString()
        }

    val objekt: ObjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_OBJECT_PROPERTY_EDGE).firstOrNull()?.toObjectVertex()

    val aspect: AspectVertex?
        get() = vertex.getVertices(ODirection.OUT, ASPECT_OBJECT_PROPERTY_EDGE).firstOrNull()?.toAspectVertex()

    val values: List<ObjectPropertyValueVertex>
        get() = vertex.getVertices(ODirection.IN, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).map { it.toObjectPropertyValueVertex() }

    fun toObjectProperty(): ObjectProperty {
        val currentObject = objekt
        val currentAspect = aspect

        if (currentObject == null) {
            throw ObjectPropertyWithoutObjectException(this)
        }

        if (currentAspect == null) {
            throw ObjectPropertyWithoutAspectException(this)
        }

        return ObjectProperty(identity, name, cardinality, currentObject, currentAspect, values)
    }
}

abstract class ObjectPropertyException(message: String) : Exception(message)
class ObjectPropertyWithoutObjectException(vertex: ObjectPropertyVertex) :
    ObjectPropertyException("Object vertex not linked for ${vertex.id} ")
class ObjectPropertyWithoutAspectException(vertex: ObjectPropertyVertex) :
    ObjectPropertyException("Aspect vertex not linked for ${vertex.id} ")