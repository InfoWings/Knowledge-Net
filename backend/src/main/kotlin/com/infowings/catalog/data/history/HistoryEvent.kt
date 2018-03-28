package com.infowings.catalog.data.history

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID

data class Payload (
    val data: Map<String, String>
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

fun <T>toPayload(entity: T, asString: List<Pair<String, (T) -> String>>) = Payload(
        asString.map {
            it.first to it.second(entity)
        }.toMap())

private fun <T>asStringOrEmpty(v: T?) = v?.toString().orEmpty()

private val aspectExtractors = listOf<Pair<String, (AspectData) -> String>>(
        Pair("measure", {v -> asStringOrEmpty(v.measure)}),
        Pair("baseType", {v -> asStringOrEmpty(v.baseType)})
)

fun AspectData.toPayload() = toPayload(this, aspectExtractors)

private fun AspectVertex.toHistoryEvent(user: String, event: EventKind): HistoryEvent =
        HistoryEvent(user = user, timestamp = System.currentTimeMillis(), version = version, event = event,
                entityId = identity, entityClass = ASPECT_CLASS)

fun AspectVertex.toCreateFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.CREATE), this.toAspectData().toPayload())

fun AspectVertex.toDeleteFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.DELETE), this.toAspectData().toPayload())

fun AspectVertex.toSoftDeleteFact(user: String) =
        HistoryFact(toHistoryEvent(user, EventKind.SOFT_DELETE), this.toAspectData().toPayload())

fun AspectVertex.toUpdateFact(user: String, previous: AspectData): HistoryFact {
    val currentData = toAspectData().toPayload().data
    val previousData = previous.toPayload().data

    // предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе
    // представить. Без явной необходимости не хочется

    val updateData = currentData.filterNot {
        previousData.containsKey(it.key) && previousData[it.key] == it.value
    }

    return HistoryFact(toHistoryEvent(user, EventKind.UPDATE), Payload(updateData))
}