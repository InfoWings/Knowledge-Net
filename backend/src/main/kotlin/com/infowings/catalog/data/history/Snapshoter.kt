package com.infowings.catalog.data.history

import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

interface Snapshoter<in T: OVertex> {
    fun dataExtractors(): List<Pair<String, (T) -> String>>

    fun entityClass(): String

    fun linksExtractors() = emptyList<Pair<String, (T) -> List<ORID>>>()

    private fun <T, S> toPlainPayload(entity: T, extractors: List<Pair<String, (T) -> S>>): Map<String, S> =
        extractors.map { it.first to it.second(entity) }.toMap()

    private fun historyEvent(user: String, entity: T, event: EventKind): HistoryEvent =
        HistoryEvent(
            user = user, timestamp = System.currentTimeMillis(), version = entity.version,
            event = event, entityId = entity.identity, entityClass = entityClass()
        )

    fun snapshot(entity: T): Snapshot = Snapshot(
        toPlainPayload(entity, dataExtractors()),
        toPlainPayload(entity, linksExtractors())
    )

    fun emptySnapshot(): Snapshot = Snapshot(
        dataExtractors().map { it.first to "" }.toMap(),
        dataExtractors().map { it.first to emptyList<ORID>() }.toMap()
    )

    private fun toFact(user: String, entity: T, event: EventKind, before: Snapshot) =
        toHistoryFact(historyEvent(user, entity, event), before, snapshot(entity))

    fun toCreateFact(user: String, entity: T) = toFact(user, entity, EventKind.CREATE, emptySnapshot())

    fun toDeleteFact(user: String, entity: T) = toFact(user, entity, EventKind.DELETE, emptySnapshot())

    fun toSoftDeleteFact(user: String, entity: T) = toFact(user, entity, EventKind.SOFT_DELETE, emptySnapshot())

    fun toUpdateFact(user: String, entity: T, previous: Snapshot) = toFact(user, entity, EventKind.UPDATE, previous)
}