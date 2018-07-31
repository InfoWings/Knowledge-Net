package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.ObjectUpdateRequest
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
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

    fun getSubValues(id: String): Set<ObjectPropertyValueVertex> = getSubValues(ORecordId(id))

    fun getSubValues(id: ORID): Set<ObjectPropertyValueVertex> = transaction(db) {
        db.query("SELECT FROM (TRAVERSE IN(\"$OBJECT_VALUE_OBJECT_VALUE_EDGE\") FROM :id)", mapOf("id" to id)) {
            it.map { it.toVertex().toObjectPropertyValueVertex() }.toSet()
        }
    }

    fun valuesOfProperty(id: String): Set<ObjectPropertyValueVertex> = valuesOfProperty(ORecordId(id))

    private fun valuesOfProperty(id: ORID): Set<ObjectPropertyValueVertex> = transaction(db) {
        db.query("select expand(in(\"$OBJECT_VALUE_OBJECT_PROPERTY_EDGE\")) from :id", mapOf("id" to id)) {
            it.map { it.toVertex().toObjectPropertyValueVertex() }.toSet()
        }
    }

    fun valuesOfPropertiesStr(ids: List<String>): Set<ObjectPropertyValueVertex> = valuesOfProperties(ids.map { ORecordId(it) })

    fun valuesOfProperties(ids: List<ORID>): Set<ObjectPropertyValueVertex> = transaction(db) {
        db.query("select expand(in(\"$OBJECT_VALUE_OBJECT_PROPERTY_EDGE\")) from :ids", mapOf("ids" to ids)) {
            it.map { it.toVertex().toObjectPropertyValueVertex() }.toSet()
        }
    }

    fun propertiesOfObject(id: String): Set<ObjectPropertyVertex> = propertiesOfObject(ORecordId(id))

    private fun propertiesOfObject(id: ORID): Set<ObjectPropertyVertex> = transaction(db) {
        db.query("select expand(in(\"$OBJECT_OBJECT_PROPERTY_EDGE\")) from :id", mapOf("id" to id)) {
            it.map { it.toVertex().toObjectPropertyVertex() }.toSet()
        }
    }

    fun linkedFrom(id: Set<ORID>, linkTypes: Set<String>, except: Set<ORID> = emptySet()): Map<ORID, Set<ORID>> {
        val linkClasses = linkTypes.joinToString("\", \"", "\"", "\"")

        return db.query("select @rid as sourceId,  in($linkClasses).@rid as id from :id", mapOf("id" to id)) {
            it.map {
                it.getProperty<ORID>("sourceId") to it.getProperty<List<ORID>>("id").toSet().filterNot { except.contains(it) }.toSet()
            }.toMap().filterValues { it.isNotEmpty() }
        }
    }

    fun valuesBetween(sources: Set<ORID>, targets: Set<ORID>): Set<ObjectPropertyValueVertex> {
        return db.query(
            "SELECT FROM (traverse out(\"$OBJECT_VALUE_OBJECT_VALUE_EDGE\") from :sources while not  @rid in :targets)",
            mapOf("sources" to sources, "targets" to targets)
        ) {
            val res = it.toList()
            res.map { it.toVertex().toObjectPropertyValueVertex() }.toSet()
        }
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
            if (vertex.subject?.id != info.subject.id) {
                vertex.getEdges(ODirection.OUT, OBJECT_SUBJECT_EDGE).forEach { it.delete<OEdge>() }
                vertex.addEdge(info.subject, OBJECT_SUBJECT_EDGE).save<OEdge>()
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
            vertex.description = info.description

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
        vertex.description = valueInfo.description
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
                ScalarTypeTag.OBJECT_PROPERTY -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.OBJECT_VALUE -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_REF_OBJECT_VALUE_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.ASPECT -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_ASPECT_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.ASPECT_PROPERTY -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_ASPECT_PROPERTY_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.DOMAIN_ELEMENT -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_DOMAIN_ELEMENT_EDGE).forEach { it.delete<OEdge>() }
                }
                ScalarTypeTag.REF_BOOK_ITEM -> {
                    vertex.getEdges(ODirection.OUT, OBJECT_VALUE_REF_REFBOOK_ITEM_EDGE).forEach { it.delete<OEdge>() }
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
                    is LinkValueVertex.Object -> replaceEdge(vertex, OBJECT_VALUE_OBJECT_EDGE, vertex.refValueObject, linkValue.vertex)
                    is LinkValueVertex.ObjectProperty -> replaceEdge(
                        vertex,
                        OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE,
                        vertex.refValueObjectProperty,
                        linkValue.vertex
                    )
                    is LinkValueVertex.ObjectValue -> replaceEdge(vertex, OBJECT_VALUE_REF_OBJECT_VALUE_EDGE, vertex.refValueObjectValue, linkValue.vertex)
                    is LinkValueVertex.Subject -> replaceEdge(vertex, OBJECT_VALUE_SUBJECT_EDGE, vertex.refValueSubject, linkValue.vertex)
                    is LinkValueVertex.DomainElement -> replaceEdge(vertex, OBJECT_VALUE_DOMAIN_ELEMENT_EDGE, vertex.refValueDomainElement, linkValue.vertex)
                    is LinkValueVertex.Aspect -> replaceEdge(vertex, OBJECT_VALUE_ASPECT_EDGE, vertex.refValueAspect, linkValue.vertex)
                    is LinkValueVertex.AspectProperty -> replaceEdge(
                        vertex,
                        OBJECT_VALUE_REF_ASPECT_PROPERTY_EDGE,
                        vertex.refValueAspectProperty,
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


    fun deleteAll(vertices: List<OVertex>) {
        transaction(db) {
            vertices.forEach { db.delete(it) }
        }
    }

    fun delete(vertex: OVertex) = deleteAll(listOf(vertex))


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

    fun getObjectVertex(id: String) = transaction(db) { db.getVertexById(id)?.toObjectVertex() }
    fun getObjectPropertyVertex(id: String) = transaction(db) { db.getVertexById(id)?.toObjectPropertyVertex() }
    fun getObjectPropertyValueVertex(id: String) = transaction(db) { db.getVertexById(id)?.toObjectPropertyValueVertex() }

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
                                is LinkValueVertex.Object -> withObjectLink(linkValue.vertex.identity)
                                is LinkValueVertex.ObjectProperty -> withObjectPropertyLink(linkValue.vertex.identity)
                                is LinkValueVertex.ObjectValue -> withObjectValueLink(linkValue.vertex.identity)
                                is LinkValueVertex.Subject -> withSubjectLink(linkValue.vertex.identity)
                                is LinkValueVertex.DomainElement -> withDomainElementLink(linkValue.vertex.identity)
                                is LinkValueVertex.Aspect -> withAspectLink(linkValue.vertex.identity)
                                is LinkValueVertex.AspectProperty -> withAspectPropertyLink(linkValue.vertex.identity)
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

    fun baseType(propertyVertex: ObjectPropertyVertex): String? = transaction(db) {
        propertyVertex.aspect?.baseType
    }

}

private val logger = loggerFor<ObjectDaoService>()

sealed class ObjectException(message: String) : Exception(message)
class EmptyObjectCreateNameException(data: ObjectCreateRequest) : ObjectException("object name is empty: $data")
class EmptyObjectUpdateNameException(data: ObjectUpdateRequest) : ObjectException("object name is empty: $data")
class ObjectNotFoundException(id: String) : ObjectException("object not found. id: $id")
class ObjectAlreadyExists(name: String) : ObjectException("object with name $name already exists")
class ObjectPropertyNotFoundException(id: String) : ObjectException("object property not found. id: $id")
class ObjectPropertyAlreadyExistException(name: String?, objectId: String, aspectId: String) :
    ObjectException("object property with name $name and aspect $aspectId already exists in object $objectId")

class ObjectPropertyValueNotFoundException(id: String) : ObjectException("object property value not found. id: $id")
class ObjectWithoutSubjectException(id: String) : ObjectException("Object vertex $id has no subject")
class ObjectIsLinkedException(valueIds: List<String>, propertyIds: List<String>, objectId: String?) :
    ObjectException("linked values: $valueIds, linked properties: $propertyIds, inked object: $objectId")
