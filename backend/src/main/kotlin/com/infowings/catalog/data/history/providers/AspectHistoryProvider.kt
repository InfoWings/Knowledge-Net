package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHistory
import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS

private val logger = loggerFor<AspectHistoryProvider>()

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val aspectConstructor: AspectConstructor
) {

    fun getAllHistory(): List<AspectHistory> {

        val allHistory = historyService.getAll()

        val aspectEventGroups: Map<String, List<HistoryFact>> = logTime(logger, "grouping aspect event") {allHistory.idEventMap(classname = ASPECT_CLASS) }
        val sessionAspectPropertyMap = logTime(logger, "grouping aspect properties") {
            allHistory.filter { it.event.entityClass == ASPECT_PROPERTY_CLASS }
            .groupBy { it.event.sessionId }
        }

        logger.info("found aspect event groups: ${aspectEventGroups.size}")

        val events = logTime(logger, "processing action event groups") {
            aspectEventGroups.values.flatMap { entityEvents ->

                var aspectDataAccumulator = AspectData()

                val versionList = listOf(aspectDataAccumulator).plus(entityEvents.map { fact ->

                    val relatedFacts = sessionAspectPropertyMap[fact.event.sessionId] ?: emptyList()

                    aspectDataAccumulator = aspectConstructor.toNextVersion(aspectDataAccumulator, fact, relatedFacts)

                    return@map aspectDataAccumulator
                })

                return@flatMap versionList.zipWithNext().zip(entityEvents)
                    .map { aspectDeltaConstructor.createDiff(it.first.first, it.first.second, it.second) }

            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}