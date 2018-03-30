package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookItemVertex() = ReferenceBookItemVertex(this)
fun OVertex.toReferenceBookItem() = ReferenceBookItemVertex(this).toReferenceBookItem()

class ReferenceBookItemVertex(private val vertex: OVertex) : OVertex by vertex {
    var value: String
        get() = vertex["value"]
        set(v) {
            vertex["value"] = v
        }

    val children: List<OVertex>
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).toList()

    val child: OVertex?
        get() = children.first()

    val parent: OVertex?
        get() = getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).firstOrNull()

    fun toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItemVertex().toReferenceBookItem() }
        return ReferenceBookItem(id, value, children)
    }
}