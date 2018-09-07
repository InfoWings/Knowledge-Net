package com.infowings.catalog.data.guid

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.PropertyUpdateResponse
import com.infowings.catalog.common.objekt.Reference
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.objekt.*
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection

data class EntityMetadata(val guid: String, val entityClass: String, val id: String)

class GuidService(private val db: OrientDatabase, private val dao: GuidDaoService) {
    fun metadata(guids: List<String>): List<EntityMetadata> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                EntityMetadata(guid = guidVertex.guid, entityClass = vertex.vertexClass(), id = vertex.id)
            }
        }
    }

    fun findAspects(guids: List<String>): List<AspectData> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.ASPECT.extName) vertex.toAspectVertex().toAspectData() else null
            }.filterNotNull()
        }
    }

    fun findAspectProperties(guids: List<String>): List<AspectPropertyData> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.ASPECT_PROPERTY.extName) vertex.toAspectPropertyVertex().toAspectPropertyData() else null
            }.filterNotNull()
        }
    }

    fun findSubject(guids: List<String>): List<Subject> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.SUBJECT.extName) vertex.toSubjectVertex().toSubject() else null
            }.filterNotNull()
        }
    }

    fun findRefBookItems(guids: List<String>): List<ReferenceBookItem> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.REFBOOK_ITEM.extName) vertex.toReferenceBookItemVertex().toReferenceBookItem() else null
            }.filterNotNull()
        }
    }

    fun findObjects(guids: List<String>): List<Objekt> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.OBJECT.extName) vertex.toObjectVertex().toObjekt() else null
            }.filterNotNull()
        }
    }

    fun findObjectProperties(guids: List<String>): List<PropertyUpdateResponse> {
        return transaction(db) {
            dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.vertexClass() == OrientClass.OBJECT_PROPERTY.extName) {
                    val propertyVertex = vertex.toObjectPropertyVertex()
                    val objectVertex = propertyVertex.objekt ?: throw IllegalStateException()
                    PropertyUpdateResponse(propertyVertex.id, Reference(objectVertex.id, objectVertex.version),
                        propertyVertex.name,
                        propertyVertex.description,
                        propertyVertex.version)
                } else null
            }.filterNotNull()
        }
    }

    fun setGuid(id: String): EntityMetadata {
        return transaction(db) {
            val vertex = db.getVertexById(id) ?: throw IllegalStateException("no vertex of id $id")
            if (vertex.getEdges(ODirection.OUT, OrientEdge.GUID_OF_OBJECT_PROPERTY.extName, OrientEdge.GUID_OF_OBJECT.extName,
                OrientEdge.GUID_OF_ASPECT_PROPERTY.extName, OrientEdge.GUID_OF_ASPECT.extName).firstOrNull() != null) {
                throw IllegalStateException("guid is already defined")
            }
            val guidVertex = dao.newGuidVertex(vertex)
            EntityMetadata(guid = guidVertex.guid, entityClass = vertex.vertexClass(), id = vertex.id)
        }
    }

}
