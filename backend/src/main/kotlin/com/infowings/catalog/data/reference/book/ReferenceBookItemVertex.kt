package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookItemVertex() = ReferenceBookItemVertex(this)


class ReferenceBookItemVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_ITEM_VERTEX

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf("value" to asStringOrEmpty(value)),
        links = mapOf("children" to children.map { it.identity })
    )

    var aspectId: String
        get() = this["aspectId"]
        set(value) {
            this["aspectId"] = value
        }

    var value: String
        get() = this["value"]
        set(value) {
            this["value"] = value
        }

    var deleted: Boolean
        get() = this["deleted"] ?: false
        set(value) {
            this["deleted"] = value
        }

    val children: List<ReferenceBookItemVertex>
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).map { it.toReferenceBookItemVertex() }

    /**
     * Return parent ReferenceBookItemVertex if it's not root
     * else [parent] is null
     */
    val parent: ReferenceBookItemVertex?
        get() {
            val oVertex = getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).first() //this maybe bookVertex
            return if (oVertex.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).any())
                oVertex.toReferenceBookItemVertex()
            else
                null
        }

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(aspectId, parent?.id, id, value, children, deleted, version)
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}