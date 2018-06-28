package com.infowings.catalog.data.reference.book

import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

private const val notDeletedSql = "(deleted is NULL or deleted = false)"
private const val selectAllNotDeletedRefBookRoots =
    "SELECT FROM $REFERENCE_BOOK_ITEM_VERTEX WHERE IN(\"$ASPECT_REFERENCE_BOOK_EDGE\").size() = 1 AND $notDeletedSql"


class ReferenceBookDao(private val db: OrientDatabase) {

    fun getAspectVertex(aspectId: String): AspectVertex? = db.getVertexById(aspectId)?.toAspectVertex()

    fun getReferenceBookVertexById(id: String) = transaction(db) {
        return@transaction db.getVertexById(id)?.toReferenceBookItemVertex()
    }

    fun getAllRootVertices(): List<ReferenceBookItemVertex> {
        return db.query(selectAllNotDeletedRefBookRoots) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }
                .toList()
        }
    }

    fun getRootVertex(aspectId: String): ReferenceBookItemVertex? = transaction(db) {
        val aspectVertex = db.getVertexById(aspectId) ?: return@transaction null
        return@transaction aspectVertex.getVertices(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toReferenceBookItemVertex() }
            .filterNot { it.deleted }
            .firstOrNull()
    }

    fun createReferenceBookItemVertex() = db.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX).toReferenceBookItemVertex()

    fun find(id: String): ReferenceBookItemVertex? =
        db.getVertexById(id)?.toReferenceBookItemVertex()

    fun find(ids: List<String>): List<ReferenceBookItemVertex> {
        return db.query(
            "select from $REFERENCE_BOOK_ITEM_VERTEX where @rid in :ids ", mapOf("ids" to ids.map { ORecordId(it) })
        ) { rs ->
            rs.mapNotNull {
                it.toVertexOrNull()?.toReferenceBookItemVertex()
            }.toList()
        }
    }

    fun saveBookItemVertex(
        parentVertex: ReferenceBookItemVertex,
        bookItemVertex: ReferenceBookItemVertex
    ): ReferenceBookItemVertex =
        transaction(db) {
            if (!parentVertex.children.contains(bookItemVertex)) {
                parentVertex.addEdge(bookItemVertex, bookItemVertex.edgeName).save<OEdge>()
                val rootOfParent = parentVertex.root
                if (rootOfParent == null) {
                    parentVertex.addEdge(bookItemVertex, REFERENCE_BOOK_ROOT_EDGE).save<OEdge>()
                } else {
                    rootOfParent.addEdge(bookItemVertex, REFERENCE_BOOK_ROOT_EDGE).save<OEdge>()
                }
            }
            return@transaction bookItemVertex.save<OVertex>().toReferenceBookItemVertex()
        }

    fun removeRefBookItemVertex(bookItemVertex: ReferenceBookItemVertex) {
        transaction(db) {
            bookItemVertex.children.forEach { removeRefBookItemVertex(it) }
            db.delete(bookItemVertex)
        }
    }

    fun getRefBookItemVertexParents(id: String): List<ReferenceBookItemVertex> = session(db) {
        val query = "TRAVERSE IN(\"$REFERENCE_BOOK_CHILD_EDGE\") FROM :itemRecord"
        return@session db.query(query, mapOf("itemRecord" to ORecordId(id))) {
            it.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }.toList()
        }
    }

    fun markItemVertexAsDeleted(bookItemVertex: ReferenceBookItemVertex) {
        transaction(db) {
            bookItemVertex.deleted = true
            bookItemVertex.children.forEach { markItemVertexAsDeleted(it) }
            bookItemVertex.save<OVertex>()
        }
    }
}
