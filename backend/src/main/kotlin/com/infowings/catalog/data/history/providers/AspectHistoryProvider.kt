package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistoryEvent
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.ASPECT_CLASS

class AspectHistoryProvider(private val aspectHistoryService: HistoryService) {

    fun getAllHistory(): List<AspectHistory> {
        val aspectEventGroups = aspectHistoryService.getAll()
            .filter { it.event.entityClass == ASPECT_CLASS }
            .sortedByDescending { it.event.timestamp }
            .groupBy { it.event.entityId }

        return aspectEventGroups
            .values
            .flatMap { groupEvents ->
                groupEvents
                    .mapIndexed { index, historyFactDto ->
                        Pair(
                            historyFactDto,
                            restoreAspectData(groupEvents.subList(index + 1, groupEvents.size))
                        )
                    }
                    .map { (fact, restoredData) ->
                        val diff = fact.payload.data.mapNotNull { (fieldName, updatedValue) ->
                            when (fieldName) {
                                "measure" -> createAspectFieldDelta(
                                    AspectField.MEASURE,
                                    restoredData.measure,
                                    updatedValue
                                )
                                "baseType" -> createAspectFieldDelta(
                                    AspectField.BASE_TYPE,
                                    restoredData.baseType,
                                    updatedValue
                                )
                                "name" -> createAspectFieldDelta(AspectField.NAME, restoredData.name, updatedValue)
                                else -> null
                            }
                        }
                        createHistoryElement(fact.event, diff, restoredData.submitEvent(fact), emptyList())
                    }
            }
            .sortedByDescending { it.timestamp }
    }

    // Know, not effective. Temporary solution
    private fun restoreAspectData(beforeEvents: List<HistoryFactDto>): AspectData =
        beforeEvents.reversed().foldRight(emptyAspectData) { fact, result -> result.submitEvent(fact) }

    private fun AspectData.submitEvent(fact: HistoryFactDto): AspectData {
        return when (fact.event.type) {
            EventType.CREATE, EventType.UPDATE -> copy(
                measure = fact.payload.data.getOrDefault("measure", measure),
                baseType = fact.payload.data.getOrDefault("baseType", baseType),
                name = fact.payload.data.getOrDefault("name", name),
                version = fact.event.version
            )
            else -> copy(deleted = true, version = fact.event.version)
        }
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

    private fun createAspectFieldDelta(field: AspectField, before: String?, after: String?) =
        Delta(field.name, before, after)
}