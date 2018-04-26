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
            .groupBy { it.event.entityId }

        return aspectEventGroups.values.flatMap { entityEvents ->
            val entityChanges = entityEvents.sortedBy { it.event.version }

            val versionList = mutableListOf<AspectData>()
            var tmpData = emptyAspectData
            for (fact in entityChanges) {
                versionList.add(tmpData)
                tmpData = tmpData.submitEvent(fact)
            }

            return@flatMap entityChanges.zip(versionList).map { (fact, version) -> version.createDiff(fact) }

        }.sortedByDescending { it.timestamp }
    }

    private fun AspectData.createDiff(fact: HistoryFactDto): AspectHistory {

        val diff = fact.payload.data.mapNotNull { (fieldName, updatedValue) ->
            when (AspectField.valueOf(fieldName)) {
                AspectField.MEASURE -> createAspectFieldDelta(
                    fact.event.type,
                    AspectField.MEASURE,
                    measure,
                    updatedValue
                )
                AspectField.BASE_TYPE -> createAspectFieldDelta(
                    fact.event.type,
                    AspectField.BASE_TYPE,
                    baseType,
                    updatedValue
                )
                AspectField.NAME -> createAspectFieldDelta(
                    fact.event.type,
                    AspectField.NAME,
                    name,
                    updatedValue
                )
                else -> null
            }
        }
        return createHistoryElement(fact.event, diff, submitEvent(fact), emptyList())
    }

    private fun AspectData.submitEvent(fact: HistoryFactDto): AspectData {
        return when (fact.event.type) {
            EventType.CREATE, EventType.UPDATE -> copy(
                measure = fact.payload.data.getOrDefault(AspectField.MEASURE.name, measure),
                baseType = fact.payload.data.getOrDefault(AspectField.BASE_TYPE.name, baseType),
                name = fact.payload.data.getOrDefault(AspectField.NAME.name, name),
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

    private fun createAspectFieldDelta(event: EventType, field: AspectField, before: String?, after: String?): Delta =
        when (event) {
            EventType.CREATE, EventType.UPDATE -> Delta(field.name, before, after)
            EventType.DELETE, EventType.SOFT_DELETE -> Delta(field.name, before, null)
        }
}