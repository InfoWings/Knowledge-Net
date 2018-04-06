package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.history.HistoryService
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


class ReferenceBookService(
    val db: OrientDatabase,
    private val dao: ReferenceBookDao,
    val historyService: HistoryService
) {
    private val validator = ReferenceBookValidator(dao)

    /**
     * Get all ReferenceBook instances
     */
    fun getAllReferenceBooks(): List<ReferenceBook> = transaction(db) {
        logger.debug("Getting all ReferenceBook instances")
        return@transaction dao.getAllReferenceBookVertex().map { it.toReferenceBook() }.toList()
    }

    /**
     * Get ReferenceBook instance by [aspectId]
     * @throws RefBookNotExist
     */
    fun getReferenceBook(aspectId: String): ReferenceBook = transaction(db) {
        logger.debug("Getting ReferenceBook by aspectId: $aspectId")
        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        return@transaction referenceBookVertex.toReferenceBook()
    }

    /**
     * Create ReferenceBook with name = [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String, userName: String): ReferenceBook = transaction(db) {
        logger.debug("Creating ReferenceBook name: $name aspectId: $aspectId by $userName")

        dao.getReferenceBookVertex(aspectId)?.let { throw RefBookAlreadyExist(aspectId) }

        val referenceBookVertex = dao.createReferenceBookVertex()
        referenceBookVertex.aspectId = aspectId
        referenceBookVertex.name = name

        val rootVertex = dao.createReferenceBookItemVertex()
        rootVertex.value = "root"
        rootVertex.aspectId = aspectId

        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        val aspectVertex = dao.getAspectVertex(aspectId) ?: throw RefBookAspectNotExist(aspectId)
        aspectVertex.validateForRemoved()
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()

        val savedReferenceBookVertex = referenceBookVertex.save<OVertex>().toReferenceBookVertex()
        historyService.storeFact(savedReferenceBookVertex.toCreateFact(userName))

        val savedRootVertex = rootVertex.save<OVertex>().toReferenceBookItemVertex()
        historyService.storeFact(savedRootVertex.toCreateFact(userName))

        return@transaction Pair(savedReferenceBookVertex, savedRootVertex)
    }.let {
        val bookVertex = it.first
        val rootVertex = it.second
        val root = ReferenceBookItem(
            aspectId,
            null,
            rootVertex.id,
            rootVertex.value,
            emptyList(),
            rootVertex.deleted,
            rootVertex.version
        )
        ReferenceBook(aspectId, bookVertex.name, root, bookVertex.deleted, bookVertex.version)
    }

    /**
     * Update ReferenceBook name
     * @throws RefBookNotExist
     */
    fun updateReferenceBook(book: ReferenceBook, userName: String): Unit = transaction(db) {
        val aspectId = book.aspectId
        val newName: String = book.name

        logger.debug("Updating ReferenceBook name: $newName aspectId: $aspectId by $userName")

        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        val before = referenceBookVertex.currentSnapshot()

        referenceBookVertex
            .validateForRemoved()
            .validateVersion(book)

        referenceBookVertex.name = newName
        referenceBookVertex.save<OVertex>()

        historyService.storeFact(referenceBookVertex.toUpdateFact(userName, before))
    }

    /**
     * Remove ReferenceBook [referenceBook] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and it has linked by Objects child
     * @throws RefBookNotExist
     */
    fun removeReferenceBook(referenceBook: ReferenceBook, userName: String, force: Boolean = false) = transaction(db) {
        val aspectId = referenceBook.aspectId
        logger.debug("Removing ReferenceBook aspectId: $aspectId by $userName")

        val referenceBookVertex = dao.getReferenceBookVertex(aspectId) ?: throw RefBookNotExist(aspectId)
        referenceBookVertex.validateRefBookAndItemsVersions(referenceBook)

        //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
        val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
        val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
        when {
            hasChildItemLinkedByObject && force -> dao.markBookVertexAsDeleted(referenceBookVertex)
            hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
            else -> dao.remove(referenceBookVertex)
        }

        //TODO: add history
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
     * @throws RefBookItemIllegalArgumentException if parentId is null
     * @return id of added ReferenceBookItem
     */
    fun addReferenceBookItem(bookItem: ReferenceBookItem, userName: String): String =
        transaction(db) {
            val parentId = bookItem.parentId ?: throw RefBookItemIllegalArgumentException("parent id must not be null")
            val value = bookItem.value

            logger.debug("Adding ReferenceBookItem parentId: $parentId, value: $value by $userName")

            val parentVertex = dao.getReferenceBookItemVertex(parentId) ?: throw RefBookItemNotExist(parentId)
            val parentBefore = parentVertex.currentSnapshot()
            parentVertex.validateValue(value, null)

            val itemVertex = dao.createReferenceBookItemVertex()
            itemVertex.aspectId = bookItem.aspectId
            itemVertex.value = value

            val savedItemVertex = dao.saveBookItemVertex(parentVertex, itemVertex)

            historyService.storeFact(savedItemVertex.toCreateFact(userName))
            historyService.storeFact(parentVertex.toUpdateFact(userName, parentBefore))

            return@transaction savedItemVertex
        }.id

    /**
     * Change value of ReferenceBookItem [bookItem] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] has linked by Objects child
     * @throws RefBookItemIllegalArgumentException if cannot parent vertex is null
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     */
    fun changeValue(bookItem: ReferenceBookItem, userName: String, force: Boolean = false) =
        transaction(db) {
            val id = bookItem.id
            val value = bookItem.value

            logger.debug("Updating ReferenceBookItem id: $id, value: $value by $userName")

            val itemVertex = dao.getReferenceBookItemVertex(id) ?: throw RefBookItemNotExist(id)
            val before = itemVertex.currentSnapshot()
            val parentVertex =
                itemVertex.parent ?: throw RefBookItemIllegalArgumentException("parent vertex must not be null")

            itemVertex
                .validateForRemoved()
                .validateItemAndChildrenVersions(bookItem)

            parentVertex
                .validateValue(value, id)

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            if (hasChildItemLinkedByObject && !force) {
                throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
            }

            itemVertex.value = value
            val savedItemVertex = dao.saveBookItemVertex(parentVertex, itemVertex)

            historyService.storeFact(itemVertex.toUpdateFact(userName, before))

            return@transaction savedItemVertex
        }

    /**
     * Remove [bookItem] if it has not linked by Object child
     * If it has linked by Object child and [force] == true then mark [bookItem] and its children as deleted
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] has linked by Objects child
     * @throws RefBookItemIllegalArgumentException if [bookItem] is root
     * @throws RefBookItemNotExist
     */
    fun removeReferenceBookItem(bookItem: ReferenceBookItem, userName: String, force: Boolean = false) {
        transaction(db) {
            logger.debug("Removing ReferenceBookItem id: ${bookItem.id} by $userName")

            val bookItemVertex =
                dao.getReferenceBookItemVertex(bookItem.id) ?: throw RefBookItemNotExist(bookItem.aspectId)

            bookItemVertex
                .validateIsNotRoot()
                .validateItemAndChildrenVersions(bookItem)

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            when {
                hasChildItemLinkedByObject && force -> dao.markItemVertexAsDeleted(bookItemVertex)
                hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
                else -> dao.remove(bookItemVertex)
            }

            //TODO: add history
        }
    }

    /**
     * Make ReferenceBookItem [source] child of ReferenceBookItem [target]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [source] is a parent of [target]
     * @throws RefBookItemIllegalArgumentException if [source] is root
     */
    fun moveReferenceBookItem(source: ReferenceBookItem, target: ReferenceBookItem, userName: String) {
        transaction(db) {
            val sourceId = source.id
            val targetId = target.id
            logger.debug("Moving ReferenceBookItem sourceId: $sourceId, targetId: $targetId by $userName")

            val sourceVertex = dao.getReferenceBookItemVertex(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = dao.getReferenceBookItemVertex(targetId) ?: throw RefBookItemNotExist(targetId)

            targetVertex
                .validateForRemoved()
                .validateItemAndChildrenVersions(target)

            sourceVertex
                .validateForRemoved()
                .validateIsNotRoot()
                .validateItemAndChildrenVersions(source)
                .validateForMoving(targetVertex)

            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }

            //TODO: add history

            return@transaction targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
        }
    }

    private fun AspectVertex.validateForRemoved() =
        this.also { if (it.deleted) throw RefBookAspectNotExist(it.id) }

    private fun ReferenceBookItemVertex.validateIsNotRoot(): ReferenceBookItemVertex =
        this.also { validator.checkIsNotRoot(this) }

    private fun ReferenceBookItemVertex.validateForRemoved(): ReferenceBookItemVertex =
        this.also { validator.checkForBookItemRemoved(this) }

    private fun ReferenceBookItemVertex.validateItemAndChildrenVersions(bookItem: ReferenceBookItem): ReferenceBookItemVertex =
        this.also { validator.checkRefBookItemAndChildrenVersions(this, bookItem) }

    private fun ReferenceBookItemVertex.validateForMoving(targetVertex: ReferenceBookItemVertex): ReferenceBookItemVertex =
        this.also { validator.checkForMoving(this, targetVertex) }

    private fun ReferenceBookItemVertex.validateValue(value: String, id: String?): ReferenceBookItemVertex =
        this.also { validator.checkRefBookItemValue(this, value, id) }

    private fun ReferenceBookVertex.validateVersion(book: ReferenceBook): ReferenceBookVertex =
        this.also { validator.checkRefBookVersion(this, book) }

    private fun ReferenceBookVertex.validateForRemoved(): ReferenceBookVertex =
        this.also { validator.checkForBookRemoved(this) }

    private fun ReferenceBookVertex.validateRefBookAndItemsVersions(book: ReferenceBook): ReferenceBookVertex =
        this.also { validator.checkRefBookAndItemsVersions(this, book) }

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

class RefBookItemIllegalArgumentException(message: String) : ReferenceBookException(message)

class RefBookItemHasLinkedEntitiesException(val itemsWithLinkedObjects: List<ReferenceBookItem>) :
    ReferenceBookException("${itemsWithLinkedObjects.map { it.id }}")

class RefBookItemConcurrentModificationException(id: String, message: String) :
    ReferenceBookException("id: $id, message: $message")

class RefBookConcurrentModificationException(aspectId: String, message: String) :
    ReferenceBookException("aspectId: $aspectId, message: $message")