package com.infowings.catalog.data.history

import com.orientechnologies.orient.core.id.ORID

data class DiffPayload(
    val data: Map<String, String>,
    val addedLinks: Map<String, List<ORID>>,
    val removedLinks: Map<String, List<ORID>>
)

data class Snapshot(val data: Map<String, String>, val links: Map<String, List<ORID>>)

data class HistoryEvent(
    val user: String,
    val timestamp: Long,
    val version: Int,
    val event: EventKind,
    val entityId: ORID,
    val entityClass: String
)

data class HistoryFact(val event: HistoryEvent, val payload: DiffPayload)


fun <T> asStringOrEmpty(v: T?) = v?.toString().orEmpty()


fun diffShapshots(before: Snapshot, after: Snapshot): DiffPayload {
    // предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе
    // представить. Без явной необходимости не хочется

    val updateData = after.data.filterNot {
        before.data.containsKey(it.key) && before.data[it.key] == it.value
    }

    val addedLinks = after.links.mapValues {
        it.value.toSet().minus(before.links.getOrElse(it.key, { emptyList() })).toList()
    }

    val removedLinks = before.links.mapValues {
        it.value.toSet().minus(after.links.getOrElse(it.key, { emptyList() })).toList()
    }

    return DiffPayload(updateData, addedLinks = addedLinks, removedLinks = removedLinks)
}

fun toHistoryFact(event: HistoryEvent, before: Snapshot, after: Snapshot) =
    HistoryFact(event, diffShapshots(before, after))
