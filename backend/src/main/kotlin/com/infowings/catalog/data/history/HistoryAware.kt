package com.infowings.catalog.data.history

import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

enum class EventKind {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

interface HistoryAware: OVertex {
    val entityClass: String

    fun currentSnapshot(): Snapshot

    fun emptySnapshot(): Snapshot {
        val base = currentSnapshot()

        val data = base.data.mapValues { "" }
        val links = base.links.mapValues { emptyList<ORID>() }

        return Snapshot(data, links)
    }

    private fun historyEvent(user: String, event: EventKind): HistoryEvent =
        HistoryEvent(
            user = user, timestamp = System.currentTimeMillis(), version = version,
            event = event, entityId = identity, entityClass = entityClass
        )

    private fun toFact(user: String, event: EventKind, before: Snapshot) =
        toHistoryFact(historyEvent(user, event), before, currentSnapshot())

    fun toCreateFact(user: String) = toFact(user, EventKind.CREATE, emptySnapshot())

    fun toDeleteFact(user: String) = toFact(user, EventKind.DELETE, emptySnapshot())

    fun toSoftDeleteFact(user: String) = toFact(user, EventKind.SOFT_DELETE, emptySnapshot())

    fun toUpdateFact(user: String, previous: Snapshot) = toFact(user, EventKind.UPDATE, previous)
}