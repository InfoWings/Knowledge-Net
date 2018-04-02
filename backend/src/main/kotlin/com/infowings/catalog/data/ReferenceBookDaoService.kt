package com.infowings.catalog.data

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.record.OVertex

const val REFERENCE_BOOK_VERTEX = "ReferenceBookVertex"
const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_CHILD_EDGE = "ReferenceBookChildEdge"
const val REFERENCE_BOOK_ASPECT_EDGE = "ReferenceBookAspectEdge"

private const val searchReferenceBookByAspectId = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE aspectId = ?"


class ReferenceBookDaoService(private val db: OrientDatabase) {
    fun newReferenceBookItemVertex(): ReferenceBookItemVertex = db.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX).toReferenceBookItemVertex()

    fun newReferenceBookVertex(): ReferenceBookVertex = db.createNewVertex(REFERENCE_BOOK_VERTEX).toReferenceBookVertex()

    fun findReferenceBookItemVertex(id: String): ReferenceBookItemVertex =
        db.getVertexById(id)?.toReferenceBookItemVertex() ?: throw RefBookItemNotExist(id)

    fun findReferenceBookVertexByAspectId(aspectId: String): ReferenceBookVertex? =
        db.query(searchReferenceBookByAspectId, aspectId) { it.map { it.toVertexOrNUll() }.firstOrNull() }
            ?.toReferenceBookVertex()
}