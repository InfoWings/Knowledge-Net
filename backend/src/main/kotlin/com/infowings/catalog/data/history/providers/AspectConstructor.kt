package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.DiffPayload
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.toSubjectData
import com.orientechnologies.orient.core.id.ORecordId

class AspectConstructor(
    private val subjectService: SubjectService,
    private val referenceBookService: ReferenceBookService
) {

    fun toNextVersion(aspect: AspectData, fact: HistoryFact, relatedFacts: List<HistoryFact>): AspectData {

        val newProps =
            fact.payload.addedFor(AspectField.PROPERTY).map { emptyAspectPropertyData.copy(id = it.toString()) }
        val updatedProps = aspect.properties.plus(newProps).submit(relatedFacts)

        return aspect.submitFieldsEvents(fact).copy(properties = updatedProps)
            .submitSubjectEvents(fact.payload)
            .submitReferenceBookEvents(fact.payload)
    }

    private fun AspectData.submitSubjectEvents(payload: DiffPayload): AspectData {
        val afterAdded = payload.addedFor(AspectField.SUBJECT).fold(this) { acc, nextFact ->
            val entityId = nextFact.identity.toString()

            // если когда-то добавляли субъект, но сейчас он удален, то подсталяем специальный маркер
            val subjectData =
                subjectService.findById(entityId)?.toSubject()?.toSubjectData() ?: removedSubjectPlaceholder(entityId)

            acc.copy(subject = subjectData)
        }

        return payload.removedFor(AspectField.SUBJECT).fold(afterAdded) { acc, _ ->
            acc.copy(subject = null)
        }
    }

    private fun AspectData.submitReferenceBookEvents(payload: DiffPayload): AspectData {
        val afterAdded = payload.addedFor(AspectField.REFERENCE_BOOK).fold(this) { acc, nextFact ->
            acc.copy(
                refBookName = referenceBookService.getReferenceBookNameById(nextFact.identity.toString()) ?: "Deleted"
            )
        }

        return payload.removedFor(AspectField.REFERENCE_BOOK).fold(afterAdded) { acc, _ ->
            acc.copy(refBookName = null)
        }
    }

    private fun AspectData.submitFieldsEvents(fact: HistoryFact): AspectData = when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> {
            val newBaseType = fact.payload.data.getOrDefault(AspectField.BASE_TYPE.name, baseType)
            copy(
                measure = fact.payload.data.getOrDefault(AspectField.MEASURE.name, measure),
                baseType = newBaseType,
                name = fact.payload.data.getOrDefault(AspectField.NAME.name, name),
                description = fact.payload.data.getOrDefault(AspectField.DESCRIPTION.name, description),
                domain = newBaseType?.let { OpenDomain(BaseType.restoreBaseType(it)).toString() },
                version = fact.event.version
            )
        }
        else -> copy(deleted = true, version = fact.event.version)
    }


    private fun List<AspectPropertyData>.submit(events: List<HistoryFact>): List<AspectPropertyData> {

        val propertyEventMap = events.groupBy { it.event.entityId }

        val updatedProps = map { aspectPropertyData ->
            val relatedEvents =
                propertyEventMap[aspectPropertyData.id]?.sortedBy { it.event.timestamp } ?: emptyList()
            return@map relatedEvents.fold(aspectPropertyData) { acc, event -> acc.submitFieldsEvents(event) }
        }

        return updatedProps.filterNot { it.deleted }
    }

    private fun AspectPropertyData.submitFieldsEvents(fact: HistoryFact): AspectPropertyData = when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> copy(
            name = fact.payload.data.getOrDefault(AspectPropertyField.NAME.name, name),
            cardinality = fact.payload.data.getOrDefault(AspectPropertyField.CARDINALITY.name, cardinality),
            aspectId = fact.payload.data.getOrDefault(AspectPropertyField.ASPECT.name, aspectId),
            description = fact.payload.data.getOrDefault(AspectPropertyField.DESCRIPTION.name, description),
            version = fact.event.version
        )
        else -> copy(deleted = true, version = fact.event.version)
    }
}

fun removedSubjectPlaceholder(id: String) = SubjectData(id = id, name = "REMOVED SUBJECT", description = null)
