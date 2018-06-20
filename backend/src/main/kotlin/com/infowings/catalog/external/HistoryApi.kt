package com.infowings.catalog.external

import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.common.RefBookHistoryList
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.history.providers.RefBookHistoryProvider
import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi(
    val aspectHistoryProvider: AspectHistoryProvider, val refBookHistoryProvider: RefBookHistoryProvider,
    val subjectHistoryProvider: SubjectHistoryProvider
) {
    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList {
        val beforeMS = System.currentTimeMillis()
        val result = AspectHistoryList(aspectHistoryProvider.getAllHistory())
        val afterMS = System.currentTimeMillis()
        logger.info("all aspects history took ${afterMS - beforeMS}")        

        return result
    }

    @GetMapping("refbook")
    fun getRefBooks(): RefBookHistoryList {
        val beforeMS = System.currentTimeMillis()
        val result = RefBookHistoryList(refBookHistoryProvider.getAllHistory())
        val afterMS = System.currentTimeMillis()
        logger.info("all ref book history took ${afterMS - beforeMS}")        
        return result
    }

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
}

private val logger = loggerFor<HistoryApi>()