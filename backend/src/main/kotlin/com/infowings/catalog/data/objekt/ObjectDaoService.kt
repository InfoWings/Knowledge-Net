package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

class ObjectDaoService(private val db: OrientDatabase) {
    fun newObjectVertex() = db.createNewVertex(OBJECT_CLASS).toObjectVertex()
    fun newObjectPropertyVertex() = db.createNewVertex(OBJECT_PROPERTY_CLASS).toObjectPropertyVertex()
    fun newObjectValueVertex() = db.createNewVertex(OBJECT_PROPERTY_VALUE_CLASS).toObjectPropertyValueVertex()

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

        if (vertex.objekt != objectProperty.objekt) {
            vertex.addEdge(objectProperty.objekt, OBJECT_OBJECT_PROPERTY_EDGE)
        }

        if (vertex.aspect != objectProperty.aspect) {
            vertex.addEdge(objectProperty.aspect, ASPECT_OBJECT_PROPERTY_EDGE)
        }

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
        val characteristics: List<CharacteristicVertex> = objectValue.characteristics

        vertex.value = objectValue.value
        vertex.range = objectValue.range
        vertex.precision = objectValue.precision

        if (vertex.objectProperty != objectValue.objectProperty) {
            vertex.addEdge(objectValue.objectProperty, OBJECT_VALUE_OBJECT_PROPERTY_EDGE)
        }

        var characteristicsSet: Set<CharacteristicVertex> = characteristics.toSet()
        var toDelete = emptySet<CharacteristicVertex>()
        vertex.characteristics.forEach {
            val currentCharacteristic = it
            if (characteristicsSet.contains(currentCharacteristic)) {
                characteristicsSet -= currentCharacteristic
            } else {
                toDelete += currentCharacteristic
            }
        }

        toDelete.forEach { it.delete<ObjectPropertyValueVertex>()}
        characteristicsSet.forEach {
            vertex.addEdge(it, OBJECT_VALUE_CHARACTERISTIC_EDGE)
        }

        return@session vertex.save<OVertex>().toObjectPropertyValueVertex()
    }

    fun getObjectVertex(id: String) = db.getVertexById(id)?.toObjectVertex()
    fun getObjectPropertyVertex(id: String) = db.getVertexById(id)?.toObjectPropertyVertex()
}

abstract class ObjectException(message: String) : Exception(message)
class EmptyObjectNameException(data: ObjectData) : ObjectException("object name is empty: $data")
class EmptyObjectPropertyNameException(data: ObjectPropertyData) : ObjectException("object name is empty: $data")
class ObjectNotFoundException(id: String) : ObjectException("object not found. id: $id")
class ObjectPropertyNotFoundException(id: String) : ObjectException("object property not found. id: $id")