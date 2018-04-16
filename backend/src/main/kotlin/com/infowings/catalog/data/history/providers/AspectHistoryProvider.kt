package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.ASPECT_CLASS

class AspectHistoryProvider(private val aspectHistoryService: HistoryService) {

    fun getAllHistory(): List<AspectHistory> {
        val aspectEventList = aspectHistoryService.getAll()
            .filter { it.event.entityClass == ASPECT_CLASS }
            .sortedByDescending { it.event.timestamp }

        return aspectEventList
            .mapIndexed { index, historyFactDto ->
                Pair(historyFactDto, restoreAspectData(aspectEventList.subList(index + 1, aspectEventList.size)))
            }
            .map { (fact, restoredData) ->
                val diff = fact.payload.data.mapNotNull { (fieldName, updatedValue) ->
                    when (fieldName) {
                        "measure" -> createAspectFieldDelta(AspectField.MEASURE, restoredData.measure, updatedValue)
                        "baseType" -> createAspectFieldDelta(AspectField.BASE_TYPE, restoredData.baseType, updatedValue)
                        "name" -> createAspectFieldDelta(AspectField.NAME, restoredData.name, updatedValue)
                        else -> null
                    }
                }
                createHistoryElement(fact.event.user, fact.event.type, diff, restoredData, emptyList())
            }
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
    username: String,
    eventType: EventType,
    changes: List<Delta>,
    data: AspectData,
    related: List<AspectData>
) =
    AspectHistory(
        username,
        eventType,
        ASPECT_CLASS,
        data.name,
        data.deleted,
        System.currentTimeMillis(),
        data.version,
        AspectDataView(data, related),
        changes
    )

private fun createAspectFieldDelta(field: AspectField, before: String?, after: String?) =
    Delta(field.name, before, after)