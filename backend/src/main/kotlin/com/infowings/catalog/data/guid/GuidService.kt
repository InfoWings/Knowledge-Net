package com.infowings.catalog.data.guid

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.common.toDTO
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.objekt.Objekt
import com.infowings.catalog.data.objekt.toObjectPropertyValueVertex
import com.infowings.catalog.data.objekt.toObjectPropertyVertex
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection

class GuidService(private val db: OrientDatabase, private val dao: GuidDaoService) {
    fun metadata(guids: List<String>): List<EntityMetadata> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                EntityMetadata(guid = guidVertex.guid, entityClass = vertex.entityClass(), id = vertex.id)
            }
        }
    }

    fun findAspects(guids: List<String>): List<AspectData> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.ASPECT) vertex.toAspectVertex().toAspectData() else null
            }
        }
    }

    fun findAspectProperties(guids: List<String>): List<AspectPropertyData> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.ASPECT_PROPERTY) vertex.toAspectPropertyVertex().toAspectPropertyData() else null
            }.filterNotNull()
        }
    }

    fun findSubject(guids: List<String>): List<Subject> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.SUBJECT) vertex.toSubjectVertex().toSubject() else null
            }.filterNotNull()
        }
    }

    fun findRefBookItems(guids: List<String>): List<ReferenceBookItem> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.REFBOOK_ITEM) vertex.toReferenceBookItemVertex().toReferenceBookItem() else null
            }.filterNotNull()
        }
    }

    fun findObjects(guids: List<String>): List<Objekt> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT) vertex.toObjectVertex().toObjekt() else null
            }.filterNotNull()
        }
    }

    fun findObjectProperties(guids: List<String>): List<PropertyUpdateResponse> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT_PROPERTY) {
                    val propertyVertex = vertex.toObjectPropertyVertex()
                    val objectVertex = propertyVertex.objekt ?: throw IllegalStateException()
                    PropertyUpdateResponse(propertyVertex.id, Reference(objectVertex.id, objectVertex.version),
                        propertyVertex.name,
                        propertyVertex.description,
                        propertyVertex.version, propertyVertex.guid)
                } else null
            }.filterNotNull()
        }
    }

    fun findObjectValues(guids: List<String>): List<FoundValueResponse> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT_VALUE) {
                    val valueVertex = vertex.toObjectPropertyValueVertex()
                    val propertyValue = valueVertex.toObjectPropertyValue()
                    val propertyVertex = valueVertex.objectProperty ?: throw IllegalStateException()
                    FoundValueResponse(
                        valueVertex.guid,
                        propertyValue.value.toObjectValueData().toDTO(),
                        propertyVertex.name,
                        propertyVertex.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                        valueVertex.measure?.name
                    )
                } else null
            }.filterNotNull()
        }
    }

    fun setGuid(id: String): EntityMetadata {
        return transaction(db) {
            val vertex = db.getVertexById(id) ?: throw IllegalStateException("no vertex of id $id")
            if (vertex.getEdges(ODirection.OUT, OrientEdge.GUID_OF_OBJECT_PROPERTY.extName, OrientEdge.GUID_OF_OBJECT.extName, OrientEdge.GUID_OF_OBJECT_VALUE.extName,
                OrientEdge.GUID_OF_ASPECT_PROPERTY.extName, OrientEdge.GUID_OF_ASPECT.extName, OrientEdge.GUID_OF_SUBJECT.extName, OrientEdge.GUID_OF_REFBOOK_ITEM.extName).singleOrNull() != null) {
                throw IllegalStateException("guid is already defined")
            }
            val guidVertex = dao.newGuidVertex(vertex)
            EntityMetadata(guid = guidVertex.guid, entityClass = vertex.entityClass(), id = vertex.id)
        }
    }
}