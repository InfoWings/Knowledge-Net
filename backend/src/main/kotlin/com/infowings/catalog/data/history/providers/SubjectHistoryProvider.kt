package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.diffSnapshots
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.SUBJECT_CLASS
import kotlin.math.sign

private val logger = loggerFor<SubjectHistoryProvider>()

data class SubjectHistoryStep(val snapshot: Snapshot, val event: HistoryEventData)


class SubjectHistoryProvider(
    private val historyService: HistoryService
) {
    private val cache: HistoryProviderCache<SubjectHistoryStep> = CHMHistoryProviderCache()

    fun getAllHistory(): List<HistorySnapshot> {
        try {
            val facts = historyService.allTimeline(SUBJECT_CLASS)
            val factsBySubject = facts.groupBy { it.event.entityId }

            logger.info("found ${facts.size} history events about ${factsBySubject.size} subjects")

            val snapshots = logTime(logger, "restore subject snapshots") {
                factsBySubject.flatMap { (id, entityFacts) ->

                    val cachedSteps = cache.get(id)

                    logger.trace("cached steps for id $id: $cachedSteps")
                    val head = SubjectHistoryStep(Snapshot(), HistoryEventData.empty)


                    val tail = if (cachedSteps?.lastOrNull()?.event == entityFacts.last().event) {
                        cachedSteps
                    } else {
                        var accumulator = head.snapshot
                        val current = accumulator.toMutable()

                        val tailNew = entityFacts.map { fact ->
                            val payload = fact.payload
                            current.apply(payload)
                            accumulator = current.immutable()
                            return@map SubjectHistoryStep(accumulator, fact.event)
                        }

                        cache.set(id, tailNew)

                        tailNew
                    }

                    val versionList = listOf(head).plus(tail)

                    return@flatMap versionList.zipWithNext()
                        .map { (before, after) ->
                            HistorySnapshot(after.event, before.snapshot, after.snapshot, diffSnapshots(before.snapshot, after.snapshot))
                        }
                }
            }

            return snapshots.sortedWith(SnapshotComparatorDesc)
        } catch (e: Exception) {
            logger.error("Caught exception during subject history collection: $e, ${e.stackTrace.toList()}")
            return emptyList()
        }
    }
}

object SnapshotComparatorDesc : Comparator<HistorySnapshot> {
    override fun compare(v1: HistorySnapshot, v2: HistorySnapshot): Int = if (v1.event.timestamp == v2.event.timestamp)
        (v2.event.version - v1.event.version).sign
    else
        (v2.event.timestamp - v1.event.timestamp).sign
}