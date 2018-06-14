package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.OBJECT_VALUE_REFBOOK_ITEM_EDGE
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import hasIncomingEdges

fun OVertex.toReferenceBookItemVertex() = ReferenceBookItemVertex(this)

const val ASPECT_REFERENCE_BOOK_EDGE = "AspectReferenceBookEdge"
const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_CHILD_EDGE = "ReferenceBookChildEdge"
const val REFERENCE_BOOK_ROOT_EDGE = "ReferenceBookRootEdge"

class ReferenceBookItemVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_ITEM_VERTEX
    val edgeName = REFERENCE_BOOK_CHILD_EDGE

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "value" to asStringOrEmpty(value),
            "description" to asStringOrEmpty(description)
        ),
        links = mapOf(
            "children" to children.map { it.identity },
            "aspect" to listOfNotNull(aspect).map { it.identity },
            "parent" to listOfNotNull(parent).map { it.identity },
            "root" to listOfNotNull(root).map { it.identity }
        )
    )

    val aspect: AspectVertex?
        get() = getVertices(ODirection.IN, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toAspectVertex() }
            .filterNot { it.deleted }
            .firstOrNull()

    var value: String
        get() = this["value"]
        set(value) {
            this["value"] = value
        }

    var description: String?
        get() = this["description"]
        set(value) {
            this["description"] = value
        }

    var deleted: Boolean
        get() = this["deleted"] ?: false
        set(value) {
            this["deleted"] = value
        }

    val children: List<ReferenceBookItemVertex>
        get() = getVertices(ODirection.OUT, edgeName).map { it.toReferenceBookItemVertex() }

    val root: ReferenceBookItemVertex?
        get() = getVertices(ODirection.IN, REFERENCE_BOOK_ROOT_EDGE).firstOrNull()?.toReferenceBookItemVertex()

    /**
     * Return parent ReferenceBookItemVertex or null
     */
    val parent: ReferenceBookItemVertex?
        get() = getVertices(ODirection.IN, edgeName).firstOrNull()?.toReferenceBookItemVertex()

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(id, value, description, children, deleted, version)
    }

    fun isLinkedBy() = hasIncomingEdges(OBJECT_VALUE_REFBOOK_ITEM_EDGE)

    fun getLinkedInSubtree(): MutableList<ReferenceBookItemVertex> {
        val linkedChildren = children.flatMap { it.getLinkedInSubtree() }.toMutableList()
        if (isLinkedBy()) {
            linkedChildren.add(this)
        }
        return linkedChildren
    }

    override fun equals(other: Any?): Boolean = vertex == other

    override fun hashCode(): Int = vertex.hashCode()
}
