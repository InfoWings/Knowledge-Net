package com.infowings.catalog.data.reference.book

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex


class ReferenceBookService(
    val db: OrientDatabase,
    private val dao: ReferenceBookDao,
    val historyService: HistoryService,
    private val userService: UserService
) {
    private val validator = ReferenceBookValidator(dao)

    /**
     * Get all ReferenceBook instances
     */
    fun getAllReferenceBooks(): List<ReferenceBook> = transaction(db) {
        logger.debug("Getting all ReferenceBook instances")
        return@transaction dao.getAllRootVertices()
            .map { it.toReferenceBook(it.aspect?.id ?: throw RefBookAspectNotFoundException(it.id)) }
    }

    fun getReferenceBookNameById(id: String) = dao.getReferenceBookVertexById(id)?.toReferenceBookItem()?.value

    fun getReferenceBookById(id: String) = dao.getReferenceBookVertexById(id)?.toReferenceBook()

    /**
     * Return ReferenceBook instance by [aspectId] or null if not found
     */
    fun getReferenceBookOrNull(aspectId: String): ReferenceBook? = transaction(db) {
        logger.debug("Getting ReferenceBook by aspectId: $aspectId")
        val rootVertex = dao.getRootVertex(aspectId)
        return@transaction rootVertex?.toReferenceBook(aspectId)
    }

    /**
     * Get ReferenceBook instance by [aspectId]
     * @throws RefBookNotExist
     */
    fun getReferenceBook(aspectId: String): ReferenceBook =
        getReferenceBookOrNull(aspectId) ?: throw RefBookNotExist(aspectId)

    /**
     * Create ReferenceBook with name = [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String, username: String): ReferenceBook {
        val userVertex = userService.findUserVertexByUsername(username)

        return transaction(db) {
            logger.debug("Creating ReferenceBook name: $name aspectId: $aspectId by $username")
            val context = HistoryContext(userVertex)

            // TODO: get aspect vertex via AspectService
            val aspectVertex = dao.getAspectVertex(aspectId) ?: throw AspectDoesNotExist(aspectId)
            val aspectBefore = aspectVertex.currentSnapshot()

            aspectVertex.validateForRemoved()
            if (aspectVertex.baseType != BaseType.Text.name) {
                throw RefBookIncorrectAspectType(aspectVertex.id, aspectVertex.baseType, BaseType.Text.name)
            }
            aspectVertex.referenceBookRootVertex?.let { throw RefBookAlreadyExist(aspectId) }

            val rootVertex = dao.createReferenceBookItemVertex()
            rootVertex.value = name

            aspectVertex.addEdge(rootVertex, ASPECT_REFERENCE_BOOK_EDGE).save<OEdge>()
            aspectVertex.save<OVertex>()

            val savedRootVertex = rootVertex.save<OVertex>().toReferenceBookItemVertex()
            historyService.storeFact(savedRootVertex.toCreateFact(context))
            historyService.storeFact(aspectVertex.toUpdateFact(context, aspectBefore))

            return@transaction savedRootVertex
        }.toReferenceBook(aspectId)
    }

    /**
     * Update ReferenceBook name or description
     * @throws RefBookNotExist
     * @throws RefBookEmptyChangeException if no changes are required
     */
    fun updateReferenceBook(book: ReferenceBook, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)

        transaction(db) {
            val aspectId = book.aspectId
            val newName: String = book.name
            val newDescription: String? = book.description

            val rootVertex = dao.getRootVertex(aspectId) ?: throw RefBookNotExist(aspectId)
            if (rootVertex.toReferenceBook(aspectId) == book) {
                throw RefBookEmptyChangeException()
            }
            val before = rootVertex.currentSnapshot()

            rootVertex
                .validateForRemoved()
                .validateVersion(book.toRoot())

            rootVertex.value = newName
            rootVertex.description = newDescription
            rootVertex.save<OVertex>()

            historyService.storeFact(rootVertex.toUpdateFact(HistoryContext(userVertex), before))
        }
    }

    /**
     * Remove ReferenceBook [referenceBook] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and it has linked by Objects child
     * @throws RefBookNotExist
     */
    fun removeReferenceBook(referenceBook: ReferenceBook, username: String, force: Boolean = false) {
        val userVertex = userService.findUserVertexByUsername(username)
        removeReferenceBook(referenceBook, userVertex, force)
    }

    /**
     * Remove ReferenceBook [referenceBook] if it has not linked by Object child
     * or if it has linked by Object child and [force] == true
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and it has linked by Objects child
     * @throws RefBookNotExist
     */
    fun removeReferenceBook(referenceBook: ReferenceBook, userVertex: UserVertex, force: Boolean = false) =
        transaction(db) {
            val aspectId = referenceBook.aspectId
            val aspectVertex = dao.getAspectVertex(aspectId) ?: throw AspectDoesNotExist(aspectId)
            val previous = aspectVertex.currentSnapshot()
            logger.debug("Removing reference book with aspectId: $aspectId by ${userVertex.username}")

            val rootVertex = dao.getRootVertex(aspectId) ?: throw RefBookNotExist(aspectId)
            rootVertex.validateItemAndChildrenVersions(referenceBook.toRoot())

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            when {
                hasChildItemLinkedByObject && force -> {
                    historyService.storeFact(rootVertex.toSoftDeleteFact(HistoryContext(userVertex)))
                    dao.markItemVertexAsDeleted(rootVertex)
                }
                hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
                else -> {
                    historyService.storeFact(rootVertex.toDeleteFact(HistoryContext(userVertex)))
                    dao.removeRefBookItemVertex(rootVertex)
                }
            }

            historyService.storeFact(aspectVertex.toUpdateFact(HistoryContext(userVertex), previous))
        }

    fun getReferenceBookItemVertex(id: String): ReferenceBookItemVertex = transaction(db) {
        logger.debug("Getting ReferenceBookItem id: $id")
        val bookItemVertex = dao.getReferenceBookItemVertex(id)
        return@transaction bookItemVertex ?: throw RefBookItemNotExist(id)
    }

    /**
     * Get ReferenceBookItem by [id]
     * @throws RefBookItemNotExist
     */
    fun getReferenceBookItem(id: String): ReferenceBookItem = transaction(db) {
        getReferenceBookItemVertex(id).toReferenceBookItem()
    }

    /**
     * Add ReferenceBookItem instance to item parent
     * @throws RefBookItemNotExist if parent item doesn't exist
     * @throws RefBookChildAlreadyExist if parent item already has child with the same value
     * @throws RefBookItemIllegalArgumentException if parentId is null
     * @return id of added ReferenceBookItem
     */
    fun addReferenceBookItem(parentId: String, bookItem: ReferenceBookItem, username: String): String {
        val userVertex = userService.findUserVertexByUsername(username)

        return transaction(db) {
            val value = bookItem.value
            val description = bookItem.description

            logger.debug("Adding ReferenceBookItem parentId: $parentId, value: $value by $username")

            val savedItemVertex: ReferenceBookItemVertex
            val parentBefore: Snapshot
            val updateFact: HistoryFact
            val context = HistoryContext(userVertex)

            val parentVertex = dao.getReferenceBookItemVertex(parentId) ?: throw RefBookItemNotExist(parentId)
            parentBefore = parentVertex.currentSnapshot()
            parentVertex.validateValue(value, null)

            val itemVertex = dao.createReferenceBookItemVertex()
            itemVertex.value = value
            itemVertex.description = description

            savedItemVertex = dao.saveBookItemVertex(parentVertex, itemVertex)
            updateFact = parentVertex.toUpdateFact(context, parentBefore)

            historyService.storeFact(savedItemVertex.toCreateFact(context))
            historyService.storeFact(updateFact)
            return@transaction savedItemVertex
        }.id
    }

    /**
     * Update ReferenceBookItem [bookItem] if satisfies all validation constraints
     * TODO: KS-141 - @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] is linked by any object value
     * @throws RefBookItemIllegalArgumentException if parent vertex does not exist
     * @throws RefBookItemNotExist if id in received DTO is illegal
     * @throws RefBookChildAlreadyExist if reference item with the same value as supplied already exists within the parent context
     * @throws RefBookEmptyChangeException if no changes are required
     */
    fun updateReferenceBookItem(bookItem: ReferenceBookItem, username: String, force: Boolean = false) {
        val userVertex = userService.findUserVertexByUsername(username)

        transaction(db) {
            logger.debug("Updating: $bookItem by $username")

            val itemVertex = dao.getReferenceBookItemVertex(bookItem.id) ?: throw RefBookItemNotExist(bookItem.id)
            if (itemVertex.toReferenceBookItem() == bookItem) {
                throw RefBookEmptyChangeException()
            }

            val before = itemVertex.currentSnapshot()

            val parentVertex =
                itemVertex.parent ?: throw RefBookItemIllegalArgumentException("parent vertex must exist")

            with(itemVertex) {
                validateForRemoved()
                validateItemAndChildrenVersions(bookItem)
                if (!force) {
                    validateLinkedByObjects()
                }
            }
            if (itemVertex.value != bookItem.value) {
                parentVertex.validateValue(bookItem.value, bookItem.id)
            }

            itemVertex.value = bookItem.value
            itemVertex.description = bookItem.description
            dao.saveBookItemVertex(parentVertex, itemVertex)

            historyService.storeFact(itemVertex.toUpdateFact(HistoryContext(userVertex), before))
        }
    }

    /**
     * Remove [bookItem] if it has not linked by Object child
     * If it has linked by Object child and [force] == true then mark [bookItem] and its children as deleted
     * @throws RefBookItemHasLinkedEntitiesException if [force] == false and [bookItem] has linked by Objects child
     * @throws RefBookItemIllegalArgumentException if [bookItem] is root
     * @throws RefBookItemNotExist
     */
    fun removeReferenceBookItem(bookItem: ReferenceBookItem, username: String, force: Boolean = false) {
        val userVertex = userService.findUserVertexByUsername(username)
        transaction(db) {
            val itemId = bookItem.id
            logger.debug("Removing ReferenceBookItem id: $itemId by $username")

            val bookItemVertex = dao.getReferenceBookItemVertex(itemId) ?: throw RefBookItemNotExist(itemId)

            bookItemVertex
                .validateItemAndChildrenVersions(bookItem)

            //TODO: checking if children items linked by Objects and set correct itemsWithLinkedObjects!
            val itemsWithLinkedObjects: List<ReferenceBookItem> = emptyList()
            val hasChildItemLinkedByObject = itemsWithLinkedObjects.isNotEmpty()
            when {
                hasChildItemLinkedByObject && force -> {
                    historyService.storeFact(bookItemVertex.toSoftDeleteFact(HistoryContext(userVertex)))
                    dao.markItemVertexAsDeleted(bookItemVertex)
                }
                hasChildItemLinkedByObject -> throw RefBookItemHasLinkedEntitiesException(itemsWithLinkedObjects)
                else -> {
                    historyService.storeFact(bookItemVertex.toDeleteFact(HistoryContext(userVertex)))
                    dao.removeRefBookItemVertex(bookItemVertex)
                }
            }
        }
    }

    /**
     * Make ReferenceBookItem [source] child of ReferenceBookItem [target]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [source] is a parent of [target]
     * @throws RefBookItemIllegalArgumentException if [source] is root
     */
    fun moveReferenceBookItem(source: ReferenceBookItem, target: ReferenceBookItem, username: String) {
        val userVertex = userService.findUserVertexByUsername(username)
        transaction(db) {
            val sourceId = source.id
            val targetId = target.id
            logger.debug("Moving ReferenceBookItem sourceId: $sourceId, targetId: $targetId by $username")

            val sourceVertex = dao.getReferenceBookItemVertex(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = dao.getReferenceBookItemVertex(targetId) ?: throw RefBookItemNotExist(targetId)

            targetVertex
                .validateForRemoved()
                .validateItemAndChildrenVersions(target)

            sourceVertex
                .validateForRemoved()
                .validateItemAndChildrenVersions(source)
                .validateForMoving(targetVertex)

            sourceVertex.getEdges(ODirection.IN, sourceVertex.edgeName).forEach {
                it.delete<OEdge>()
                historyService.storeFact(it.from.toReferenceBookItemVertex().toDeleteFact(HistoryContext(userVertex)))
            }
            targetVertex.addEdge(sourceVertex, sourceVertex.edgeName).save<ORecord>()
            historyService.storeFact(targetVertex.toCreateFact(HistoryContext(userVertex)))
        }
    }

    private fun ReferenceBook.toRoot() = ReferenceBookItem(
        id = id,
        value = name,
        description = description,
        children = children,
        deleted = deleted,
        version = version
    )

    private fun ReferenceBookItemVertex.toReferenceBook(aspectId: String) = ReferenceBook(
        aspectId = aspectId,
        id = id,
        name = value,
        description = description,
        children = toReferenceBookItem().children,
        deleted = deleted,
        version = version
    )

    private fun AspectVertex.validateForRemoved() =
        this.also { if (it.deleted) throw AspectDoesNotExist(it.id) }

    private fun ReferenceBookItemVertex.validateForRemoved(): ReferenceBookItemVertex =
        this.also { validator.checkForBookItemRemoved(this) }

    private fun ReferenceBookItemVertex.validateItemAndChildrenVersions(bookItem: ReferenceBookItem): ReferenceBookItemVertex =
        this.also { validator.checkRefBookItemAndChildrenVersions(this, bookItem) }

    private fun ReferenceBookItemVertex.validateForMoving(targetVertex: ReferenceBookItemVertex): ReferenceBookItemVertex =
        this.also { validator.checkForMoving(this, targetVertex) }

    private fun ReferenceBookItemVertex.validateValue(value: String, id: String?): ReferenceBookItemVertex =
        this.also { validator.checkRefBookItemValue(this, value, id) }

    private fun ReferenceBookItemVertex.validateVersion(bookItem: ReferenceBookItem): ReferenceBookItemVertex =
        this.also { validator.checkRefBookItemVersion(this, bookItem) }

    private fun ReferenceBookItemVertex.validateLinkedByObjects(): ReferenceBookItemVertex =
        this // TODO: KS-141 - Check if the vertex is referenced as value from any object

}

private val logger = loggerFor<ReferenceBookService>()

sealed class ReferenceBookException(message: String? = null) : Exception(message)
class RefBookAspectNotFoundException(id: String) : ReferenceBookException("id: $id")
class RefBookAlreadyExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookNotExist(val aspectId: String) : ReferenceBookException("aspectId: $aspectId")
class RefBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
class RefBookChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
class RefBookItemMoveImpossible(sourceId: String, targetId: String) :
    ReferenceBookException("sourceId: $sourceId, targetId: $targetId")

class RefBookItemIllegalArgumentException(message: String) : ReferenceBookException(message)

class RefBookItemHasLinkedEntitiesException(val itemsWithLinkedObjects: List<ReferenceBookItem>) :
    ReferenceBookException("${itemsWithLinkedObjects.map { it.id }}")

class RefBookConcurrentModificationException(id: String, message: String) :
    ReferenceBookException("id: $id, message: $message")

class RefBookIncorrectAspectType(aspectId: String, type: String?, expected: String) :
    ReferenceBookException("Bad type of aspect $aspectId: $type. Expected: $expected")

class RefBookEmptyChangeException : ReferenceBookException()