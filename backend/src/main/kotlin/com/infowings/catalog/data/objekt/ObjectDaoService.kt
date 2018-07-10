package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
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

    fun getTruncatedObjects() =
        transaction(db) {
            val query =
                "SELECT @rid, name, description, " +
                        "FIRST(OUT($OBJECT_SUBJECT_EDGE)).name as subjectName, " +
                        "IN($OBJECT_OBJECT_PROPERTY_EDGE).size() as objectPropertiesCount " +
                        "FROM $OBJECT_CLASS"
            return@transaction db.query(query) {
                it.map {
                    ObjectTruncated(
                        it.getProperty("@rid"),
                        it.getProperty("name"),
                        it.getProperty("description"),
                        it.getProperty("subjectName"),
                        it.getProperty("objectPropertiesCount")
                    )
                }
            }.toList()
        }

    fun getPropertyValues(propertyVertex: ObjectPropertyVertex): List<RootValueResponse> =
        transaction(db) {
            val rootPropertyValues = propertyVertex.values.filter { it.aspectProperty == null }
            return@transaction rootPropertyValues.map { rootValue ->
                RootValueResponse(
                    rootValue.id,
                    rootValue.toObjectPropertyValue().value.toObjectValueData().toDTO(),
                    rootValue.description,
                    rootValue.children.map { it.toDetailedAspectPropertyValueResponse() }
                )
            }
        }

    private fun ObjectPropertyValueVertex.toDetailedAspectPropertyValueResponse(): ValueResponse {
        val aspectProperty = this.aspectProperty ?: throw IllegalStateException("Object property with id ${this.id} has no associated aspect")
        val aspect = aspectProperty.associatedAspect
        return ValueResponse(
            this.id,
            this.toObjectPropertyValue().value.toObjectValueData().toDTO(),
            this.description,
            AspectPropertyDataExtended(
                aspectProperty.id,
                aspectProperty.name,
                aspect.id,
                aspectProperty.cardinality,
                aspect.name,
                aspect.measure?.name,
                OpenDomain(BaseType.restoreBaseType(aspect.baseType)).toString(),
                aspect.baseType ?: throw IllegalStateException("Aspect with id ${aspect.id} has no associated base type"),
                aspect.referenceBookRootVertex?.name
            ),
            this.children.map { it.toDetailedAspectPropertyValueResponse() }
        )
    }

    fun saveObject(vertex: ObjectVertex, info: ObjectWriteInfo, properties: List<ObjectPropertyVertex>): ObjectVertex =
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

    fun updateObject(vertex: ObjectVertex, info: ObjectWriteInfo): ObjectVertex =
        transaction(db) {
            vertex.name = info.name
            vertex.description = info.description
            return@transaction vertex.save<OVertex>().toObjectVertex()
        }

    fun saveObjectProperty(
        vertex: ObjectPropertyVertex,
        info: PropertyWriteInfo,
        values: List<ObjectPropertyValueVertex>
    ): ObjectPropertyVertex =
        transaction(db) {
            vertex.name = info.name

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

    fun delete(deleteInfo: DeleteInfo) {
        transaction(db) {
            deleteInfo.incoming.forEach { db.delete(it) }
            deleteInfo.outgoing.forEach { db.delete(it) }
            db.delete(deleteInfo.vertex)
        }
    }

    fun softDelete(vertex: DeletableVertex) {
        transaction(db) {
            vertex.deleted = true
        }
    }

    fun getObjectVertex(id: String) = db.getVertexById(id)?.toObjectVertex()
    fun getObjectPropertyVertex(id: String) = db.getVertexById(id)?.toObjectPropertyVertex()
    fun getObjectPropertyValueVertex(id: String) = db.getVertexById(id)?.toObjectPropertyValueVertex()

    fun getObjectVertexesByNameAndSubject(name: String, subjectId: ORID): List<ObjectVertex> {
        val sqlBuilder = objectSqlBuilder {
            allObjects {
                withoutDeleted()
                withName(name)
                withSubjectLink(subjectId)
            }
        }
        return db.query(sqlBuilder.sql, sqlBuilder.params) { rs ->
            rs.map { it.toVertex().toObjectVertex() }.toList()
        }
    }

    fun getPropertyVertexesByNameAndAspect(name: String?, objectId: ORID, aspectId: ORID): List<ObjectPropertyVertex> {

        val sqlBuilder = objectSqlBuilder {
            fromObject(objectId) {
                toObjProps {
                    withName(name)
                    withoutDeleted()
                    withAspect(aspectId)
                }
            }
        }

        return db.query(sqlBuilder.sql, sqlBuilder.params) { rs ->
            rs.map { it.toVertex().toObjectPropertyVertex() }.toList()
        }
    }

    fun getValuesByObjectPropertyAndValue(objPropertyId: ORID, objectValue: ObjectValue): List<ObjectPropertyValueVertex> {

        val sqlBuilder = objectSqlBuilder {
            fromObjProp(objPropertyId) {
                toObjValues {
                    withoutDeleted()
                    when (objectValue) {
                        is ObjectValue.IntegerValue -> withIntValue(objectValue.value)
                        is ObjectValue.DecimalValue -> withDecimalValue(objectValue.value)
                        is ObjectValue.StringValue -> withStrValue(objectValue.value)
                        is ObjectValue.RangeValue -> withRange(objectValue.range)
                        is ObjectValue.BooleanValue -> withBooleanValue(objectValue.value)
                        is ObjectValue.Link -> {
                            val linkValue = objectValue.value
                            when (linkValue) {
                                is LinkValueVertex.ObjectValue -> withObjectLink(linkValue.vertex.identity)
                                is LinkValueVertex.SubjectValue -> withSubjectLink(linkValue.vertex.identity)
                                is LinkValueVertex.DomainElementValue -> withDomainElementLink(linkValue.vertex.identity)
                            }
                        }
                        ObjectValue.NullValue -> withNullValue()
                    }
                }
            }
        }

        logger.info("sql: ${sqlBuilder.sql}")
        logger.info("params: ${sqlBuilder.params}")

        return db.query(sqlBuilder.sql, sqlBuilder.params) { rs ->
            rs.map { it.toVertex().toObjectPropertyValueVertex() }.toList()
        }
    }
}

private val logger = loggerFor<ObjectDaoService>()

sealed class ObjectException(message: String) : Exception(message)
class EmptyObjectNameException(data: ObjectCreateRequest) : ObjectException("object name is empty: $data")
class ObjectNotFoundException(id: String) : ObjectException("object not found. id: $id")
class ObjectAlreadyExists(name: String) : ObjectException("object with name $name already exists")
class ObjectPropertyNotFoundException(id: String) : ObjectException("object property not found. id: $id")
class ObjectPropertyAlreadyExistException(name: String?, objectId: String, aspectId: String) :
    ObjectException("object property with name $name and aspect $aspectId already exists in object $objectId")

class ObjectPropertyValueNotFoundException(id: String) : ObjectException("object property value not found. id: $id")
class ObjectWithoutSubjectException(id: String) : ObjectException("Object vertex $id has no subject")
class ObjectPropertyValueAlreadyExists(value: ObjectValueData) : ObjectException("Object property value with value $value already exists")
class ObjectHasPropertiesException(ids: List<String>) :
        ObjectException("properties: $ids")
