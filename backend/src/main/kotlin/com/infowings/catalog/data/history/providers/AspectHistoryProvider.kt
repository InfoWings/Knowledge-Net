package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHistory
import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.SUBJECT_CLASS
import java.util.concurrent.CopyOnWriteArrayList

private val logger = loggerFor<AspectHistoryProvider>()

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val aspectConstructor: AspectConstructor
) {

    fun getAllHistory(): List<AspectHistory> {
        val aspectFactsByEntity = historyService.allTimeline(ASPECT_CLASS).groupBy { it.event.entityId }

        logger.info("found aspect event groups: ${aspectFactsByEntity.size}")

        val propertyFacts = historyService.allTimeline(ASPECT_PROPERTY_CLASS)
        val propertyFactsBySession = propertyFacts.groupBy { it.event.sessionId }

        val propertySnapshots = propertyFacts.map { it.event.entityId to MutableSnapshot() }.toMap()

        val events = logTime(logger, "processing aspect event groups") {
            aspectFactsByEntity.values.flatMap {  entityFacts ->

                val snapshot = MutableSnapshot()

                var aspectDataAccumulator = AspectData(name = "")

                entityFacts.forEach {
                    snapshot.apply(it.payload)
                    val propertyFacts = propertyFactsBySession[it.event.sessionId]
                    propertyFacts?.forEach {
                        propertySnapshots[it.event.entityId]?.apply(it.payload)
                    }
                }

                val versionList: List<AspectData> = logTime(logger, "reconstruct aspect versions") {
                    listOf(aspectDataAccumulator).plus(entityFacts.map { fact ->

                        val relatedFacts = propertyFactsBySession[fact.event.sessionId] ?: emptyList()

                        aspectDataAccumulator = aspectConstructor.toNextVersion(aspectDataAccumulator, fact, relatedFacts)

                        return@map aspectDataAccumulator
                    })
                }

                logger.info("snapshot: ${snapshot.toSnapshot()}")
                logger.info("latest: ${versionList.lastOrNull()}")

                return@flatMap logTime(logger, "aspect diffs creation for aspect ${entityFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(entityFacts)
                        .map { aspectDeltaConstructor.createDiff(it.first.first, it.first.second, it.second) }
                }
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}