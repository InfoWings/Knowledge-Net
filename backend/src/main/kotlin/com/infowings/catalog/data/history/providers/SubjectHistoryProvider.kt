package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.data.history.*
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.SUBJECT_CLASS
import java.util.concurrent.ConcurrentHashMap

private val logger = loggerFor<SubjectHistoryProvider>()

data class SubjectHistoryStep(val snapshot: Snapshot, val event: HistoryEventData)


class SubjectHistoryProvider(
    private val historyService: HistoryService
) {
    // пока такой наивный кеш. Словим OOME - переделаем
    private val cache = ConcurrentHashMap<String, List<SubjectHistoryStep>>()

    fun getAllHistory(): List<HistorySnapshot> {
        val facts = historyService.allTimeline(SUBJECT_CLASS)
        val factsBySubject = facts.groupBy { it.event.entityId }

        logger.info("found ${facts.size} history events about ${factsBySubject.size} subjects")

        val snapshots = logTime(logger, "restore subject snapshots") {
            factsBySubject.flatMap { (id, entityFacts) ->

                val cachedSteps = cache.get(id)

                logger.info("cached steps for id $id: ${cachedSteps}")
                val head = Pair(Snapshot(), DiffPayload())


                var accumulator: Pair<Snapshot, DiffPayload> = head
                val current = accumulator.first.toMutable()

                val tail = entityFacts.map { fact ->
                    val payload = fact.payload
                    current.apply(payload)
                    accumulator = Pair(current.toSnapshot(), payload)
                    return@map accumulator
                }

                val versionList = listOf(head).plus(tail)

                val events = entityFacts.map { it.event }

                cache.putIfAbsent(id, versionList.drop(1).zip(events).map { SubjectHistoryStep(it.first.first, it.second) })

                return@flatMap versionList.zipWithNext().zip(events)
                    .map {
                        val event = it.second
                        val (before, after) = it.first
                        val snapshotBefore = before.first
                        val snapshotAfter = after.first
                        val payload = after.second
                        HistorySnapshot(event, snapshotBefore, snapshotAfter, payload)
                    }
            }
        }

        return snapshots.sortedByDescending { it.event.timestamp }
    }
}