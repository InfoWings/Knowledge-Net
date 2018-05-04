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

const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_CHILD_EDGE = "ReferenceBookChildEdge"

class ReferenceBookItemVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_ITEM_VERTEX
    val edgeName = REFERENCE_BOOK_CHILD_EDGE

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf("value" to asStringOrEmpty(value)),
        links = mapOf("children" to children.map { it.identity })
    )

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

    val children: List<ReferenceBookItemVertex> =
        getVertices(ODirection.OUT, edgeName).map { it.toReferenceBookItemVertex() }

    /**
     * Return parent vertex (ReferenceBookItemVertex or ReferenceBookVertex) or null
     */
    val parent: OVertex?
        get() {
            val oVertex = getVertices(ODirection.IN, edgeName).firstOrNull()
            return oVertex?.let {
                if (oVertex.getVertices(ODirection.IN, edgeName).any())
                    oVertex.toReferenceBookItemVertex()
                else
                    oVertex.toReferenceBookVertex()
            }
        }

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(id, value, children, deleted, version)
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}