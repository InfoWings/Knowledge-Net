package com.infowings.catalog.data.history

import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID


object AspectSnaphoter : Snapshoter<AspectVertex> {
    override fun entityClass(): String = ASPECT_CLASS

    override fun dataExtractors() = listOf<Pair<String, (AspectVertex) -> String>>(
        Pair("name", { v -> asStringOrEmpty(v.name) }),
        Pair("measure", { v -> asStringOrEmpty(v.measure) }),
        Pair("baseType", { v -> asStringOrEmpty(v.baseType) })
    )

    override fun linksExtractors() = listOf<Pair<String, (AspectVertex) -> List<ORID>>>(
        Pair("properties", { v -> v.properties.map { it.identity } })
    )
}

fun AspectVertex.toSnapshot() = AspectSnaphoter.snapshot(this)

fun AspectVertex.toCreateFact(user: String) = AspectSnaphoter.toCreateFact(user, this)

fun AspectVertex.toDeleteFact(user: String) = AspectSnaphoter.toDeleteFact(user, this)

fun AspectVertex.toSoftDeleteFact(user: String) = AspectSnaphoter.toSoftDeleteFact(user, this)

fun AspectVertex.toUpdateFact(user: String, previous: Snapshot) = AspectSnaphoter.toUpdateFact(user, this, previous)
