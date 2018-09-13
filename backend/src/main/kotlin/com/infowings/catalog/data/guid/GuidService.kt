package com.infowings.catalog.data.guid

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityClass
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.common.objekt.PropertyUpdateResponse
import com.infowings.catalog.common.objekt.Reference
import com.infowings.catalog.common.toDTO
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.objekt.ObjectVertex
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

@Suppress("TooManyFunctions")
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

    fun findObject(guid: String): BriefObjectViewResponse {
        return transaction(db) {
            dao.find(listOf(guid)).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT) vertex.toObjectVertex().toBriefViewResponse() else null
            }.singleOrNull() ?: throw EntityNotFoundException("No object is found by guid: $guid")
        }
    }

    fun findObjectById(id: String): BriefObjectViewResponse {
        return transaction(db) {
            val vertex = dao.findById(id)
            if (vertex.entityClass() == EntityClass.OBJECT) vertex.toObjectVertex().toBriefViewResponse()
            else throw EntityNotFoundException("No object is found by id: $id")
        }
    }

    private fun ObjectVertex.toBriefViewResponse() = BriefObjectViewResponse(
        name,
        subject?.name
    )

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

    fun findObjectValue(guid: String): BriefValueViewResponse {
        return transaction(db) {
            dao.find(listOf(guid)).mapNotNull { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                if (vertex.entityClass() == EntityClass.OBJECT_VALUE) {
                    val valueVertex = vertex.toObjectPropertyValueVertex()
                    val propertyValue = valueVertex.toObjectPropertyValue()
                    val aspectPropertyVertex = valueVertex.aspectProperty
                    aspectPropertyVertex?.let {
                        BriefValueViewResponse(
                            valueVertex.guid,
                            propertyValue.value.toObjectValueData().toDTO(),
                            it.name,
                            it.associatedAspect.name,
                            valueVertex.measure?.name
                        )
                    } ?: run {
                        val objectPropertyVertex = valueVertex.objectProperty ?: throw IllegalStateException()
                        BriefValueViewResponse(
                            valueVertex.guid,
                            propertyValue.value.toObjectValueData().toDTO(),
                            objectPropertyVertex.name,
                            objectPropertyVertex.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                            valueVertex.measure?.name
                        )
                    }
                } else null
            }.singleOrNull() ?: throw EntityNotFoundException("No value is found by guid: $guid")
        }
    }

    fun findObjectValueById(id: String): BriefValueViewResponse {
        return transaction(db) {
            val vertex = dao.findById(id)
            if (vertex.entityClass() == EntityClass.OBJECT_VALUE) {
                val valueVertex = vertex.toObjectPropertyValueVertex()
                val propertyValue = valueVertex.toObjectPropertyValue()
                val aspectPropertyVertex = valueVertex.aspectProperty
                aspectPropertyVertex?.let {
                    BriefValueViewResponse(
                        valueVertex.guid,
                        propertyValue.value.toObjectValueData().toDTO(),
                        it.name,
                        it.associatedAspect.name,
                        valueVertex.measure?.name
                    )
                } ?: run {
                    val objectPropertyVertex = valueVertex.objectProperty ?: throw IllegalStateException()
                    BriefValueViewResponse(
                        valueVertex.guid,
                        propertyValue.value.toObjectValueData().toDTO(),
                        objectPropertyVertex.name,
                        objectPropertyVertex.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                        valueVertex.measure?.name
                    )
                }
            } else throw EntityNotFoundException("No value is found by id: $id")
        }
    }

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

    init { setGuidsAll() }

    @Suppress("TooGenericExceptionCaught")
    private fun setGuidsAll() {
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

@Suppress("SpreadOperator")
private fun OVertex.hasGuidEdge() = getEdges(ODirection.OUT, *edgesWithGuid.map { it.extName }.toTypedArray()).firstOrNull() != null

sealed class GuidApiException(override val message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : GuidApiException(message)
