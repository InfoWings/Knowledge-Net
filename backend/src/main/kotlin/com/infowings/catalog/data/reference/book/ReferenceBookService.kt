package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex


const val REFERENCE_BOOK_VERTEX = "ReferenceBookVertex"
const val REFERENCE_BOOK_ITEM_VERTEX = "ReferenceBookItemVertex"
const val REFERENCE_BOOK_CHILD_EDGE = "ReferenceBookChildEdge"
const val REFERENCE_BOOK_ASPECT_EDGE = "ReferenceBookAspectEdge"


class ReferenceBookService(val db: OrientDatabase, private val dao: ReferenceBookDao) {
    private val validator = ReferenceBookValidator(dao)

    /**
     * Get all ReferenceBook instances
     */
    fun getAllReferenceBooks(): List<ReferenceBook> = transaction(db) {
        return@transaction dao.getAllReferenceBookVertex().map { it.toReferenceBook() }.toList()
    }

    /**
     * Get ReferenceBook instance by aspect id
     * @throws RefBookNotExist
     */
    fun getReferenceBook(aspectId: String): ReferenceBook = transaction(db){
        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        return@transaction referenceBookVertex.toReferenceBook()
    }

    /**
     * Create ReferenceBook with name [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(db) {
        dao.getReferenceBookVertex(aspectId)?.let { throw RefBookAlreadyExist(aspectId) }

        val referenceBookVertex = dao.createReferenceBookVertex()
        referenceBookVertex.aspectId = aspectId
        referenceBookVertex.name = name

        val rootVertex = dao.createReferenceBookItemVertex()
        rootVertex.value = "root"
        rootVertex.aspectId = aspectId

        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()

        val aspectVertex = dao.getVertex(aspectId)
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()

        return@transaction Pair(
            referenceBookVertex.save<OVertex>().toReferenceBookVertex(),
            rootVertex.save<OVertex>().toReferenceBookItemVertex()
        )
    }.let {
        val bookVertex = it.first
        val itemVertex = it.second
        val bookItem = ReferenceBookItem(
            aspectId,
            null,
            itemVertex.id,
            itemVertex.value,
            emptyList(),
            itemVertex.deleted,
            itemVertex.version
        )
        ReferenceBook(aspectId, bookVertex.name, bookItem, bookVertex.deleted, bookVertex.version)
    }

    /**
     * Change ReferenceBook name to [newName] where aspect id equals to [aspectId]
     * @throws RefBookNotExist if ReferenceBook with [aspectId] not found
     * */
    fun updateReferenceBook(aspectId: String, newName: String) = transaction(db) {
        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        referenceBookVertex["name"] = newName
        return@transaction referenceBookVertex.save<OVertex>().toReferenceBookVertex().toReferenceBook()
    }

    /**
     * Get ReferenceBookItem by id
     * @throws RefBookItemNotExist
     */
    fun getReferenceBookItem(id: String): ReferenceBookItem = transaction(db){
        val bookItemVertex = dao.getReferenceBookItemVertex(id)
        return@transaction bookItemVertex?.toReferenceBookItem() ?: throw RefBookItemNotExist(id)
    }

    /**
     * Add ReferenceBookItem instance to item with id = [parentId]
     * @throws RefBookItemNotExist if item with id [parentId] doesn't exist
     * @throws RefBookChildAlreadyExist if item with id [parentId] already has child with value equals to [value]
     */
    internal fun addReferenceBookItem(aspectId: String, parentId: String, value: String): String =
        transaction(db) {
            val parentVertex = dao.getReferenceBookItemVertex(parentId) ?: throw RefBookItemNotExist(parentId)

            if (parentVertex.children.any { it.value == value }) {
                throw RefBookChildAlreadyExist(parentId, value)
            }

            val childVertex = dao.createReferenceBookItemVertex()
            parentVertex.addEdge(childVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
            childVertex.aspectId = aspectId
            childVertex.value = value

            return@transaction childVertex.save<OVertex>()
        }.id

    /**
     * Change value of ReferenceBookItem with id [id]
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     */
    internal fun changeValue(id: String, value: String) {
        transaction(db) {
            val vertex = dao.getReferenceBookItemVertex(id) ?: throw RefBookItemNotExist(id)
            val parentVertex = vertex.parent!!
            val vertexWithSameNameAlreadyExist = parentVertex.children.any { it.value == value && it.id != id }

            if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX && vertexWithSameNameAlreadyExist) {
                throw RefBookChildAlreadyExist(parentVertex.id, value)
            }

            vertex.value = value
            return@transaction vertex.save<OVertex>()
        }
    }

    fun addItemAndGetReferenceBook(bookItem: ReferenceBookItem): ReferenceBook {
        addReferenceBookItem(bookItem.aspectId, bookItem.parentId!!, bookItem.value)
        return getReferenceBook(bookItem.aspectId)
    }

    fun updateItemAndGetReferenceBook(bookItem: ReferenceBookItem): ReferenceBook {
        changeValue(bookItem.id, bookItem.value)
        return getReferenceBook(bookItem.aspectId)
    }

    /**
     * Make ReferenceBookItem with id [sourceId] to be a child of ReferenceBookItem with id [targetId]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [sourceId] is a parent pf [targetId]
     */
    fun moveReferenceBookItem(sourceId: String, targetId: String) {
        transaction(db) {
            val sourceVertex = dao.getReferenceBookItemVertex(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = dao.getReferenceBookItemVertex(targetId) ?: throw RefBookItemNotExist(targetId)

            var tmpPointer = targetVertex
            while (tmpPointer.parent != null) {
                val parent = tmpPointer.parent!!
                if (tmpPointer.id == sourceId) {
                    throw RefBookItemMoveImpossible(sourceId, targetId)
                }
                tmpPointer = parent
            }

            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }
            return@transaction targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
        }
    }

    fun removeReferenceBookItem(bookItem: ReferenceBookItem, force: Boolean = false) {
        transaction(db) {
            val bookItemVertex =
                dao.getReferenceBookItemVertex(bookItem.id) ?: throw RefBookItemNotExist(bookItem.aspectId)

            validator.checkRefBookItemAndChildrenVersion(bookItemVertex, bookItem)

            //TODO: add correct checking for clause is item linked by Object entities
            val isLinkedByObjects = false
            when {
                isLinkedByObjects && force -> fakeRemoveReferenceBookItemVertex(bookItemVertex)
                isLinkedByObjects -> throw RefBookItemHasLinkedEntitiesException(bookItem.id)
                else -> dao.remove(bookItemVertex)
            }
        }
    }

    private fun fakeRemoveReferenceBookVertex(bookVertex: ReferenceBookVertex) {
        session(db) {
            bookVertex.deleted = true
            bookVertex.save<OVertex>()
        }
    }

    private fun fakeRemoveReferenceBookItemVertex(bookItemVertex: ReferenceBookItemVertex) {
        session(db) {
            bookItemVertex.deleted = true
            bookItemVertex.save<OVertex>()
        }
    }
}

sealed class ReferenceBookException(message: String? = null) : Exception(message)
class RefBookAlreadyExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookNotExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
class RefBookChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
class RefBookAspectNotExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookItemMoveImpossible(sourceId: String, targetId: String) :
    ReferenceBookException("sourceId: $sourceId, targetId: $targetId")

class RefBookItemHasLinkedEntitiesException(val id: String) : ReferenceBookException("id: $id")
class RefBookItemConcurrentModificationException(id: String, message: String) :
    ReferenceBookException("id = $id, message = $message")
