package com.infowings.catalog.data.guid

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.common.toDTO
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.objekt.Objekt
import com.infowings.catalog.data.objekt.toObjectPropertyValueVertex
import com.infowings.catalog.data.objekt.toObjectPropertyVertex
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

class GuidService(
    private val db: OrientDatabase,
    private val dao: GuidDaoService,
    private val userService: UserService,
    private val historyService: HistoryService) {
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
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.ASPECT_PROPERTY) vertex.toAspectPropertyVertex().toAspectPropertyData() else null
            }
        }
    }

    fun findSubject(guids: List<String>): List<Subject> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.SUBJECT) vertex.toSubjectVertex().toSubject() else null
            }
        }
    }

    fun findRefBookItems(guids: List<String>): List<ReferenceBookItem> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.REFBOOK_ITEM) vertex.toReferenceBookItemVertex().toReferenceBookItem() else null
            }
        }
    }

    fun findObjects(guids: List<String>): List<Objekt> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT) vertex.toObjectVertex().toObjekt() else null
            }
        }
    }

    fun findObjectProperties(guids: List<String>): List<PropertyUpdateResponse> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT_PROPERTY) {
                    val propertyVertex = vertex.toObjectPropertyVertex()
                    val objectVertex = propertyVertex.objekt ?: throw IllegalStateException()
                    PropertyUpdateResponse(
                        propertyVertex.id, Reference(objectVertex.id, objectVertex.version),
                        propertyVertex.name,
                        propertyVertex.description,
                        propertyVertex.version, propertyVertex.guid
                    )
                } else null
            }
        }
    }

    fun findObjectValues(guids: List<String>): List<FoundValueResponse> {
        return transaction(db) {
            dao.find(guids).mapNotNull { guidVertex ->
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
            }
        }
    }

    private fun OVertex.hasGuidEdge() = getEdges(ODirection.OUT, *edgesWithGuid.map { it.extName }.toTypedArray()).firstOrNull() != null

    fun setGuid(id: String, username: String): EntityMetadata {
        val userVertex = userService.findUserVertexByUsername(username)
        val context = HistoryContext(userVertex)

        return transaction(db) {
            val vertex = db.getVertexById(id) ?: throw IllegalStateException("no vertex of id $id")
            if (vertex.hasGuidEdge()) {
                throw IllegalStateException("guid is already defined")
            }

            val historyVertex = vertex.asHistoryAware()

            val before = historyVertex.currentSnapshot()
            val guidVertex = dao.newGuidVertex(vertex)
            historyService.storeFact(historyVertex.toUpdateFact(context, before))
            EntityMetadata(guid = guidVertex.guid, entityClass = vertex.entityClass(), id = vertex.id)
        }
    }

    fun setGuids(orientClass: OrientClass, username: String): List<EntityMetadata> {
        val idsToSet = transaction(db) {
            db.query("select from ${orientClass.extName}") {
                it.toList().map { it.toVertex() }.filterNot { it.hasGuidEdge() }.map { it.id }
            }
        }
        logger.info("ids to set guid for class $orientClass: $idsToSet")
        return idsToSet.map { setGuid(it, username) }
    }

    init {
        edgesWithGuid.forEach {
            try {
                val result = this.setGuids(dao.edge2Class.getValue(it), "admin")
                logger.info("results of setting $it")
                result.forEach {  metaInfo -> logger.info(metaInfo.toString()) }
            } catch (e: RuntimeException) {
                logger.warn("exception during setting of guids for $it")
            }
        }
    }
}

private val edgesWithGuid: List<OrientEdge> = listOf(
    OrientEdge.GUID_OF_OBJECT_PROPERTY, OrientEdge.GUID_OF_OBJECT, OrientEdge.GUID_OF_OBJECT_VALUE,
    OrientEdge.GUID_OF_ASPECT_PROPERTY, OrientEdge.GUID_OF_ASPECT, OrientEdge.GUID_OF_SUBJECT,
    OrientEdge.GUID_OF_REFBOOK_ITEM
)

private val logger = loggerFor<GuidService>()
