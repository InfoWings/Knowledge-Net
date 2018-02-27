package com.infowings.catalog.data

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
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
     * @throws ReferenceBookException.ReferenceBookNotExist
     * */
    fun getReferenceBook(name: String): ReferenceBook

    /**
     * Get ReferenceBookItem by id
     * @throws ReferenceBookException.ReferenceBookItemNotExist
     * */
    fun getReferenceBookItem(id: String): ReferenceBookItem

    /**
     * Change value of ReferenceBookItem with id [id]
     * @throws ReferenceBookException.ReferenceBookItemNotExist
     * */
    fun changeValue(id: String, value: String)

    /**
     * @throws ReferenceBookException.ReferenceBookAlreadyExist
     */
    fun createReferenceBook(name: String, aspectId: String): ReferenceBook

    fun addReferenceBookItem(parentId: String, value: String): String
    fun moveReferenceBookItem(sourceId: String, targetId: String)
}

class DefaultReferenceBookService(val database: OrientDatabase, val aspectService: AspectService) : ReferenceBookService {

    override fun getReferenceBook(name: String): ReferenceBook = session(database) {
        val referenceBookVertex = getReferenceBookVertexByName(name)
                ?: throw ReferenceBookException.ReferenceBookNotExist(name)
        val rootVertex = referenceBookVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).first()
        val root = getReferenceBookItem(rootVertex.identity.toString())
        val aspectId = referenceBookVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_ASPECT_EDGE).first()
                ?: throw ReferenceBookException.LinkedAspectNotExist(name)
        return@session ReferenceBook(name, aspectId.identity.toString(), root)
    }

    override fun changeValue(id: String, value: String) {
        session(database) {
            val vertex = getVertexById(id) ?: throw ReferenceBookException.ReferenceBookItemNotExist(id)
            val parentVertex = vertex.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).first()
            if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX
                    && parentVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE)
                            .any { it.get<String>("value") == value && it.identity.toString() != id }) {
                throw ReferenceBookException.ChildAlreadyExist(parentVertex.identity.toString(), value)
            }
            vertex["value"] = value
            vertex.save<OVertex>()
        }
    }

    override fun addReferenceBookItem(parentId: String, value: String): String = transaction(database) {
        val parentVertex = getVertexById(parentId)
                ?: throw ReferenceBookException.ReferenceBookItemNotExist(parentId)
        if (parentVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).any { it.get<String>("value") == value })
            throw ReferenceBookException.ChildAlreadyExist(parentId, value)
        val childVertex = it.newVertex(REFERENCE_BOOK_ITEM_VERTEX)
        parentVertex.addEdge(childVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        childVertex["value"] = value
        childVertex.save<OVertex>()
    }.identity.toString()


    override fun moveReferenceBookItem(sourceId: String, targetId: String) {
        transaction(database) {
            val sourceVertex = getVertexById(sourceId)
                    ?: throw ReferenceBookException.ReferenceBookItemNotExist(sourceId)
            val targetVertex = getVertexById(targetId)
                    ?: throw ReferenceBookException.ReferenceBookItemNotExist(targetId)
            var tmpPointer = targetVertex
            while (!tmpPointer.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).none()) {
                val parent = tmpPointer.getVertices(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).first()
                if (tmpPointer.identity.toString() == sourceId) {
                    throw ReferenceBookException.MoveImpossible
                }
                tmpPointer = parent
            }
            sourceVertex.getEdges(ODirection.IN, REFERENCE_BOOK_CHILD_EDGE).forEach { it.delete<OEdge>() }
            targetVertex.addEdge(sourceVertex, REFERENCE_BOOK_CHILD_EDGE).save<ORecord>()
        }
    }

    override fun createReferenceBook(name: String, aspectId: String): ReferenceBook = transaction(database) {
        if (getReferenceBookVertexByName(name) != null) {
            throw ReferenceBookException.ReferenceBookAlreadyExist(name)
        }
        val referenceBookVertex = it.newVertex(REFERENCE_BOOK_VERTEX)
        referenceBookVertex["name"] = name
        val rootVertex = it.newVertex(REFERENCE_BOOK_ITEM_VERTEX)
        rootVertex["value"] = "root"
        referenceBookVertex.addEdge(rootVertex, REFERENCE_BOOK_CHILD_EDGE).save<OEdge>()
        val aspectVertex = aspectService.getVertexById(aspectId)
        referenceBookVertex.addEdge(aspectVertex, REFERENCE_BOOK_ASPECT_EDGE).save<OEdge>()
        rootVertex.save<OVertex>()
        referenceBookVertex.save<OVertex>()
        Pair(referenceBookVertex, rootVertex)
    }.let { ReferenceBook(it.first["name"], aspectId, ReferenceBookItem(it.second.identity.toString(), it.second["value"])) }


    override fun getReferenceBookItem(id: String): ReferenceBookItem = session(database) {
        val rootVertex = getVertexById(id) ?: throw ReferenceBookException.ReferenceBookItemNotExist(id)
        val value = rootVertex.get<String>("value")
        val children = rootVertex.getVertices(ODirection.OUT, REFERENCE_BOOK_CHILD_EDGE).map { getReferenceBookItem(it.identity.toString()) }
        ReferenceBookItem(id, value, children)
    }

    private fun getVertexById(id: String): OVertex? =
            database.query(selectById, ORecordId(id)) { it.map { it.toVertexOrNUll() }.firstOrNull() }

    private fun getReferenceBookVertexByName(name: String): OVertex? =
            database.query(searchReferenceBookByName, name) { it.map { it.toVertexOrNUll() }.firstOrNull() }
}

sealed class ReferenceBookException(message: String? = null) : Exception(message) {
    class ReferenceBookAlreadyExist(val name: String) : ReferenceBookException("name: $name")
    class ReferenceBookNotExist(val name: String) : ReferenceBookException("name: $name")
    class ReferenceBookItemNotExist(val id: String) : ReferenceBookException("id: $id")
    class ChildAlreadyExist(val id: String, val value: String) : ReferenceBookException("id: $id, value: $value")
    class LinkedAspectNotExist(val name: String) : ReferenceBookException("name: $name")
    object MoveImpossible : ReferenceBookException()
}

private const val searchReferenceBookByName = "SELECT * FROM $REFERENCE_BOOK_VERTEX WHERE name = ?"
private const val selectById = "SELECT FROM ?"