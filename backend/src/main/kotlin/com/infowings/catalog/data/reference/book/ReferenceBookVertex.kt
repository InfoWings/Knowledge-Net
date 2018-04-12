package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookVertex() = ReferenceBookVertex(this)

class ReferenceBookVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_VERTEX

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf("name" to asStringOrEmpty(name)),
        links = mapOf("aspect" to listOf(aspect.identity))
    )

    var name: String
        get() = this["name"]
        set(value) {
            this["name"] = value
        }

    var deleted: Boolean
        get() = this["deleted"] ?: false
        set(value) {
            this["deleted"] = value
        }

    val root: ReferenceBookItemVertex
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE)
            .map { it.toReferenceBookItemVertex() }
            .first()

    private val aspect: AspectVertex
        get() = getVertices(ODirection.IN, ASPECT_REFERENCE_BOOK_EDGE).first().toAspectVertex()

    fun toReferenceBook(): ReferenceBook {
        val root = root.toReferenceBookItem()
        return ReferenceBook(aspect.id, name, root, deleted, version)
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}