package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.data.history.*
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.SUBJECT_CLASS
import java.util.concurrent.ConcurrentHashMap

private val logger = loggerFor<SubjectHistoryProvider>()

data class SubjectHistoryStep(val snapshot: Snapshot, val fact: HistoryFact)


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
                val head = SubjectHistoryStep(Snapshot(), HistoryFact.empty)

                var accumulator = head.snapshot
                val current = accumulator.toMutable()

                val tail = entityFacts.map { fact ->
                    val payload = fact.payload
                    current.apply(payload)
                    accumulator = current.toSnapshot()
                    return@map SubjectHistoryStep(accumulator, fact)
                }

                val versionList = listOf(head).plus(tail)

                cache.putIfAbsent(id, tail)

                return@flatMap versionList.zipWithNext()
                    .map { (before, after) ->
                        val event = after.fact.event
                        val snapshotBefore = before.snapshot
                        val snapshotAfter = after.snapshot
                        val payload = after.fact.payload
                        logger.info("same payloads: ${payload == diffSnapshots(before.snapshot, after.snapshot)}")
                        HistorySnapshot(event, snapshotBefore, snapshotAfter, payload)
                    }
            }
        }

        return snapshots.sortedByDescending { it.event.timestamp }
    }
}