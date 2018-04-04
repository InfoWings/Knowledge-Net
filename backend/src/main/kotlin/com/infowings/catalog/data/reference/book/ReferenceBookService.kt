package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
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
        logger.debug("Getting all reference books")
        return@transaction dao.getAllReferenceBookVertex().map { it.toReferenceBook() }.toList()
    }

    /**
     * Get ReferenceBook instance by [aspectId]
     * @throws RefBookNotExist
     */
    fun getReferenceBook(aspectId: String): ReferenceBook = transaction(db) {
        logger.debug("Getting reference books by aspectId=$aspectId")
        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        return@transaction referenceBookVertex.toReferenceBook()
    }

    /**
     * Create ReferenceBook with name = [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(db) {
        logger.debug("Creating reference book. name: $name aspectId: $aspectId")

        dao.getReferenceBookVertex(aspectId)?.let { throw RefBookAlreadyExist(aspectId) }

        val referenceBookVertex = dao.createReferenceBookVertex()
        referenceBookVertex.aspectId = aspectId
        referenceBookVertex.name = name

        val rootVertex = dao.createReferenceBookItemVertex()
        rootVertex.value = "root"
        rootVertex.aspectId = aspectId

        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        val aspectVertex = dao.getAspectVertex(aspectId) ?: throw RefBookAspectNotExist(aspectId)
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
     * Update ReferenceBook name
     * @throws RefBookNotExist
     */
    fun updateReferenceBook(book: ReferenceBook) = transaction(db) {
        val aspectId = book.aspectId
        val newName: String = book.name

        logger.debug("Updating reference book name to $newName where aspectId=$aspectId")

        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        validator.checkRefBookVersion(referenceBookVertex, book)
        validator.checkForBookRemoved(referenceBookVertex)
        referenceBookVertex.name = newName
        return@transaction referenceBookVertex.save<OVertex>().toReferenceBookVertex().toReferenceBook()
    }

    /**
     * Remove ReferenceBook [referenceBook] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and it has linked by Objects child
     * @throws RefBookNotExist
     */
    fun removeReferenceBook(referenceBook: ReferenceBook, force: Boolean = false) = transaction(db) {
        val aspectId = referenceBook.aspectId
        logger.debug("Removing reference book. aspectId: $aspectId")

        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        validator.checkRefBookAndItemsVersion(referenceBookVertex, referenceBook)

        //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
        val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
        val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
        when {
            hasChildItemLinkedByObject && force -> dao.fakeRemoveReferenceBookVertex(referenceBookVertex)
            hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
            else -> dao.remove(referenceBookVertex)
        }
    }

    /**
     * Get ReferenceBookItem by [id]
     * @throws RefBookItemNotExist
     */
    fun getReferenceBookItem(id: String): ReferenceBookItem = transaction(db) {
        logger.debug("Getting ReferenceBookItem id: $id")
        val bookItemVertex = dao.getReferenceBookItemVertex(id)
        return@transaction bookItemVertex?.toReferenceBookItem() ?: throw RefBookItemNotExist(id)
    }

    /**
     * Add ReferenceBookItem instance to item parent
     * @throws RefBookItemNotExist if parent item doesn't exist
     * @throws RefBookChildAlreadyExist if parent item already has child with the same value
     */
    fun addReferenceBookItem(bookItem: ReferenceBookItem): String =
        transaction(db) {
            val parentId = bookItem.parentId ?: throw RefBookModificationException("parent id must not be null")
            val value = bookItem.value

            logger.debug("Adding reference book item. parentId: $parentId, value: $value")

            val parentVertex = dao.getReferenceBookItemVertex(parentId) ?: throw RefBookItemNotExist(parentId)
            validator.checkRefBookItemValue(parentVertex, value, null)

            val itemVertex = dao.createReferenceBookItemVertex()
            itemVertex.aspectId = bookItem.aspectId
            itemVertex.value = value

            return@transaction dao.saveBookItemVertex(parentVertex, itemVertex)
        }.id

    /**
     * Change value of ReferenceBookItem [bookItem] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] has linked by Objects child
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     */
    fun changeValue(bookItem: ReferenceBookItem, force: Boolean = false) =
        transaction(db) {
            val id = bookItem.id
            val value = bookItem.value

            logger.debug("Updating reference book item. id: $id, value: $value")

            val itemVertex = dao.getReferenceBookItemVertex(id) ?: throw RefBookItemNotExist(id)
            val parentVertex = itemVertex.parent ?: throw RefBookModificationException("parent vertex must not be null")
            validator.checkForBookItemRemoved(itemVertex)
            validator.checkRefBookItemAndChildrenVersion(itemVertex, bookItem)
            validator.checkRefBookItemValue(parentVertex, value, id)

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            when {
                hasChildItemLinkedByObject && force -> itemVertex.value = value
                hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
                else -> itemVertex.value = value
            }

            itemVertex.value = value

            return@transaction dao.saveBookItemVertex(parentVertex, itemVertex)
        }

    /**
     * Remove [bookItem] if it has not linked by Object child
     * If it has linked by Object child and [force] == true then mark [bookItem] and its children as deleted
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] has linked by Objects child
     * @throws RefBookItemNotExist
     */
    fun removeReferenceBookItem(bookItem: ReferenceBookItem, force: Boolean = false) {
        transaction(db) {
            val bookItemVertex =
                dao.getReferenceBookItemVertex(bookItem.id) ?: throw RefBookItemNotExist(bookItem.aspectId)

            validator.checkRefBookItemAndChildrenVersion(bookItemVertex, bookItem)

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            when {
                hasChildItemLinkedByObject && force -> dao.fakeRemoveReferenceBookItemVertex(bookItemVertex)
                hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
                else -> dao.remove(bookItemVertex)
            }
        }
    }

    /**
     * Make ReferenceBookItem [source] child of ReferenceBookItem [target]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [source] is a parent of [target]
     */
    fun moveReferenceBookItem(source: ReferenceBookItem, target: ReferenceBookItem) {
        transaction(db) {
            val sourceId = source.id
            val targetId = target.id
            logger.debug("Moving ReferenceBookItem. sourceId: $sourceId, targetId: $targetId")

            val sourceVertex = dao.getReferenceBookItemVertex(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = dao.getReferenceBookItemVertex(targetId) ?: throw RefBookItemNotExist(targetId)

            validator.checkForBookItemRemoved(sourceVertex)
            validator.checkForBookItemRemoved(targetVertex)
            validator.checkRefBookItemAndChildrenVersion(sourceVertex, source)
            validator.checkRefBookItemAndChildrenVersion(targetVertex, target)
            validator.checkForMoving(sourceVertex, targetVertex)

            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }
            return@transaction targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
        }
    }
}

private val logger = loggerFor<ReferenceBookService>()

sealed class ReferenceBookException(message: String? = null) : Exception(message)
class RefBookAlreadyExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookNotExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
class RefBookChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
class RefBookAspectNotExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookItemMoveImpossible(sourceId: String, targetId: String) :
    ReferenceBookException("sourceId: $sourceId, targetId: $targetId")

class RefBookModificationException(message: String) : ReferenceBookException(message)

class RefBookItemHasLinkedEntitiesException(val itemsWithLinkedObjects: List<ReferenceBookItem>) :
    ReferenceBookException("${itemsWithLinkedObjects.map { it.id }}")

class RefBookItemConcurrentModificationException(id: String, message: String) :
    ReferenceBookException("id: $id, message: $message")

class RefBookConcurrentModificationException(aspectId: String, message: String) :
    ReferenceBookException("aspectId: $aspectId, message: $message")