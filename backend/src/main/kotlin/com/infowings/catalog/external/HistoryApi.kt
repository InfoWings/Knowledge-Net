package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi(val aspectHistoryProvider: AspectHistoryProvider, val subjectHistoryProvider: SubjectHistoryProvider) {

    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList = AspectHistoryList(aspectHistoryProvider.getAllHistory())

    @GetMapping("subjects")
    fun getSubjects(): SubjectHistoryList {
        val history: List<HistorySnapshot> = subjectHistoryProvider.getAllHistory()

        val deleteVersions: Map<String, Int> = history.filter {
            it.event.type.isDelete()
        }.map {
            it.event.entityId to it.event.version
        }.toMap()

        return SubjectHistoryList(history.map {
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
    }

}