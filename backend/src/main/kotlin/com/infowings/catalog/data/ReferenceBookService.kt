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

interface ReferenceBookService {

    /**
     * Get ReferenceBook instance by name
     * @throws RefBookNotExist
     * */
    fun getReferenceBook(name: String): ReferenceBook

    /**
     * Get ReferenceBookItem by id
     * @throws RefBookItemNotExist
     * */
    fun getReferenceBookItem(id: String): ReferenceBookItem

    /**
     * Change value of ReferenceBookItem with id [id]
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     * */
    fun changeValue(id: String, value: String)

    /**
     * Save ReferenceBook with name [name]
     * @throws RefBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook

    /**
     * Add new item as a child to ReferenceBookItem
     *
     * @return id of created item
     *
     * @throws RefBookItemNotExist
     * @throws RefBookChildAlreadyExist
     */
    fun addReferenceBookItem(parentId: String, value: String): String

    /**
     * Make ReferenceBookItem with id [sourceId] to be a child of ReferenceBookItem with id [targetId]
     * @throws RefBookItemNotExist
     * @throws RefBookItemMoveImpossible in case of [sourceId] is a parent pf [targetId]
     */
    fun moveReferenceBookItem(sourceId: String, targetId: String)
}

class DefaultReferenceBookService(val database: OrientDatabase) : ReferenceBookService {

    override fun getReferenceBookItem(id: String): ReferenceBookItem {
        val rootVertex = database.getVertexById(id) ?: throw RefBookItemNotExist(id)
        return getReferenceBookItem(rootVertex)
    }

    override fun getReferenceBook(name: String): ReferenceBook = session(database) {
        val referenceBookVertex = getReferenceBookVertexByName(name) ?: throw RefBookNotExist(name)
        val root = getReferenceBookItem(referenceBookVertex.child!!)
        val aspectId = referenceBookVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).first()
                ?: throw RefBookAspectNotExist(name)
        return@session ReferenceBook(name, aspectId.id, root)
    }

    override fun changeValue(id: String, value: String) {
        transaction(database) {
            val vertex = database.getVertexById(id) ?: throw RefBookItemNotExist(id)
            val parentVertex = vertex.parent!!
            val vertexWithSameNameAlreadyExist = parentVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE)
                    .any { it.get<String>("value") == value && it.id != id }

            if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX && vertexWithSameNameAlreadyExist) {
                throw RefBookChildAlreadyExist(parentVertex.id, value)
            }

            vertex["value"] = value
            return@transaction vertex.save<OVertex>()
        }
    }

    override fun addReferenceBookItem(parentId: String, value: String): String = transaction(database) { session ->
        val parentVertex = database.getVertexById(parentId) ?: throw RefBookItemNotExist(parentId)

        if (parentVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).any { it.get<String>("value") == value }) {
            throw RefBookChildAlreadyExist(parentId, value)
        }

        val childVertex = session.newVertex(REFERENCE_BOOK_ITEM_VERTEX)
        parentVertex.addEdge(childVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        childVertex["value"] = value
        return@transaction childVertex.save<OVertex>()
    }.id


    override fun moveReferenceBookItem(sourceId: String, targetId: String) {
        transaction(database) {
            val sourceVertex = database.getVertexById(sourceId) ?: throw RefBookItemNotExist(sourceId)
            val targetVertex = database.getVertexById(targetId) ?: throw RefBookItemNotExist(targetId)

            var tmpPointer = targetVertex
            while (!tmpPointer.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).none()) {
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

    override fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(database) { session ->
        if (getReferenceBookVertexByName(name) != null) {
            throw RefBookAlreadyExist(name)
        }
        val referenceBookVertex = session.newVertex(REFERENCE_BOOK_VERTEX)
        referenceBookVertex["name"] = name
        val rootVertex = session.newVertex(REFERENCE_BOOK_ITEM_VERTEX)
        rootVertex["value"] = "root"
        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        val aspectVertex = database.getVertexById(aspectId)
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()
        return@transaction Pair(referenceBookVertex.save<OVertex>(), rootVertex.save<OVertex>())
    }.let { ReferenceBook(it.first["name"], aspectId, ReferenceBookItem(it.second.id, it.second["value"])) }


    private fun getReferenceBookItem(vertex: OVertex): ReferenceBookItem = session(database) {
        val value = vertex.get<String>("value")
        val children = vertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).map { getReferenceBookItem(it) }
        return@session ReferenceBookItem(vertex.id, value, children)
    }

    private fun getReferenceBookVertexByName(name: String): OVertex? =
            database.query(searchReferenceBookByName, name) { it.map { it.toVertexOrNUll() }.firstOrNull() }
}

private val OVertex.child: OVertex?
    get() = getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).first()
private val OVertex.parent: OVertex?
    get() = getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).first()

sealed class ReferenceBookException(message: String? = null) : Exception(message)
class RefBookAlreadyExist(val name: String) : ReferenceBookException("name: $name")
class RefBookNotExist(val name: String) : ReferenceBookException("name: $name")
class RefBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
class RefBookChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
class RefBookAspectNotExist(val name: String) : ReferenceBookException("name: $name")
object RefBookItemMoveImpossible : ReferenceBookException()

private const val searchReferenceBookByName = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE name = ?"