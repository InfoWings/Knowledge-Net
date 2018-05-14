package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ScalarValue
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import kotlinx.serialization.json.JSON

class ObjectDaoService(private val db: OrientDatabase) {
    fun newObjectVertex() = db.createNewVertex(OBJECT_CLASS).toObjectVertex()
    fun newObjectPropertyVertex() = db.createNewVertex(OBJECT_PROPERTY_CLASS).toObjectPropertyVertex()
    fun newObjectValueVertex() = db.createNewVertex(OBJECT_PROPERTY_VALUE_CLASS).toObjectPropertyValueVertex()

    private fun <T : OVertex> replaceEdge(vertex: OVertex, edgeClass: String, oldTarget: T?, newTarget: T?) {
        if (oldTarget != newTarget) {
            vertex.getEdges(ODirection.OUT, edgeClass).forEach { it.delete<OEdge>() }
            newTarget ?.let {
                val edge = vertex.addEdge(it, edgeClass)
                edge.save<OEdge>()
            }
        }
    }

    fun saveObject(vertex: ObjectVertex, objekt: Objekt): ObjectVertex = session(db) {
        vertex.name = objekt.name
        if (objekt.description != null) {
            vertex.description = objekt.description
        }
        if (vertex.subject != objekt.subject) {
            vertex.addEdge(objekt.subject, OBJECT_SUBJECT_EDGE)
        }

        var propertiesSet = objekt.properties.toSet()
        var toDelete = emptySet<ObjectPropertyVertex>()
        vertex.properties.forEach {
            if (propertiesSet.contains(it)) {
                propertiesSet -= it
            } else {
                toDelete += it
            }
        }

        toDelete.forEach { it.delete<ObjectPropertyVertex>()}
        propertiesSet.forEach { it.addEdge(vertex, OBJECT_OBJECT_PROPERTY_EDGE) }

        return@session vertex.save<OVertex>().toObjectVertex()
    }

    fun saveObjectProperty(vertex: ObjectPropertyVertex, objectProperty: ObjectProperty): ObjectPropertyVertex = session(db) {
        vertex.name = objectProperty.name
        vertex.cardinality = objectProperty.cardinality

        replaceEdge(vertex, OBJECT_OBJECT_PROPERTY_EDGE, vertex.objekt, objectProperty.objekt)
        replaceEdge(vertex, ASPECT_OBJECT_PROPERTY_EDGE, vertex.aspect, objectProperty.aspect)

        var valuesSet = objectProperty.values.toSet()
        var toDelete = emptySet<ObjectPropertyValueVertex>()
        vertex.values.forEach {
            if (valuesSet.contains(it)) {
                valuesSet -= it
            } else {
                toDelete += it
            }
        }

        toDelete.forEach { it.delete<ObjectPropertyValueVertex>()}
        valuesSet.forEach { it.addEdge(vertex, OBJECT_VALUE_OBJECT_PROPERTY_EDGE) }

        return@session vertex.save<OVertex>().toObjectPropertyVertex()
    }

    fun saveObjectValue(vertex: ObjectPropertyValueVertex, objectValue: ObjectPropertyValue): ObjectPropertyValueVertex = session(db) {
        vertex.intType ?.let {
            vertex.intValue = null
            vertex.intType = null
        }
        vertex.strType ?.let {
            vertex.strValue = null
            vertex.strType = null
        }
        vertex.compoundType ?.let {
            vertex.compoundValue = null
            vertex.compoundType = null
        }

        objectValue.scalarValue ?.let {
            when (it) {
                is ScalarValue.IntegerValue -> {
                    vertex.intType = it.typeName
                    vertex.intValue = it.value
                }
                is ScalarValue.StringValue -> {
                    vertex.strType = it.typeName
                    vertex.strValue = it.value
                }
                is ScalarValue.CompoundValue -> {
                    vertex.compoundType = it.typeName
                    vertex.compoundValue = JSON.stringify(it.value)
                }
            }
        }

        vertex.range = null
        vertex.range = objectValue.range
        vertex.precision = objectValue.precision

        replaceEdge(vertex, OBJECT_VALUE_OBJECT_PROPERTY_EDGE, vertex.objectProperty, objectValue.objectProperty)
        replaceEdge(vertex, OBJECT_VALUE_ASPECT_EDGE, vertex.rootCharacteristic, objectValue.rootCharacteristic)
        replaceEdge(vertex, OBJECT_VALUE_OBJECT_VALUE_EDGE, vertex.parentValue, objectValue.parentValue)

        replaceEdge(vertex, OBJECT_VALUE_MEASURE_EDGE, vertex.measure, objectValue.measure)

        return@session vertex.save<OVertex>().toObjectPropertyValueVertex()
    }

    fun getObjectVertex(id: String) = db.getVertexById(id)?.toObjectVertex()
    fun getObjectPropertyVertex(id: String) = db.getVertexById(id)?.toObjectPropertyVertex()
    fun getObjectPropertyValueVertex(id: String) = db.getVertexById(id)?.toObjectPropertyValueVertex()
}

abstract class ObjectException(message: String) : Exception(message)
class EmptyObjectNameException(data: ObjectData) : ObjectException("object name is empty: $data")
class EmptyObjectPropertyNameException(data: ObjectPropertyData) : ObjectException("object name is empty: $data")
class ObjectNotFoundException(id: String) : ObjectException("object not found. id: $id")
class ObjectPropertyNotFoundException(id: String) : ObjectException("object property not found. id: $id")
class ObjectPropertyValueNotFoundException(id: String) : ObjectException("object property value not found. id: $id")