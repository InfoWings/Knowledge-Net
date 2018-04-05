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
    val event: EventKind?,
    val entityId: ORID,
    val entityClass: String
)

data class HistoryFact(val event: HistoryEvent, val payload: DiffPayload, val subject: HistoryAware)


fun <T> asStringOrEmpty(v: T?) = v?.toString().orEmpty()


fun diffSnapshots(base: Snapshot, other: Snapshot): DiffPayload {
    // предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе
    // представить. Без явной необходимости не хочется

    val updateData = other.data.filterNot {
        base.data[it.key] == it.value
    }

    val addedLinks = other.links.mapValues {
        it.value - (base.links.getOrElse(it.key, { emptyList() }))
    }.filterValues { !it.isEmpty() }

    val removedLinks = base.links.mapValues {
        it.value - (other.links.getOrElse(it.key, { emptyList() }))
    }.filterValues { !it.isEmpty() }

    return DiffPayload(updateData, addedLinks = addedLinks, removedLinks = removedLinks)
}

fun toHistoryFact(event: HistoryEvent, subject: HistoryAware, base: Snapshot, other: Snapshot) =
    HistoryFact(event, diffSnapshots(base, other), subject)
