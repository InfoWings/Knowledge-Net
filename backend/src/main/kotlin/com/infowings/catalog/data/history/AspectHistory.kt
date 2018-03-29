package com.infowings.catalog.data.history

import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID


object AspectSnaphoter : Snapshoter<AspectVertex> {
    override fun dataExtractors() = listOf<Pair<String, (AspectVertex) -> String>>(
        Pair("measure", { v -> asStringOrEmpty(v.measure) }),
        Pair("baseType", { v -> asStringOrEmpty(v.baseType) })
    )

    override fun linksExtractors() = listOf<Pair<String, (AspectVertex) -> List<ORID>>>(
        Pair("properties", { v -> v.properties.map { it.identity } })
    )
}

fun AspectVertex.toSnapshot() = AspectSnaphoter.snapshot(this)

fun AspectVertex.toHistoryEvent(user: String, event: EventKind): HistoryEvent =
    HistoryEvent(
        user = user, timestamp = System.currentTimeMillis(), version = version, event = event,
        entityId = identity, entityClass = ASPECT_CLASS
    )

fun AspectVertex.toCreateFact(user: String) =
    toHistoryFact(
        toHistoryEvent(user, EventKind.CREATE), AspectSnaphoter.emptySnapshot(),
        AspectSnaphoter.snapshot(this)
    )

fun AspectVertex.toDeleteFact(user: String) =
    toHistoryFact(
        toHistoryEvent(user, EventKind.DELETE), AspectSnaphoter.emptySnapshot(),
        AspectSnaphoter.snapshot(this)
    )

fun AspectVertex.toSoftDeleteFact(user: String) =
    HistoryFact(
        toHistoryEvent(user, EventKind.SOFT_DELETE),
        diffShapshots(AspectSnaphoter.emptySnapshot(), AspectSnaphoter.snapshot(this))
    )

fun AspectVertex.toUpdateFact(user: String, previous: Snapshot) =
    HistoryFact(
        toHistoryEvent(user, EventKind.SOFT_DELETE),
        diffShapshots(previous, AspectSnaphoter.snapshot(this))
    )