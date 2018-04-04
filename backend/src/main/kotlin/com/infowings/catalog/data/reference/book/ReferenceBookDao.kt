package com.infowings.catalog.data.reference.book

import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.toVertexOrNull
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

private const val searchReferenceBookByAspectId = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE aspectId = ?"
private const val selectFromReferenceBook = "SELECT FROM $REFERENCE_BOOK_VERTEX"

class ReferenceBookDao(private val db: OrientDatabase) {

    fun getAspectVertex(aspectId: String): AspectVertex? = db.getVertexById(aspectId)?.toAspectVertex()

    fun getAllReferenceBookVertex(): List<ReferenceBookVertex> =
        db.query(selectFromReferenceBook) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toReferenceBookVertex() }.toList()
        }

    fun getReferenceBookVertex(aspectId: String): ReferenceBookVertex? =
        db.query(searchReferenceBookByAspectId, aspectId) {
            it.map { it.toVertexOrNull()?.toReferenceBookVertex() }.firstOrNull()
        }

    fun createReferenceBookVertex() = db.createNewVertex(REFERENCE_BOOK_VERTEX).toReferenceBookVertex()

    fun createReferenceBookItemVertex() = db.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX).toReferenceBookItemVertex()

    fun getReferenceBookItemVertex(id: String): ReferenceBookItemVertex? =
        db.getVertexById(id)?.toReferenceBookItemVertex()

    fun saveBookItemVertex(
        parentVertex: ReferenceBookItemVertex,
        bookItemVertex: ReferenceBookItemVertex
    ): ReferenceBookItemVertex =
        transaction(db) {
            if (!parentVertex.children.contains(bookItemVertex)) {
                parentVertex.addEdge(bookItemVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
            }
            return@transaction bookItemVertex.save<OVertex>().toReferenceBookItemVertex()
        }

    fun remove(vertex: OVertex) {
        db.delete(vertex)
    }

    fun getRefBookItemVertexParents(id: String): List<ReferenceBookItemVertex> = transaction(db) {
        val query = "TRAVERSE IN(\"$REFERENCE_BOOK_CHILD_EDGE\") FROM :itemRecord"
        return@transaction db.query(query, mapOf("itemRecord" to ORecordId(id))) {
            it.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }.toList()
        }
    }

    fun fakeRemoveReferenceBookVertex(bookVertex: ReferenceBookVertex) {
        transaction(db) {
            bookVertex.deleted = true
            bookVertex.root.children.forEach { fakeRemoveReferenceBookItemVertex(it) }
            bookVertex.save<OVertex>()
        }
    }

    fun fakeRemoveReferenceBookItemVertex(bookItemVertex: ReferenceBookItemVertex) {
        transaction(db) {
            bookItemVertex.deleted = true
            bookItemVertex.children.forEach { fakeRemoveReferenceBookItemVertex(it) }
            bookItemVertex.save<OVertex>()
        }
    }
}
