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
                        createHistoryElement(fact.event, diff, restoredData, emptyList())
                    }
            }
            .sortedByDescending { it.eventType }
    }

    // Know, not effective. Temporary solution
    private fun restoreAspectData(beforeEvents: List<HistoryFactDto>): AspectData {
        var result = emptyAspectData
        beforeEvents.reversed().forEach { fact ->
            result = when (fact.event.type) {
                EventType.CREATE, EventType.UPDATE -> result.copy(
                    measure = fact.payload.data.getOrDefault("measure", result.measure),
                    baseType = fact.payload.data.getOrDefault("baseType", result.baseType),
                    name = fact.payload.data.getOrDefault("name", result.name),
                    version = fact.event.version
                )
                else -> result.copy(deleted = true, version = fact.event.version)
            }
        }
        return result
    }
}

private fun createHistoryElement(
    event: HistoryEvent,
    changes: List<Delta>,
    data: AspectData,
    related: List<AspectData>
) =
    AspectHistory(
        event.user,
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