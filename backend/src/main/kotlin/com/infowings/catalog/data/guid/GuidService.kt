package com.infowings.catalog.data.guid

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
import com.infowings.catalog.data.objekt.ObjectVertex
import com.infowings.catalog.data.objekt.toObjectPropertyValueVertex
import com.infowings.catalog.data.objekt.toObjectPropertyVertex
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*

@Suppress("TooManyFunctions")
class GuidService(private val db: OrientDatabase, private val dao: GuidDaoService) {
    fun metadata(guids: List<String>): List<EntityMetadata> {
        return transaction(db) {
            val aspectEntities = dao.findByGuidInClass(OrientClass.ASPECT, guids) {
                val aspectVertex = it.toAspectVertex()
                EntityMetadata(guid = aspectVertex.guid, entityClass = EntityClass.ASPECT, id = aspectVertex.id)
            }
            val aspectPropertyEntities = dao.findByGuidInClass(OrientClass.ASPECT_PROPERTY, guids) {
                val aspectPropertyVertex = it.toAspectPropertyVertex()
                EntityMetadata(guid = aspectPropertyVertex.guid, entityClass = EntityClass.ASPECT_PROPERTY, id = aspectPropertyVertex.id)
            }
            val subjectEntities = dao.findByGuidInClass(OrientClass.SUBJECT, guids) {
                val subjectVertex = it.toSubjectVertex()
                EntityMetadata(guid = subjectVertex.guid, entityClass = EntityClass.SUBJECT, id = subjectVertex.id)
            }
            val allOtherEntities = dao.find(guids).map { guidVertex ->
                val vertex = dao.vertex(guidVertex)
                EntityMetadata(guid = guidVertex.guid, entityClass = vertex.entityClass(), id = vertex.id)
            }
            allOtherEntities + aspectEntities + aspectPropertyEntities + subjectEntities
        }
    }

    fun findAspects(guids: List<String>): List<AspectData> = dao.findByGuidInClass(OrientClass.ASPECT, guids) { it.toAspectVertex().toAspectData() }

    fun findAspectProperties(guids: List<String>): List<AspectPropertyData> =
        dao.findByGuidInClass(OrientClass.ASPECT_PROPERTY, guids) { it.toAspectPropertyVertex().toAspectPropertyData() }

    fun findSubject(guids: List<String>): List<Subject> =
        dao.findByGuidInClass(OrientClass.SUBJECT, guids) { it.toSubjectVertex().toSubject() }

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
                    val objectVertex = valueVertex.objectProperty?.objekt
                        ?: throw IllegalStateException("object of value ${valueVertex.toObjectPropertyValue()} is not found")
                    val aspectPropertyVertex = valueVertex.aspectProperty
                    val measureSymbol = valueVertex.getOrCalculateMeasureSymbol()
                    aspectPropertyVertex?.let {
                        BriefValueViewResponse(
                            valueVertex.guid,
                            propertyValue.calculateObjectValueData().toDTO(),
                            it.name,
                            it.associatedAspect.name,
                            measureSymbol,
                            objectVertex.id,
                            objectVertex.guid ?: "???"
                        )
                    } ?: run {
                        val objectPropertyVertex = valueVertex.objectProperty ?: throw IllegalStateException()
                        BriefValueViewResponse(
                            valueVertex.guid,
                            propertyValue.calculateObjectValueData().toDTO(),
                            objectPropertyVertex.name,
                            objectPropertyVertex.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                            measureSymbol,
                            objectVertex.id,
                            objectVertex.guid ?: "???"
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
                val objectVertex = valueVertex.objectProperty?.objekt
                    ?: throw IllegalStateException("object of value ${valueVertex.toObjectPropertyValue()} is not found")
                val propertyValue = valueVertex.toObjectPropertyValue()
                val aspectPropertyVertex = valueVertex.aspectProperty
                val measureSymbol = valueVertex.getOrCalculateMeasureSymbol()
                aspectPropertyVertex?.let {
                    BriefValueViewResponse(
                        valueVertex.guid,
                        propertyValue.calculateObjectValueData().toDTO(),
                        it.name,
                        it.associatedAspect.name,
                        measureSymbol,
                        objectVertex.id,
                        objectVertex.guid ?: "???"
                    )
                } ?: run {
                    val objectPropertyVertex = valueVertex.objectProperty ?: throw IllegalStateException()
                    BriefValueViewResponse(
                        valueVertex.guid,
                        propertyValue.calculateObjectValueData().toDTO(),
                        objectPropertyVertex.name,
                        objectPropertyVertex.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                        measureSymbol,
                        objectVertex.id,
                        objectVertex.guid ?: "???"
                    )
                }
            } else throw EntityNotFoundException("No value is found by id: $id")
        }
    }
}

private val logger = loggerFor<GuidService>()

sealed class GuidApiException(override val message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : GuidApiException(message)
