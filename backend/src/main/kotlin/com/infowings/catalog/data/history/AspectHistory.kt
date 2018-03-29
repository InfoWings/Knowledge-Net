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

private fun AspectVertex.noneEvent(user: String): HistoryEvent =
    HistoryEvent(
        user = user, timestamp = System.currentTimeMillis(), version = version,
        event = null, entityId = identity, entityClass = ASPECT_CLASS
    )

fun AspectVertex.toCreateFact(user: String) = AspectSnaphoter.toCreateFact(noneEvent(user), this)

fun AspectVertex.toDeleteFact(user: String) = AspectSnaphoter.toDeleteFact(noneEvent(user), this)

fun AspectVertex.toSoftDeleteFact(user: String) = AspectSnaphoter.toSoftDeleteFact(noneEvent(user), this)

fun AspectVertex.toUpdateFact(user: String, previous: Snapshot) =
    AspectSnaphoter.toUpdateFact(noneEvent(user), this, previous)
