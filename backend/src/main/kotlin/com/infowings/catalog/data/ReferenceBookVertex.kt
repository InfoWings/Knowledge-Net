package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookVertex() = ReferenceBookVertex(this)
fun OVertex.toReferenceBook() = ReferenceBookVertex(this).toReferenceBook()

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

    val children: List<OVertex>
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).toList()

    val child: OVertex?
        get() = children.first()


    var deleted: Boolean
        get() = this["deleted"] ?: false
        set(value) {
            this["deleted"] = value
        }

    val aspect: OVertex?
        get() = getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).firstOrNull()


    fun toReferenceBook(): ReferenceBook {
        val aspectId = aspect?.id ?: throw RefBookAspectNotExist(aspectId)
        val root = toReferenceBookVertex().child!!.toReferenceBookItem()
        return ReferenceBook(name, aspectId, root)
    }
}