package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toReferenceBookVertex() = ReferenceBookVertex(this)
fun OVertex.toReferenceBook() = ReferenceBookVertex(this).toReferenceBook()

class ReferenceBookVertex(private val vertex: OVertex) : HistoryAware,  OVertex by vertex {
    override val entityClass = REFERENCE_BOOK_VERTEX

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "name" to asStringOrEmpty(name)
        ),
        links = mapOf(
            "aspect" to listOf(aspectOVertex().identity)
        )
    )

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

    fun aspectOVertex() = aspect ?: throw RefBookAspectNotExist(aspectId)

    fun toReferenceBook(): ReferenceBook {
        val aspectId = aspectOVertex().id
        val root = toReferenceBookVertex().child!!.toReferenceBookItem()
        return ReferenceBook(name, aspectId, root)
    }
}