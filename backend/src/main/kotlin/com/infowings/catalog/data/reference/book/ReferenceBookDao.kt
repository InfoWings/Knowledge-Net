package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

private const val searchReferenceBookByAspectId = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE aspectId = ?"
private const val selectFromReferenceBook = "SELECT FROM $REFERENCE_BOOK_VERTEX"

class ReferenceBookDao(private val db: OrientDatabase) {

    fun getVertex(id: String): OVertex? = db.getVertexById(id)

    fun getAllReferenceBookVertex(): List<ReferenceBookVertex> =
        db.query(selectFromReferenceBook) { rs ->
            rs.mapNotNull { it.toVertexOrNUll()?.toReferenceBookVertex() }.toList()
        }

    fun getReferenceBookVertex(aspectId: String): ReferenceBookVertex? =
        db.query(searchReferenceBookByAspectId, aspectId) {
            it.map { it.toVertexOrNUll()?.toReferenceBookVertex() }.firstOrNull()
        }

    fun createReferenceBookVertex() = db.createNewVertex(REFERENCE_BOOK_VERTEX).toReferenceBookVertex()

    fun createReferenceBookItemVertex() = db.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX).toReferenceBookItemVertex()

    fun getReferenceBookItemVertex(id: String): ReferenceBookItemVertex? =
        db.getVertexById(id)?.toReferenceBookItemVertex()

    fun saveBookItem(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem): ReferenceBookItemVertex =
        session(db) {
            val parentId = bookItem.parentId!!
            val parentVertex = db.getVertexById(parentId) ?: throw RefBookItemNotExist(parentId)
            if (!parentVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_ITEM_VERTEX).contains(bookItemVertex)) {
                parentVertex.addEdge(bookItemVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
            }
            bookItemVertex.value = bookItem.value
            bookItemVertex.aspectId = bookItem.aspectId
            return@session bookItemVertex.save<OVertex>().toReferenceBookItemVertex()
        }

    fun remove(vertex: OVertex) {
        db.delete(vertex)
    }
}
