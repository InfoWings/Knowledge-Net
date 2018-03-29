package com.infowings.catalog.data.history

import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS


object AspectPropertySnaphoter : Snapshoter<AspectPropertyVertex> {
    override fun entityClass(): String = ASPECT_PROPERTY_CLASS

    override fun dataExtractors() = listOf<Pair<String, (AspectPropertyVertex) -> String>>(
        Pair("name", { v -> asStringOrEmpty(v.name) }),
        Pair("aspect", { v -> asStringOrEmpty(v.aspect) }),
        Pair("cardinality", { v -> asStringOrEmpty(v.cardinality) })
    )
}

fun AspectPropertyVertex.toSnapshot() = AspectPropertySnaphoter.snapshot(this)

fun AspectPropertyVertex.toCreateFact(user: String) = AspectPropertySnaphoter.toCreateFact(user, this)

fun AspectPropertyVertex.toDeleteFact(user: String) = AspectPropertySnaphoter.toDeleteFact(user, this)

fun AspectPropertyVertex.toSoftDeleteFact(user: String) = AspectPropertySnaphoter.toSoftDeleteFact(user, this)

fun AspectPropertyVertex.toUpdateFact(user: String, previous: Snapshot) =
    AspectPropertySnaphoter.toUpdateFact(user, this, previous)
