package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.DiffPayload
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.toSubjectData
import com.orientechnologies.orient.core.id.ORecordId

class AspectConstructor(private val subjectService: SubjectService, private val referenceBookService: ReferenceBookService) {

    fun toNextVersion(aspect: AspectData, fact: HistoryFactDto, relatedFacts: List<HistoryFactDto>): AspectData {

        val newProps = fact.payload.addedFor(AspectField.PROPERTY).map { emptyAspectPropertyData.copy(id = it.toString()) }
        val updatedProps = aspect.properties.plus(newProps).submit(relatedFacts)

        return aspect.submitFieldsEvents(fact).copy(properties = updatedProps)
            .submitSubjectEvents(fact.payload)
            .submitReferenceBookEvents(fact.payload)
    }

    private fun AspectData.submitSubjectEvents(payload: DiffPayload): AspectData {
        val afterAdded = payload.addedFor(AspectField.SUBJECT).fold(this) { acc, nextFact ->
            acc.copy(subject = subjectService.findById(nextFact.identity.toString())?.toSubject()?.toSubjectData())
        }

        return payload.removedFor(AspectField.SUBJECT).fold(afterAdded) { acc, _ ->
            acc.copy(subject = null)
        }
    }

    private fun AspectData.submitReferenceBookEvents(payload: DiffPayload): AspectData {
        val afterAdded = payload.removedFor(AspectField.REFERENCE_BOOK).fold(this) { acc, nextFact ->
            acc.copy(refBookName = referenceBookService.getReferenceBookNameById(nextFact.identity.toString()) ?: "Deleted")
        }

        return payload.removedFor(AspectField.REFERENCE_BOOK).fold(afterAdded) { acc, _ ->
            acc.copy(refBookName = null)
        }
    }

    private fun AspectData.submitFieldsEvents(fact: HistoryFactDto): AspectData = when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> {
            val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
            copy(
                measure = fact.payload.data.getOrDefault(AspectField.MEASURE.name, measure),
                baseType = fact.payload.data.getOrDefault(AspectField.BASE_TYPE.name, baseType),
                name = fact.payload.data.getOrDefault(AspectField.NAME.name, name),
                description = fact.payload.data.getOrDefault(AspectField.DESCRIPTION.name, description),
                domain = baseTypeObj?.let { OpenDomain(it).toString() },
                version = fact.event.version
            )
        }
        else -> copy(deleted = true, version = fact.event.version)
    }


    private fun List<AspectPropertyData>.submit(events: List<HistoryFactDto>): List<AspectPropertyData> {

        val propertyEventMap = events.groupBy { it.event.entityId }

        val updatedProps = map { aspectPropertyData ->
            val relatedEvents = propertyEventMap[ORecordId(aspectPropertyData.id)]?.sortedBy { it.event.timestamp } ?: emptyList()
            var initial = aspectPropertyData
            relatedEvents.forEach {
                initial = aspectPropertyData.submitFieldsEvents(it)
            }
            return@map initial
        }

        return updatedProps.filterNot { it.deleted }
    }

    private fun AspectPropertyData.submitFieldsEvents(fact: HistoryFactDto): AspectPropertyData = when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> copy(
            name = fact.payload.data.getOrDefault(AspectPropertyField.NAME.name, name),
            cardinality = fact.payload.data.getOrDefault(AspectPropertyField.CARDINALITY.name, cardinality),
            aspectId = fact.payload.data.getOrDefault(AspectPropertyField.ASPECT.name, aspectId),
            version = fact.event.version
        )
        else -> copy(deleted = true, version = fact.event.version)
    }
}
