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
import com.infowings.catalog.data.objekt.*
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
            val refBookItemEntities = dao.findByGuidInClass(OrientClass.REFBOOK_ITEM, guids) {
                val referenceBookItemVertex = it.toReferenceBookItemVertex()
                EntityMetadata(guid = referenceBookItemVertex.guid, entityClass = EntityClass.REFBOOK_ITEM, id = referenceBookItemVertex.id)
            }
            val objectEntities = dao.findByGuidInClass(OrientClass.OBJECT, guids) {
                val objectVertex = it.toObjectVertex()
                EntityMetadata(guid = objectVertex.guid, entityClass = EntityClass.OBJECT, id = objectVertex.id)
            }
            val objectPropertyEntities = dao.findByGuidInClass(OrientClass.OBJECT_PROPERTY, guids) {
                val objectVertex = it.toObjectPropertyVertex()
                EntityMetadata(guid = objectVertex.guid, entityClass = EntityClass.OBJECT_PROPERTY, id = objectVertex.id)
            }
            val objectPropertyValueEntities = dao.findByGuidInClass(OrientClass.OBJECT_VALUE, guids) {
                val objectVertex = it.toObjectPropertyValueVertex()
                EntityMetadata(guid = objectVertex.guid, entityClass = EntityClass.OBJECT_VALUE, id = objectVertex.id)
            }
            listOf(
                aspectEntities,
                aspectPropertyEntities,
                subjectEntities,
                refBookItemEntities,
                objectEntities,
                objectPropertyEntities,
                objectPropertyValueEntities
            ).flatten()
        }
    }

    fun findAspects(guids: List<String>): List<AspectData> = dao.findByGuidInClass(OrientClass.ASPECT, guids) { it.toAspectVertex().toAspectData() }

    fun findAspectProperties(guids: List<String>): List<AspectPropertyData> =
        dao.findByGuidInClass(OrientClass.ASPECT_PROPERTY, guids) { it.toAspectPropertyVertex().toAspectPropertyData() }

    fun findSubject(guids: List<String>): List<Subject> =
        dao.findByGuidInClass(OrientClass.SUBJECT, guids) { it.toSubjectVertex().toSubject() }

    fun findRefBookItems(guids: List<String>): List<ReferenceBookItem> =
        dao.findByGuidInClass(OrientClass.REFBOOK_ITEM, guids) { it.toReferenceBookItemVertex().toReferenceBookItem() }

    fun findObject(guid: String): BriefObjectViewResponse =
        dao.findByGuidInClass(OrientClass.OBJECT, listOf(guid)) { it.toObjectVertex().toBriefViewResponse() }.singleOrNull()
            ?: throw EntityNotFoundException("No object is found by guid: $guid")


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

    fun findObjectProperties(guids: List<String>): List<PropertyUpdateResponse> =
        dao.findByGuidInClass(OrientClass.OBJECT_PROPERTY, guids) { vertex ->
            val propertyVertex = vertex.toObjectPropertyVertex()
            val objectVertex = propertyVertex.objekt ?: throw IllegalStateException()
            PropertyUpdateResponse(
                propertyVertex.id, Reference(objectVertex.id, objectVertex.version),
                propertyVertex.name,
                propertyVertex.description,
                propertyVertex.version, propertyVertex.guid
            )
        }

    fun findObjectValue(guid: String): BriefValueViewResponse = transaction(db) {
        dao.findByGuidInClass(OrientClass.OBJECT_VALUE, listOf(guid)) {
            it.toObjectPropertyValueVertex().toBriefValueViewResponse()
        }.singleOrNull() ?: throw EntityNotFoundException("No value is found by guid: $guid")
    }

    private fun ObjectPropertyValueVertex.toBriefValueViewResponse(): BriefValueViewResponse {
        val propertyValue = toObjectPropertyValue()
        val objectVertex = objectProperty?.objekt ?: throw IllegalStateException("object of value ${toObjectPropertyValue()} is not found")
        val aspectPropertyVertex = aspectProperty
        val measureSymbol = getOrCalculateMeasureSymbol()

        if (aspectPropertyVertex != null) {
            return BriefValueViewResponse(
                guid,
                propertyValue.calculateObjectValueData().toDTO(),
                aspectPropertyVertex.name,
                aspectPropertyVertex.associatedAspect.name,
                measureSymbol,
                objectVertex.id,
                objectVertex.guid
            )
        } else {
            return BriefValueViewResponse(
                guid,
                propertyValue.calculateObjectValueData().toDTO(),
                objectProperty?.name ?: throw IllegalStateException(),
                objectProperty?.aspect?.name ?: throw IllegalStateException("aspect is not defined"),
                measureSymbol,
                objectVertex.id,
                objectVertex.guid
            )
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
                        objectVertex.guid
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
                        objectVertex.guid
                    )
                }
            } else throw EntityNotFoundException("No value is found by id: $id")
        }
    }
}

private val logger = loggerFor<GuidService>()

sealed class GuidApiException(override val message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : GuidApiException(message)
