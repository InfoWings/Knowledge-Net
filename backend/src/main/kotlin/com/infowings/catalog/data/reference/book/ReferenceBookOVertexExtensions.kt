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

    private val children: List<ReferenceBookItemVertex>
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).map { it.toReferenceBookItemVertex() }


    private val child: ReferenceBookItemVertex
        get() = children.first()

    private val aspect: OVertex?
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).firstOrNull()

    fun toReferenceBook(): ReferenceBook {
        val aspectId = aspect?.id ?: throw RefBookAspectNotExist(aspectId)
        val root = child.toReferenceBookItem()
        return ReferenceBook(name, aspectId, root)
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}

class ReferenceBookItemVertex(private val vertex: OVertex) : OVertex by vertex {

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

    val parent: ReferenceBookItemVertex?
        get() = getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).firstOrNull()?.toReferenceBookItemVertex()

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(id, value, children)
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}