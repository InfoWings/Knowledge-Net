package com.infowings.catalog.data.history

import com.infowings.catalog.data.REFERENCE_BOOK_VERTEX
import com.infowings.catalog.data.ReferenceBookVertex
import com.orientechnologies.orient.core.id.ORID

object ReferenceBookSnaphoter : Snapshoter<ReferenceBookVertex> {
    override fun entityClass(): String = REFERENCE_BOOK_VERTEX

    override fun dataExtractors() = listOf<Pair<String, (ReferenceBookVertex) -> String>>(
        Pair("name", { v -> asStringOrEmpty(v.name) })
    )

    override fun linksExtractors() = listOf<Pair<String, (ReferenceBookVertex) -> List<ORID>>>(
        Pair("aspect", { v -> listOf(v.aspectOVertex().identity) })
    )
}

fun ReferenceBookVertex.toSnapshot() = ReferenceBookSnaphoter.snapshot(this)

fun ReferenceBookVertex.toCreateFact(user: String) = ReferenceBookSnaphoter.toCreateFact(user, this)

fun ReferenceBookVertex.toDeleteFact(user: String) = ReferenceBookSnaphoter.toDeleteFact(user, this)

fun ReferenceBookVertex.toSoftDeleteFact(user: String) = ReferenceBookSnaphoter.toSoftDeleteFact(user, this)

fun ReferenceBookVertex.toUpdateFact(user: String, previous: Snapshot) =
    ReferenceBookSnaphoter.toUpdateFact(user, this, previous)