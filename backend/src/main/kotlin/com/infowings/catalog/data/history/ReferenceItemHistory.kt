package com.infowings.catalog.data.history

import com.infowings.catalog.data.REFERENCE_BOOK_ITEM_VERTEX
import com.infowings.catalog.data.ReferenceBookItemVertex
import com.orientechnologies.orient.core.id.ORID

object ReferenceBookItemSnaphoter : Snapshoter<ReferenceBookItemVertex> {
    override fun entityClass(): String = REFERENCE_BOOK_ITEM_VERTEX

    override fun dataExtractors() = listOf<Pair<String, (ReferenceBookItemVertex) -> String>>(
        Pair("name", { v -> asStringOrEmpty(v.value) })
    )

    override fun linksExtractors() = listOf<Pair<String, (ReferenceBookItemVertex) -> List<ORID>>>(
        Pair("properties", { v -> v.children.map { it.identity } })
    )
}

fun ReferenceBookItemVertex.toSnapshot() = ReferenceBookItemSnaphoter.snapshot(this)

fun ReferenceBookItemVertex.toCreateFact(user: String) = ReferenceBookItemSnaphoter.toCreateFact(user, this)

fun ReferenceBookItemVertex.toDeleteFact(user: String) = ReferenceBookItemSnaphoter.toDeleteFact(user, this)

fun ReferenceBookItemVertex.toSoftDeleteFact(user: String) = ReferenceBookItemSnaphoter.toSoftDeleteFact(user, this)

fun ReferenceBookItemVertex.toUpdateFact(user: String, previous: Snapshot) =
    ReferenceBookItemSnaphoter.toUpdateFact(user, this, previous)
