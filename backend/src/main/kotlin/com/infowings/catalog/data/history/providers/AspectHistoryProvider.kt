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

        return aspectEventList.mapIndexed { index, historyFactDto ->
            Pair(
                historyFactDto, restore(
                    aspectEventList.subList(
                        index,
                        aspectEventList.size
                    )
                )
            )
        }.map { (fact, data) ->
            val diff = fact.payload.data.mapNotNull { (fieldName, value) ->
                when (fieldName) {
                    "measure" -> createAspectFieldDelta(AspectField.MEASURE, value, data.measure)
                    "baseType" -> createAspectFieldDelta(AspectField.BASE_TYPE, value, data.baseType)
                    "name" -> createAspectFieldDelta(AspectField.NAME, value, data.name)
                    else -> null
                }
            }
            createHistElement(fact.event.user, fact.event.event!!, diff, data, emptyList())
        }

    }

    // Know, not effective. Temporary solution
    private fun restore(beforeEvents: List<HistoryFactDto>): AspectData {
        var result = emptyAspectData
        beforeEvents.reversed().forEach { fact ->
            result = when (fact.event.event) {
                EventKind.CREATE, EventKind.UPDATE -> result.copy(
                    measure = fact.payload.data.getOrDefault("measure", result.measure),
                    baseType = fact.payload.data.getOrDefault("baseType", result.baseType),
                    name = fact.payload.data.getOrDefault("name", result.name)
                )
                else -> result.copy(deleted = true, version = fact.event.version)
            }
        }
        return result
    }
}

private fun createHistElement(
    username: String,
    eventKind: com.infowings.catalog.common.EventKind,
    changes: List<Delta>,
    data: AspectData,
    related: List<AspectData>
) =
    AspectHistory(
        username,
        eventKind,
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