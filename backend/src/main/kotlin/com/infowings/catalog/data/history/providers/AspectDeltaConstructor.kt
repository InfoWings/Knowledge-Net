package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyCardinality
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryEvent
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.storage.ASPECT_CLASS

class AspectDeltaConstructor(val aspectService: AspectService) {

    fun createDiff(before: AspectData, after: AspectData, mainFact: HistoryFactDto): AspectHistory {

        var diffs = mutableListOf<Delta>()

        diffs.addAll(fieldsDiff(mainFact, before, after))

        if (before.subject != after.subject) {
            diffs.add(createAspectFieldDelta(mainFact.event.type, AspectField.SUBJECT, before.subject?.name, after.subject?.name))
        }

        if (before.refBookName != after.refBookName) {
            diffs.add(createAspectFieldDelta(mainFact.event.type, AspectField.REFERENCE_BOOK, before.refBookName, after.refBookName))
        }

        diffs.addAll(propertiesDiff(before, after))

        diffs = diffs.filter { it.after != it.before }.toMutableList()

        return createHistoryElement(mainFact.event, diffs, after, after.properties.map { getAspect(it.aspectId) })
    }

    private fun fieldsDiff(mainFact: HistoryFactDto, before: AspectData, after: AspectData): List<Delta> = mainFact.payload.data.mapNotNull { (fieldName, _) ->

        when (AspectField.valueOf(fieldName)) {
            AspectField.MEASURE -> createAspectFieldDelta(mainFact.event.type, AspectField.MEASURE.view, before.measure, after.measure)
            AspectField.BASE_TYPE -> createAspectFieldDelta(mainFact.event.type, AspectField.BASE_TYPE.view, before.baseType, after.baseType)
            AspectField.NAME -> createAspectFieldDelta(mainFact.event.type, AspectField.NAME.view, before.name, after.name)
            AspectField.DESCRIPTION -> createAspectFieldDelta(mainFact.event.type, AspectField.DESCRIPTION.view, before.description, after.description)
        }
    }

    private fun propertiesDiff(before: AspectData, after: AspectData): List<Delta> {

        val diffs = mutableListOf<Delta>()

        val beforePropertyIdSet = before.properties.map { it.id }.toSet()
        val afterPropertyIdSet = after.properties.map { it.id }.toSet()

        diffs.addAll(beforePropertyIdSet.intersect(afterPropertyIdSet).flatMap { id -> propertyDiff(id, EventType.UPDATE, before, after) })
        diffs.addAll(beforePropertyIdSet.subtract(afterPropertyIdSet).flatMap { id -> propertyDiff(id, EventType.DELETE, before, after) })
        diffs.addAll(afterPropertyIdSet.subtract(beforePropertyIdSet).flatMap { id -> propertyDiff(id, EventType.CREATE, before, after) })

        return diffs
    }

    private fun propertyDiff(id: String, eventType: EventType, before: AspectData, after: AspectData): List<Delta> {
        val propertyDiff = createAspectFieldDelta(eventType, after[id].deltaName, before[id]?.view, after[id]?.view)

        if (before[id]?.description != after[id]?.description) {

            val descriptionDiff =
                createAspectFieldDelta(eventType, after[id].deltaName + "(Description)", before[id]?.description, after[id]?.description)

            return listOf(propertyDiff, descriptionDiff)
        }
        return listOf(propertyDiff)
    }

    private fun createHistoryElement(event: HistoryEvent, changes: List<Delta>, data: AspectData, related: List<AspectData>) =
        AspectHistory(
            event.username,
            event.type,
            ASPECT_CLASS,
            data.name,
            data.deleted,
            event.timestamp,
            event.version,
            AspectDataView(data, related),
            changes
        )

    private val AspectPropertyData?.deltaName
        get() = "${AspectField.PROPERTY} ${this?.name ?: " "}"

    private val AspectPropertyData.view: String
        get() {
            val cardinalityLabel = when (AspectPropertyCardinality.valueOf(cardinality)) {
                AspectPropertyCardinality.ZERO -> "0"
                AspectPropertyCardinality.INFINITY -> "∞"
                AspectPropertyCardinality.ONE -> "0:1"
            }
            return "$name ${getAspect(aspectId).name} : [$cardinalityLabel]"
        }

    private fun getAspect(aspectId: String): AspectData = try {
        aspectService.findById(aspectId).toAspectData()
    } catch (e: AspectDoesNotExist) {
        AspectData(id = aspectId, name = "'Aspect removed'")
    }
}