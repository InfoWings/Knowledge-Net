package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.*

import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex


class ReferenceBookService(val database: OrientDatabase, val daoService: ReferenceBookDaoService) {

    /**
     * Get all ReferenceBook instances
     */
    fun getAllReferenceBooks(): List<ReferenceBook> = transaction(database) {
        return@transaction database.query(selectFromReferenceBook) { rs ->
            rs.mapNotNull { it.toVertexOrNUll()?.toReferenceBook() }.toList()
        }
    }

    /**
     * Get ReferenceBook instance by aspect id
     * @throws RefBookNotExist
     */
    fun getReferenceBook(aspectId: String): ReferenceBook = transaction(database) {
        val referenceBookVertex = daoService.findReferenceBookVertexByAspectId(aspectId)
        return@transaction referenceBookVertex.toReferenceBook()
    }

    /**
     * Create ReferenceBook with name [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(database) {
        daoService.noReferenceBookVertexByAspectId(aspectId)

        val referenceBookVertex = daoService.newReferenceBookVertex()
        referenceBookVertex.aspectId = aspectId
        referenceBookVertex.name = name

        val rootVertex = daoService.newReferenceBookItemVertex()
        rootVertex.value = "root"

        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()

        val aspectVertex = database.getVertexById(aspectId)
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()

        return@transaction Pair(referenceBookVertex.save<OVertex>(), rootVertex.save<OVertex>())
    }.let { ReferenceBook(it.first.toReferenceBookVertex().name, aspectId, ReferenceBookItem(it.second.id, it.second.toReferenceBookItemVertex().value)) }

    /**
     * Change ReferenceBook name to [newName] where aspect id equals to [aspectId]
     * @throws RefBookNotExist if ReferenceBook with [aspectId] not found
     * */
    fun updateReferenceBook(aspectId: String, newName: String) = transaction(database) {
        val referenceBookVertex = daoService.findReferenceBookVertexByAspectId(aspectId)
        referenceBookVertex.name = newName
        return@transaction referenceBookVertex.save<OVertex>().toReferenceBook()
    }

    /**
     * Get ReferenceBookItem by id
     * @throws RefBookItemNotExist
     */
    fun getReferenceBookItem(id: String): ReferenceBookItem = transaction(database) {
        val rootVertex = database.getVertexById(id) ?: throw RefBookItemNotExist(id)
        return@transaction rootVertex.toReferenceBookItem()
    }

    /**
     * Add ReferenceBookItem instance to item with id = [parentId]
     * @throws RefBookItemNotExist if item with id [parentId] doesn't exist
     * @throws RefBookChildAlreadyExist if item with id [parentId] already has child with value equals to [value]
     */
    internal fun addReferenceBookItem(parentId: String, value: String): String = transaction(database) {
        val parentVertex = daoService.findReferenceBookItemVertex(parentId)

        if (parentVertex.children.any { it.toReferenceBookItemVertex().value == value }) {
            throw RefBookChildAlreadyExist(parentId, value)
        }

        val childVertex = daoService.newReferenceBookItemVertex()
        parentVertex.addEdge(childVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        childVertex.value = value

        return@transaction childVertex.save<OVertex>()
    }.id

    /**
     * Change value of ReferenceBookItem with id [id]
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     */
    internal fun changeValue(id: String, value: String) {
        transaction(database) {

            val vertex = daoService.findReferenceBookItemVertex(id)
            val parentVertex = vertex.parent?.toReferenceBookItemVertex() ?: throw RefBookParentNotFound(vertex)
            val vertexWithSameNameAlreadyExist = parentVertex.children.any { it.toReferenceBookItemVertex().value == value && it.id != id }

            if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX && vertexWithSameNameAlreadyExist) {
                throw RefBookChildAlreadyExist(parentVertex.id, value)
            }

            vertex.value = value
            return@transaction vertex.save<OVertex>()
        }
    }

    fun addItemAndGetReferenceBook(aspectId: String, parentId: String, value: String): ReferenceBook {
        addReferenceBookItem(parentId, value)
        return getReferenceBook(aspectId)
    }

    fun updateItemAndGetReferenceBook(aspectId: String, id: String, newValue: String): ReferenceBook {
        changeValue(id, newValue)
        return getReferenceBook(aspectId)
    }

    /**
     * Make ReferenceBookItem with id [sourceId] to be a child of ReferenceBookItem with id [targetId]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [sourceId] is a parent pf [targetId]
     */
    fun moveReferenceBookItem(sourceId: String, targetId: String) {
        transaction(database) {
            val sourceVertex = daoService.findReferenceBookItemVertex(sourceId)
            val targetVertex = daoService.findReferenceBookItemVertex(targetId)

            var tmpPointer = targetVertex
            while (tmpPointer.parent != null) {
                val parent = tmpPointer.parent ?: throw RefBookParentNotFound(tmpPointer)
                if (tmpPointer.id == sourceId) {
                    throw RefBookItemMoveImpossible(sourceId, targetId)
                }
                tmpPointer = parent.toReferenceBookItemVertex()
            }

            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }
            return@transaction targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
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
class RefBookParentNotFound(val vertex: ReferenceBookItemVertex) :
    ReferenceBookException("Not found parent for item ${vertex.id}")


private const val selectFromReferenceBook = "SELECT FROM $REFERENCE_BOOK_VERTEX"
