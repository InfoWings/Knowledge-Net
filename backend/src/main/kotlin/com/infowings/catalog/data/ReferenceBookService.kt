package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.record.OVertex
import kotlinx.serialization.json.JSON


const val REFERENCE_BOOK_VERTEX = "ReferenceBookVertex"
const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_ITEM_EDGE = "ReferenceBookItemEdge"

interface ReferenceBookService {
    fun getReferenceBook(name: String): ReferenceBook?
    fun saveReferenceBook(book: ReferenceBook): ReferenceBook
    fun removeReferenceBook(name: String)
    fun changeReferenceBookDescription(name: String, newDescription: String): ReferenceBook
}

interface ReferenceBookItemService {
    fun removeReferenceBookItemAndAllChildren(bookId: String)
    fun addReferenceBookItemAsChild(parentId: String, child: ReferenceBookItem)

    fun changeReferenceBookItemName(id: String, newName: String)
    fun changeReferenceBookItemDescription(id: String, newDescription: String)
    fun removeParentChildRelationship(parentId: String, childId: String)
}

class ReferenceBookServiceImpl(val database: OrientDatabase) : ReferenceBookService {
    override fun getReferenceBook(name: String): ReferenceBook? = getVertexByName(name).toReferenceBook()

    override fun saveReferenceBook(book: ReferenceBook): ReferenceBook {
        session(database) { session ->
            if (getReferenceBook(book.name) != null) {
                throw IllegalStateException("Reference book with this name already exists")
            }
            val rootVertex = session.newVertex(REFERENCE_BOOK_VERTEX)
            rootVertex.setProperty("name", book.name)
            rootVertex.setProperty("description", book.description)
            session.save<OVertex>(rootVertex)
            Unit
        }
        return getReferenceBook(book.name) ?: throw IllegalStateException("Saving reference book exception")
    }

    override fun removeReferenceBook(name: String) = session(database) {
        getVertexByName(name)?.delete<OVertex>()
        Unit
    }


    override fun changeReferenceBookDescription(name: String, newDescription: String): ReferenceBook {
        session(database) {
            val vertex = getVertexByName(name)
                    ?: throw IllegalStateException("Reference book with name $name not saved")
            vertex.setProperty("description", newDescription)
            vertex.save<OVertex>()
        }
        return getReferenceBook(name) ?: throw IllegalStateException("Reference book with name $name not saved")
    }

    private fun getVertexByName(name: String): OVertex? = database.query(SEARCH_BY_NAME_QUERY, name) {
        it.map { it.toVertexOrNUll() }.firstOrNull()
    }
}

private const val SEARCH_BY_NAME_QUERY = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE name = ?"

private fun OVertex?.toReferenceBook(): ReferenceBook? = this?.let {
    JSON.nonstrict.parse<ReferenceBook>(it.toJSON()).copy(id = it.identity.next())
}