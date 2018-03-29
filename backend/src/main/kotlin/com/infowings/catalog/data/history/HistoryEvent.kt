package com.infowings.catalog.data.history

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID

data class Payload(
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

data class HistoryFact(val event: HistoryEvent, val payload: Payload)

fun <T, S> toPayload(entity: T, extractors: List<Pair<String, (T) -> S>>): Map<String, S> =
    extractors.map { it.first to it.second(entity) }.toMap()

private fun <T> asStringOrEmpty(v: T?) = v?.toString().orEmpty()

private val aspectExtractors = listOf<Pair<String, (AspectData) -> String>>(
    Pair("measure", { v -> asStringOrEmpty(v.measure) }),
    Pair("baseType", { v -> asStringOrEmpty(v.baseType) })
)

private val aspectLinksExtractors = listOf<Pair<String, (AspectVertex) -> List<ORID>>>(
    Pair("properties", { v -> v.properties.map { it.identity } })
)

private fun AspectVertex.toCreatePayload() = Payload(
    toPayload(this.toAspectData(), aspectExtractors),
    addedLinks = toPayload(this, aspectLinksExtractors), removedLinks = emptyMap()
)

private fun AspectVertex.toRemovePayload() = Payload(
    toPayload(this.toAspectData(), aspectExtractors),
    removedLinks = toPayload(this, aspectLinksExtractors), addedLinks = emptyMap()
)

fun AspectVertex.toSnapshot() = Snapshot(
    toPayload(this.toAspectData(), aspectExtractors),
    toPayload(this, aspectLinksExtractors)
)

private fun AspectVertex.toUpdatePayload(previous: Snapshot): Payload {
    val currentData = toPayload(this.toAspectData(), aspectExtractors)

    // предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе
    // представить. Без явной необходимости не хочется

    val updateData = currentData.filterNot {
        previous.data.containsKey(it.key) && previous.data[it.key] == it.value
    }

    val currentLinksData = toPayload(this, aspectLinksExtractors)

    val addedLinks = currentLinksData.mapValues {
        it.value.toSet().minus(previous.links.getOrElse(it.key, { emptyList() })).toList()
    }

    val removedLinks = previous.links.mapValues {
        it.value.toSet().minus(currentLinksData.getOrElse(it.key, { emptyList() })).toList()
    }

    return Payload(updateData, addedLinks = addedLinks, removedLinks = removedLinks)
}

private fun AspectVertex.toHistoryEvent(user: String, event: EventKind): HistoryEvent =
    HistoryEvent(
        user = user, timestamp = System.currentTimeMillis(), version = version, event = event,
        entityId = identity, entityClass = ASPECT_CLASS
    )

fun AspectVertex.toCreateFact(user: String) =
    HistoryFact(toHistoryEvent(user, EventKind.CREATE), this.toCreatePayload())

fun AspectVertex.toDeleteFact(user: String) =
    HistoryFact(toHistoryEvent(user, EventKind.DELETE), this.toRemovePayload())

fun AspectVertex.toSoftDeleteFact(user: String) =
    HistoryFact(toHistoryEvent(user, EventKind.SOFT_DELETE), this.toRemovePayload())

fun AspectVertex.toUpdateFact(user: String, previous: Snapshot) =
    HistoryFact(toHistoryEvent(user, EventKind.UPDATE), toUpdatePayload(previous))
