package com.infowings.catalog.data

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


class ReferenceBookService(val database: OrientDatabase) {

    /**
     * Get ReferenceBookItem by id
     * @throws RefBookItemNotExist
     * */
    fun getReferenceBookItem(id: String): ReferenceBookItem = transaction(database) {
        val rootVertex = database.getVertexById(id) ?: throw RefBookItemNotExist(id)
        return@transaction rootVertex.toReferenceBookItem()
    }

    /**
     * Get ReferenceBook instance by name
     * @throws RefBookNotExist
     * */
    fun getReferenceBook(name: String): ReferenceBook = transaction(database) {
        val referenceBookVertex = getReferenceBookVertexByName(name) ?: throw RefBookNotExist(name)
        return@transaction referenceBookVertex.toReferenceBook()
    }

    /**
     * Get all ReferenceBook instances
     * */
    fun getReferenceBooks(): List<ReferenceBook> = transaction(database) {
        return@transaction database.query(selectFromReferenceBook) { rs ->
            rs.mapNotNull { it.toVertexOrNUll()?.toReferenceBook() }.toList()
        }
    }

    /**
     * Get ReferenceBook instances by aspect id
     * */
    fun getReferenceBooksByAspectId(aspectId: String): List<ReferenceBook> {
        return getReferenceBooks().filter { it.aspectId == aspectId }.toList()
    }

    /**
     * Change value of ReferenceBookItem with id [id]
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     * */
    fun changeValue(id: String, value: String) {
        transaction(database) {

            val vertex = database.getVertexById(id) ?: throw RefBookItemNotExist(id)
            val parentVertex = vertex.parent!!
            val vertexWithSameNameAlreadyExist = parentVertex.children.any { it.value == value && it.id != id }

            if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX && vertexWithSameNameAlreadyExist) {
                throw RefBookChildAlreadyExist(parentVertex.id, value)
            }

            vertex["value"] = value
            return@transaction vertex.save<OVertex>()
        }
    }

    fun addReferenceBookItem(parentId: String, value: String): String = transaction(database) {
        val parentVertex = database.getVertexById(parentId) ?: throw RefBookItemNotExist(parentId)

        if (parentVertex.children.any { it.value == value }) {
            throw RefBookChildAlreadyExist(parentId, value)
        }

        val childVertex = database.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX)
        parentVertex.addEdge(childVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        childVertex["value"] = value

        return@transaction childVertex.save<OVertex>()
    }.id

    fun addItemAndGetReferenceBook(bookName: String, parentId: String, value: String): ReferenceBook {
        addReferenceBookItem(parentId, value)
        return getReferenceBook(bookName)
    }

    /**
     * Make ReferenceBookItem with id [sourceId] to be a child of ReferenceBookItem with id [targetId]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [sourceId] is a parent pf [targetId]
     */
    fun moveReferenceBookItem(sourceId: String, targetId: String) {
        transaction(database) {
            val sourceVertex = database.getVertexById(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = database.getVertexById(targetId) ?: throw RefBookItemNotExist(targetId)

            var tmpPointer = targetVertex
            while (tmpPointer.parent != null) {
                val parent = tmpPointer.parent!!
                if (tmpPointer.id == sourceId) {
                    throw RefBookItemMoveImpossible
                }
                tmpPointer = parent
            }

            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }
            return@transaction targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
        }
    }

    /**
     * Save ReferenceBook with name [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(database) {
        getReferenceBookVertexByName(name)?.let { throw RefBookAlreadyExist(name) }

        val referenceBookVertex = database.createNewVertex(REFERENCE_BOOK_VERTEX)
        referenceBookVertex["name"] = name

        val rootVertex = database.createNewVertex(REFERENCE_BOOK_ITEM_VERTEX)
        rootVertex["value"] = "root"

        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()

        val aspectVertex = database.getVertexById(aspectId)
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()

        return@transaction Pair(referenceBookVertex.save<OVertex>(), rootVertex.save<OVertex>())
    }.let { ReferenceBook(it.first["name"], aspectId, ReferenceBookItem(it.second.id, it.second["value"])) }


    /**
     * Change ReferenceBook's [oldName] to [newName]
     * @throws RefBookNotExist if ReferenceBook with [oldName] not found
     * @throws RefBookAlreadyExist if exists ReferenceBook with [newName]
     * */
    fun updateReferenceBook(oldName: String, newName: String) = transaction(database) {
        val referenceBookVertex = getReferenceBookVertexByName(oldName) ?: throw RefBookNotExist(oldName)
        getReferenceBookVertexByName(newName)?.let { throw RefBookAlreadyExist(newName) }
        referenceBookVertex["name"] = newName
        return@transaction referenceBookVertex.save<OVertex>().toReferenceBook()
    }

    private fun getReferenceBookVertexByName(name: String): OVertex? =
        database.query(searchReferenceBookByName, name) { it.map { it.toVertexOrNUll() }.firstOrNull() }

    private fun OVertex.toReferenceBook(): ReferenceBook {
        val aspectId = aspect?.id ?: throw RefBookAspectNotExist(name)
        val root = child!!.toReferenceBookItem()
        return ReferenceBook(name, aspectId, root)
    }

    private fun OVertex.toReferenceBookItem(): ReferenceBookItem {
        val children = children.map { it.toReferenceBookItem() }
        return ReferenceBookItem(id, value, children)
    }
}

sealed class ReferenceBookException(message: String? = null) : Exception(message)
class RefBookAlreadyExist(val name: String) : ReferenceBookException("name: $name")
class RefBookNotExist(val name: String) : ReferenceBookException("name: $name")
class RefBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
class RefBookChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
class RefBookAspectNotExist(val name: String) : ReferenceBookException("name: $name")
object RefBookItemMoveImpossible : ReferenceBookException()

private const val searchReferenceBookByName = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE name = ?"
private const val selectFromReferenceBook = "SELECT FROM $REFERENCE_BOOK_VERTEX"

private val OVertex.name: String
    get() = this["name"]
private val OVertex.value: String
    get() = this["value"]
private val OVertex.children: List<OVertex>
    get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).toList()
private val OVertex.child: OVertex?
    get() = children.first()
private val OVertex.aspect: OVertex?
    get() = getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).firstOrNull()
private val OVertex.parent: OVertex?
    get() = getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).firstOrNull()