package com.infowings.catalog.data.history

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID

data class Payload (
    val data: Map<String, String>,
    val addedLinks: Map<String, List<String>>,
    val removedLinks: Map<String, List<String>>
)

data class HistoryEvent(
    val user: String,
    val timestamp: Long,
    val version: Int,
    val event: EventKind,
    val entityId: ORID,
    val entityClass: String
)

data class HistoryFact(
        val event: HistoryEvent,
        val payload: Payload
)

fun <T, S>toPayload(entity: T, extractors: List<Pair<String, (T) -> S>>): Map<String, S> =
        extractors.map { it.first to it.second(entity) }.toMap()

private fun <T>asStringOrEmpty(v: T?) = v?.toString().orEmpty()

private val aspectExtractors = listOf<Pair<String, (AspectData) -> String>>(
        Pair("measure", {v -> asStringOrEmpty(v.measure)}),
        Pair("baseType", {v -> asStringOrEmpty(v.baseType)})
)

private val aspectLinksExtractors = listOf<Pair<String, (AspectData) -> List<String>>>(
        Pair("properties", {v -> v.properties.map {it.id}})
)

private fun AspectData.toCreatePayload() = Payload(toPayload(this, aspectExtractors),
        addedLinks = toPayload(this, aspectLinksExtractors), removedLinks = emptyMap())

private fun AspectData.toRemovePayload() = Payload(toPayload(this, aspectExtractors),
        removedLinks = toPayload(this, aspectLinksExtractors), addedLinks = emptyMap())

private fun AspectData.toUpdatePayload(previous: AspectData): Payload {
    val logger = loggerFor<AspectData>()

    val currentData = toPayload(this, aspectExtractors)
    val previousData = toPayload(previous, aspectExtractors)

    // предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе
    // представить. Без явной необходимости не хочется

    val updateData = currentData.filterNot {
        previousData.containsKey(it.key) && previousData[it.key] == it.value
    }

    logger.info("currentData: $currentData")
    logger.info("previousData: $previousData")
    logger.info("updateData: $updateData")

    val currentLinksData = toPayload(this, aspectLinksExtractors)
    val previousLinksData = toPayload(previous, aspectLinksExtractors)

    logger.info("currentLinksData: $currentLinksData")
    logger.info("previousLinksData: $previousLinksData")

    val addedLinks = currentLinksData.mapValues {
        it.value.toSet().minus(previousLinksData.getOrElse(it.key, {emptyList()})).toList()
    }

    val removedLinks = previousLinksData.mapValues {
        it.value.toSet().minus(currentLinksData.getOrElse(it.key, {emptyList()})).toList()
    }

    logger.info("addedLinks: $addedLinks")
    logger.info("removedLinks: $removedLinks")

    return Payload(updateData, addedLinks = addedLinks, removedLinks = removedLinks)}

private fun AspectVertex.toHistoryEvent(user: String, event: EventKind): HistoryEvent =
        HistoryEvent(user = user, timestamp = System.currentTimeMillis(), version = version, event = event,
                entityId = identity, entityClass = ASPECT_CLASS)

fun AspectVertex.toCreateFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.CREATE), this.toAspectData().toCreatePayload())

fun AspectVertex.toDeleteFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.DELETE), this.toAspectData().toRemovePayload())

fun AspectVertex.toSoftDeleteFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.SOFT_DELETE), this.toAspectData().toRemovePayload())

fun AspectVertex.toUpdateFact(user: String, previous: AspectData): HistoryFact {
    return HistoryFact(toHistoryEvent(user, EventKind.UPDATE), toAspectData().toUpdatePayload(previous))
}