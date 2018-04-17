package com.infowings.catalog.data.history

import com.infowings.catalog.common.EventType
import com.orientechnologies.orient.core.id.ORID

data class DiffPayload(
    val data: Map<String, String>,
    val addedLinks: Map<String, List<ORID>>,
    val removedLinks: Map<String, List<ORID>>
)

data class Snapshot(
    val data: Map<String, String>,
    val links: Map<String, List<ORID>>
)

data class HistoryEvent(
    val user: String,
    val timestamp: Long,
    val version: Int,
    val type: EventType,
    val entityId: ORID,
    val entityClass: String
)

data class HistoryFact(
    val event: HistoryEvent,
    val payload: DiffPayload,
    val subject: HistoryAware
)

fun toHistoryFact(event: HistoryEvent, subject: HistoryAware, base: Snapshot, other: Snapshot) =
    HistoryFact(event, diffSnapshots(base, other), subject)

fun diffSnapshots(base: Snapshot, other: Snapshot): DiffPayload {
    // Предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе представить.
    // Без явной необходимости не хочется
    val updatedData = other.data
        .filter {
            it.value != base.data[it.key]
        }

    val addedLinks = other.links
        .mapValues {
            it.value - (base.links.getOrElse(it.key, { emptyList() }))
        }
        .filterValues {
            it.isNotEmpty()
        }

    val removedLinks = base.links
        .mapValues {
            it.value - (other.links.getOrElse(it.key, { emptyList() }))
        }
        .filterValues {
            it.isNotEmpty()
        }

    return DiffPayload(updatedData, addedLinks = addedLinks, removedLinks = removedLinks)
}

fun <T> asStringOrEmpty(v: T?) = v?.toString().orEmpty()
