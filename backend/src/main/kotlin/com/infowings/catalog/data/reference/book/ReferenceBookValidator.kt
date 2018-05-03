package com.infowings.catalog.data.reference.book


import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.id

/**
 * Class for validating reference books and reference book items.
 * Methods should be called in transaction
 */
class ReferenceBookValidator(private val dao: ReferenceBookDao) {

    fun checkRefBookAndItemsVersions(bookVertex: ReferenceBookVertex, book: ReferenceBook) {
        checkRefBookVersion(bookVertex, book)

        val idToItemVertexMapFromBookItemVertices = bookVertex.itemVertices.map { it.id to it }.toMap()
        val idToItemVertexMapFromBookItems = book.children.map { it.id to it }.toMap()

        val changedIds = idToItemVertexMapFromBookItemVertices
            .filter { (id, itemVertex) -> itemVertex.version != idToItemVertexMapFromBookItems[id]?.version }
            .map { (id, _) -> id }

        if (changedIds.isNotEmpty()) {
            throw RefBookItemConcurrentModificationException(changedIds.joinToString(), "ReferenceBookItems changed.")
        }

        idToItemVertexMapFromBookItemVertices
            .forEach { (id, itemVertex) ->
                checkRefBookItemAndChildrenVersions(itemVertex, idToItemVertexMapFromBookItems[id]!!)
            }
    }

    fun checkRefBookVersion(bookVertex: ReferenceBookVertex, book: ReferenceBook) {
        if (bookVertex.version != book.version) {
            throw RefBookConcurrentModificationException(book.aspectId, "ReferenceBook changed.")
        }
    }

    fun checkRefBookItemAndChildrenVersions(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
        checkRefBookItemVersion(bookItemVertex, bookItem)
        checkChildrenVersions(bookItemVertex, bookItem)
    }

    private fun checkRefBookItemVersion(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
        if (bookItemVertex.version != bookItem.version) {
            throw RefBookItemConcurrentModificationException(bookItem.id, "ReferenceBookItem changed.")
        }
    }

    private fun checkChildrenVersions(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
        val realVersionMap = idToVersionMapFromBookItemVertex(bookItemVertex)
        val receivedVersionMap = idToVersionMapFromBookItem(bookItem)

        if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
            throw RefBookItemConcurrentModificationException(bookItem.id, "ReferenceBookItem child changed.")
        }

        val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
        if (different) {
            throw RefBookItemConcurrentModificationException(bookItem.id, "ReferenceBookItem child changed.")
        }
    }

    fun checkRefBookItemValue(parentVertex: ReferenceBookItemVertex, value: String, id: String?) {
        val vertexWithSameValueAlreadyExist =
            parentVertex.children.any { it.value == value && it.id != id && it.deleted.not() }
        if (vertexWithSameValueAlreadyExist) {
            throw RefBookChildAlreadyExist(parentVertex.id, value)
        }
    }

    fun checkRefBookItemValue(refBookVertex: ReferenceBookVertex, value: String, id: String?) {
        val vertexWithSameValueAlreadyExist =
            refBookVertex.itemVertices.any { it.value == value && it.id != id && it.deleted.not() }
        if (vertexWithSameValueAlreadyExist) {
            throw RefBookChildAlreadyExist(refBookVertex.id, value)
        }
    }

    fun checkForBookRemoved(bookVertex: ReferenceBookVertex) {
        if (bookVertex.deleted) {
            throw RefBookNotExist(bookVertex.id)
        }
    }

    fun checkForBookItemRemoved(bookItemVertex: ReferenceBookItemVertex) {
        if (bookItemVertex.deleted) {
            throw RefBookItemNotExist(bookItemVertex.id)
        }
    }

    fun checkForMoving(sourceVertex: ReferenceBookItemVertex, targetVertex: ReferenceBookItemVertex) {
        val sourceId = sourceVertex.id
        val targetId = targetVertex.id
        if (dao.getRefBookItemVertexParents(targetId).map { it.id }.contains(sourceId)) {
            throw RefBookItemMoveImpossible(sourceId, targetId)
        }
    }
}

private fun idToVersionMapFromBookItemVertex(itemVertex: ReferenceBookItemVertex): HashMap<String, Int> {
    val map = HashMap<String, Int>()
    for (child in itemVertex.children) {
        map[child.id] = child.version
        map.putAll(idToVersionMapFromBookItemVertex(child))
    }
    return map
}

private fun idToVersionMapFromBookItem(item: ReferenceBookItem): HashMap<String, Int> {
    val map = HashMap<String, Int>()
    for (child in item.children) {
        map[child.id] = child.version
        map.putAll(idToVersionMapFromBookItem(child))
    }
    return map
}