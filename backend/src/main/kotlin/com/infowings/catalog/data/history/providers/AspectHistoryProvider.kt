package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHistory
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS

class AspectHistoryProvider(
    private val aspectHistoryService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val aspectConstructor: AspectConstructor
) {

    fun getAllHistory(): List<AspectHistory> {

        val allHistory = aspectHistoryService.getAll()

        val aspectEventGroups = allHistory.idEventMap(classname = ASPECT_CLASS)
        val sessionAspectPropertyMap = allHistory.filter { it.event.entityClass == ASPECT_PROPERTY_CLASS }
            .groupBy { it.sessionId }
            .toMap()

        return aspectEventGroups.values.flatMap { entityEvents ->

            var aspectDataAccumulator = AspectData()

            val versionList = listOf(aspectDataAccumulator).plus(entityEvents.map { fact ->

                val relatedEvents = sessionAspectPropertyMap[fact.sessionId] ?: emptyList()

                aspectDataAccumulator = aspectConstructor.toNextVersion(aspectDataAccumulator, fact, relatedEvents)

                return@map aspectDataAccumulator
            })

            return@flatMap versionList.zipWithNext().zip(entityEvents).map { aspectDeltaConstructor.createDiff(it.first.first, it.first.second, it.second) }

        }.sortedByDescending { it.timestamp }
    }
}