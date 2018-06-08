package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import java.math.BigDecimal

class ObjectDaoService(private val db: OrientDatabase) {
    fun newObjectVertex() = db.createNewVertex(OBJECT_CLASS).toObjectVertex()
    fun newObjectPropertyVertex() = db.createNewVertex(OBJECT_PROPERTY_CLASS).toObjectPropertyVertex()
    fun newObjectValueVertex() = db.createNewVertex(OBJECT_PROPERTY_VALUE_CLASS).toObjectPropertyValueVertex()

    private fun <T : OVertex> replaceEdge(vertex: OVertex, edgeClass: String, oldTarget: T?, newTarget: T?) {
        if (oldTarget?.identity != newTarget?.identity) {
            vertex.getEdges(ODirection.OUT, edgeClass).forEach { it.delete<OEdge>() }
            newTarget?.let {
                vertex.addEdge(it, edgeClass).save<OEdge>()
            }
        }
    }

    fun saveObject(vertex: ObjectVertex, info: ObjectCreateInfo, properties: List<ObjectPropertyVertex>): ObjectVertex =
        transaction(db) {
            vertex.name = info.name
            vertex.description = info.description

            if (vertex.subject != info.subject) {
                vertex.addEdge(info.subject, OBJECT_SUBJECT_EDGE).save<OEdge>()
            }

            val newProperties: Set<ObjectPropertyVertex> = properties.toSet()
            val currentProperties: Set<ObjectPropertyVertex> = vertex.properties.toSet()
            val toDelete: Set<ObjectPropertyVertex> = currentProperties.minus(newProperties)
            val toAdd = newProperties.minus(currentProperties)

            toDelete.forEach { it.delete<ObjectPropertyVertex>() }
            toAdd.forEach {
                it.addEdge(vertex, OBJECT_OBJECT_PROPERTY_EDGE).save<OEdge>()
            }

            return@transaction vertex.save<OVertex>().toObjectVertex()
        }

    fun saveObjectProperty(
        vertex: ObjectPropertyVertex,
        info: PropertyWriteInfo,
        values: List<ObjectPropertyValueVertex>
    ): ObjectPropertyVertex =
        transaction(db) {
            vertex.name = info.name
            vertex.cardinality = info.cardinality

            replaceEdge(vertex, OBJECT_OBJECT_PROPERTY_EDGE, vertex.objekt, info.objekt)
            replaceEdge(vertex, ASPECT_OBJECT_PROPERTY_EDGE, vertex.aspect, info.aspect)

            var valuesSet = values.toSet()
            var toDelete = emptySet<ObjectPropertyValueVertex>()
            vertex.values.forEach {
                if (valuesSet.contains(it)) {
                    valuesSet -= it
                } else {
                    toDelete += it
                }
            }

            toDelete.forEach { it.delete<ObjectPropertyValueVertex>() }
            valuesSet.forEach {
                it.addEdge(vertex, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).save<OEdge>()
            }

            return@transaction vertex.save<OVertex>().toObjectPropertyVertex()
        }

    fun saveObjectValue(
        vertex: ObjectPropertyValueVertex,
        valueInfo: ValueWriteInfo
    ): ObjectPropertyValueVertex = transaction(db) {
        val newTypeTag = valueInfo.value.tag()

        if (vertex.typeTag != newTypeTag) {
            when (vertex.typeTag) {
                ScalarTypeTag.INTEGER -> {
                    vertex.removeProperty<Int>(INT_TYPE_PROPERTY)
                    vertex.removeProperty<Int>(PRECISION_PROPERTY)
                }
                ScalarTypeTag.DECIMAL -> vertex.removeProperty<BigDecimal>(DECIMAL_TYPE_PROPERTY)
                ScalarTypeTag.STRING -> vertex.removeProperty<String>(STR_TYPE_PROPERTY)
                ScalarTypeTag.RANGE -> vertex.removeProperty<Range>(RANGE_TYPE_PROPERTY)
                ScalarTypeTag.BOOLEAN -> vertex.removeProperty<Boolean>(BOOL_TYPE_PROPERTY)
                ScalarTypeTag.SUBJECT -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_SUBJECT_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.OBJECT -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_OBJECT_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.DOMAIN_ELEMENT -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_REFBOOK_ITEM_EDGE).forEach { it.delete<OEdge>() }
                }
            }

            vertex.typeTag = newTypeTag
        }

        val objectValue = valueInfo.value
        when (objectValue) {
            is ObjectValue.IntegerValue -> {
                vertex.intValue = objectValue.value
                vertex.precision = objectValue.precision
            }
            is ObjectValue.DecimalValue -> {
                vertex.decimalValue = objectValue.value
            }
            is ObjectValue.StringValue -> {
                vertex.strValue = objectValue.value
            }
            is ObjectValue.RangeValue -> {
                vertex.range = objectValue.range
            }
            is ObjectValue.BooleanValue -> {
                vertex.booleanValue = objectValue.value
            }

            is ObjectValue.Link -> {
                val linkValue = objectValue.value
                when (linkValue) {
                    is LinkValueVertex.ObjectValue ->
                        replaceEdge(vertex, OBJECT_VALUE_OBJECT_EDGE, vertex.refValueObject, linkValue.vertex)
                    is LinkValueVertex.SubjectValue ->
                        replaceEdge(vertex, OBJECT_VALUE_SUBJECT_EDGE, vertex.refValueSubject, linkValue.vertex)
                    is LinkValueVertex.DomainElementValue ->
                        replaceEdge(
                            vertex,
                            OBJECT_VALUE_REFBOOK_ITEM_EDGE,
                            vertex.refValueDomainElement,
                            linkValue.vertex
                        )
                }
            }
        }

        replaceEdge(vertex, OBJECT_VALUE_MEASURE_EDGE, vertex.measure, valueInfo.measure)
        replaceEdge(vertex, OBJECT_VALUE_OBJECT_PROPERTY_EDGE, vertex.objectProperty, valueInfo.objectProperty)
        replaceEdge(vertex, OBJECT_VALUE_ASPECT_PROPERTY_EDGE, vertex.aspectProperty, valueInfo.aspectProperty)
        replaceEdge(vertex, OBJECT_VALUE_OBJECT_VALUE_EDGE, vertex.parentValue, valueInfo.parentValue)

        return@transaction vertex.save<OVertex>().toObjectPropertyValueVertex()
    }

    fun getObjectVertex(id: String) = db.getVertexById(id)?.toObjectVertex()
    fun getObjectPropertyVertex(id: String) = db.getVertexById(id)?.toObjectPropertyVertex()
    fun getObjectPropertyValueVertex(id: String) = db.getVertexById(id)?.toObjectPropertyValueVertex()
}

abstract class ObjectException(message: String) : Exception(message)
class EmptyObjectNameException(data: ObjectCreateRequest) : ObjectException("object name is empty: $data")
class EmptyObjectPropertyNameException(data: PropertyCreateRequest) : ObjectException("object name is empty: $data")
class ObjectNotFoundException(id: String) : ObjectException("object not found. id: $id")
class ObjectPropertyNotFoundException(id: String) : ObjectException("object property not found. id: $id")
class ObjectPropertyValueNotFoundException(id: String) : ObjectException("object property value not found. id: $id")