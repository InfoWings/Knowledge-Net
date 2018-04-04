package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookVertex() = ReferenceBookVertex(this)
fun OVertex.toReferenceBookItemVertex() = ReferenceBookItemVertex(this)

class ReferenceBookVertex(private val vertex: OVertex) : OVertex by vertex {

    var aspectId: String
        get() = this["aspectId"]
        set(value) {
            this["aspectId"] = value
        }

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

    private val aspect: OVertex
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).first()

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

class ReferenceBookItemVertex(private val vertex: OVertex) : OVertex by vertex {

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
            return if (oVertex.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).firstOrNull() != null)
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