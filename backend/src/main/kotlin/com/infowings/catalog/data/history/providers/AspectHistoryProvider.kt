package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectPropertyCardinality
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryEvent
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS

class AspectHistoryProvider(private val aspectHistoryService: HistoryService, private val aspectService: AspectService) {

    fun getAllHistory(): List<AspectHistory> {

        val allHistory = aspectHistoryService.getAll()

        val aspectEventGroups = allHistory.filterByClassAndGroupById(ASPECT_CLASS)
        val aspectPropertyEventGroupsBySessionId = allHistory.filter { it.event.entityClass == ASPECT_PROPERTY_CLASS }
            .groupBy { it.sessionId }
            .toMap()

        return aspectEventGroups.values.flatMap { entityEvents ->

            val versionList = mutableListOf<AspectData>()
            var tmpData = emptyAspectData
            versionList.add(tmpData)
            for (fact in entityEvents) {
                val allRelated = aspectPropertyEventGroupsBySessionId[fact.sessionId] ?: emptyList()
                val newProps = fact.payload.addedLinks[AspectField.PROPERTY]?.map { emptyAspectPropertyData.copy(id = it.toString()) }
                        ?: emptyList()

                val updatedProps = tmpData.properties.plus(newProps).submit(allRelated)
                tmpData = tmpData.submit(fact).copy(properties = updatedProps)
                versionList.add(tmpData)
            }

            return@flatMap versionList.zipWithNext().zip(entityEvents).map { createDiff(it.first.first, it.first.second, it.second) }

        }.sortedByDescending { it.timestamp }
    }

    private fun createDiff(before: AspectData, after: AspectData, mainFact: HistoryFactDto): AspectHistory {
        var diffs = mutableListOf<Delta>()

        val aspectFieldDiff = mainFact.payload.data.mapNotNull { (fieldName, _) ->
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
                else -> null
            }
        }

        diffs.addAll(aspectFieldDiff)

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

        diffs = diffs.filter { it.after != it.before }.toMutableList()

        return createHistoryElement(mainFact.event, diffs, after, after.properties.map { getAspect(it.aspectId) })
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
        emptyAspectData.copy(id = aspectId, name = "'Aspect removed'")
    }
}