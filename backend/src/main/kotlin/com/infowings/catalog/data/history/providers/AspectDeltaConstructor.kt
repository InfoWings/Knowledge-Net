package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryEvent
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.storage.ASPECT_CLASS

class AspectDeltaConstructor(val aspectService: AspectService) {

    fun createDiff(before: AspectData, after: AspectData, mainFact: HistoryFactDto): AspectHistory {

        var diffs = mutableListOf<Delta>()

        diffs.addAll(fieldsDiff(mainFact, before, after))

        if (before.subject != after.subject) {
            diffs.add(
                createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.SUBJECT,
                    before.subject?.name,
                    after.subject?.name
                )
            )
        }

        if (before.refBookName != after.refBookName) {
            diffs.add(
                createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.REFERENCE_BOOK,
                    before.refBookName,
                    after.refBookName
                )
            )
        }

        diffs.addAll(propertiesDiff(before, after))

        diffs = diffs.filter { it.after != it.before }.toMutableList()

        return createHistoryElement(mainFact.event, diffs, after, after.properties.map { getAspect(it.aspectId) })
    }

    private fun fieldsDiff(mainFact: HistoryFactDto, before: AspectData, after: AspectData): List<Delta> =
        mainFact.payload.data.mapNotNull { (fieldName, _) ->

            when (AspectField.valueOf(fieldName)) {
                AspectField.MEASURE -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.MEASURE.view,
                    before.measure,
                    after.measure
                )
                AspectField.BASE_TYPE -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.BASE_TYPE.view,
                    before.baseType,
                    after.baseType
                )
                AspectField.NAME -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.NAME.view,
                    before.name,
                    after.name
                )
                AspectField.DESCRIPTION -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.NAME.view,
                    before.name,
                    after.name
                )
            }
        }

    private fun propertiesDiff(before: AspectData, after: AspectData): List<Delta> {

        val diffs = mutableListOf<Delta>()

        val beforePropertyIdSet = before.properties.map { it.id }.toSet()
        val afterPropertyIdSet = after.properties.map { it.id }.toSet()

        diffs.addAll(beforePropertyIdSet.intersect(afterPropertyIdSet).map { id ->
            val afterProperty = after.properties.find { it.id == id }
            createAspectFieldDelta(
                EventType.UPDATE,
                "${AspectField.PROPERTY} ${afterProperty?.name ?: ""}",
                before.properties.find { it.id == id }?.toView(),
                afterProperty?.toView()
            )
        })

        diffs.addAll(beforePropertyIdSet.subtract(afterPropertyIdSet).map { id ->
            val beforeProperty = before.properties.find { it.id == id }
            createAspectFieldDelta(
                EventType.DELETE,
                "${AspectField.PROPERTY} ${beforeProperty?.name ?: ""}",
                before.properties.find { it.id == id }?.toView(),
                null
            )
        })

        diffs.addAll(afterPropertyIdSet.subtract(beforePropertyIdSet).map { id ->
            val afterProperty = after.properties.find { it.id == id }
            createAspectFieldDelta(
                EventType.CREATE,
                "${AspectField.PROPERTY} ${afterProperty?.name ?: ""}",
                null,
                after.properties.find { it.id == id }?.toView()
            )
        })

        return diffs
    }


    private fun createHistoryElement(
        event: HistoryEvent,
        changes: List<Delta>,
        data: AspectData,
        related: List<AspectData>
    ) =
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

    private fun AspectPropertyData.toView(): String {

        val cardinalityLabel = when (PropertyCardinality.valueOf(cardinality)) {
            PropertyCardinality.ZERO -> "0"
            PropertyCardinality.INFINITY -> "∞"
            PropertyCardinality.ONE -> "0:1"
        }
        return "$name ${getAspect(aspectId).name} : [$cardinalityLabel]"
    }

    private fun getAspect(aspectId: String): AspectData = try {
        aspectService.findById(aspectId).toAspectData()
    } catch (e: AspectDoesNotExist) {
        AspectData(id = aspectId, name = "'Aspect removed'")
    }
}