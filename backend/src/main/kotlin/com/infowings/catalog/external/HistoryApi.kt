package com.infowings.catalog.external

import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.common.ObjectHistoryList
import com.infowings.catalog.common.RefBookHistoryList
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.history.providers.ObjectHistoryProvider
import com.infowings.catalog.data.history.providers.RefBookHistoryProvider
import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistoryDao
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import com.infowings.catalog.loggerFor
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.springframework.web.bind.annotation.*

fun <T> logTime(logger: Logger, comment :String, action: () -> T): T {
    val beforeMS = System.currentTimeMillis()
    val result = action()
    val afterMS = System.currentTimeMillis()
    logger.info("${comment} took ${afterMS - beforeMS}ms")
    return result
}

@Serializable
data class TimeStampMigrationStatus(val id: String, val code: Int)

@RestController
@RequestMapping("api/history")
class HistoryApi(
    val aspectHistoryProvider: AspectHistoryProvider,
    val refBookHistoryProvider: RefBookHistoryProvider,
    val objectHistoryProvider: ObjectHistoryProvider,
    val subjectHistoryProvider: SubjectHistoryProvider,
    val historyService: HistoryService,
    val historyDao: HistoryDao
) {
    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList {
        return logTime(logger, "all aspects history") {
            AspectHistoryList(aspectHistoryProvider.getAllHistory())
        }
    }

    @GetMapping("refbook")
    fun getRefBooks(): RefBookHistoryList {
        val beforeMS = System.currentTimeMillis()
        val result = RefBookHistoryList(refBookHistoryProvider.getAllHistory())
        val afterMS = System.currentTimeMillis()
        logger.info("all ref book history took ${afterMS - beforeMS}")        
        return result
    }

    @GetMapping("objects")
    fun getObjects() = ObjectHistoryList(objectHistoryProvider.getAllHistory())

    @GetMapping("subjects")
    fun getSubjects(): SubjectHistoryList {
        val beforeMS = System.currentTimeMillis()

        val history: List<HistorySnapshot> = subjectHistoryProvider.getAllHistory()

        val deleteVersions: Map<String, Int> = history.filter {
            it.event.type.isDelete()
        }.map {
            it.event.entityId to it.event.version
        }.toMap()

        val result = SubjectHistoryList(history.map {
            val snapshot = it.toData()
            val deletedAt = deleteVersions[snapshot.event.entityId]
            val isDeleted =
                it.event.type.isDelete() //if (deletedAt != null) deletedAt < snapshot.event.version else false

            SubjectHistory(
                event = snapshot.event,
                info = snapshot.after.data["name"],
                deleted = isDeleted,
                fullData = if (isDeleted) snapshot.before else snapshot.after,
                changes = snapshot.diff.data.map { FieldDelta(it.key, snapshot.before.data[it.key], it.value) })
        })

        val afterMS = System.currentTimeMillis()
        logger.info("all subjects history took ${afterMS - beforeMS}")        

        return result
    }

    @GetMapping("/entity/{id}")
    fun getEntityHistory(@PathVariable id: String): EntityHistory {
        return logTime(logger, "entity history") {
            val timeline = historyService.entityTimeline(id)

            return@logTime EntityHistory(id)
        }
    }

    @PostMapping("/migrate-timestamp/{eventId}")
    fun migrateTimestamp(@PathVariable eventId: String): TimeStampMigrationStatus {
        val eventVertex = historyDao.findEvent(eventId) ?: throw IllegalArgumentException("history event $eventId is not found")
        val tsDate = eventVertex.timestampDate
        if (tsDate != null) return TimeStampMigrationStatus(eventId, 1)
        else {
            val ts = eventVertex.timestamp
            eventVertex.timestamp = ts
            return TimeStampMigrationStatus(eventId, 0)
        }
    }

    @PostMapping("/migrate-timestamps/{eventIds}")
    fun migrateTimestamp(@PathVariable eventIds: List<String>): List<TimeStampMigrationStatus> {
        return eventIds.map { migrateTimestamp(it) }
    }

    @PostMapping("/migrate-all-timestamps")
    fun migrateTimestampAll(): List<TimeStampMigrationStatus> {
        return historyDao.getAllWithoutTimestampDate().map { migrateTimestamp(it) }
    }
}

private val logger = loggerFor<HistoryApi>()