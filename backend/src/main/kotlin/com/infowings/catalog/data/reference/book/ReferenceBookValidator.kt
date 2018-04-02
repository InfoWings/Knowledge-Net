package com.infowings.catalog.data.reference.book


import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.id

class ReferenceBookValidator {

    fun checkRefBookAndItemsVersion(bookVertex: ReferenceBookVertex, book: ReferenceBook) {
        checkRefBookVersion(bookVertex, book)
        checkRefBookItemAndChildrenVersion(bookVertex.root, book.root)
    }

    private fun checkRefBookVersion(bookVertex: ReferenceBookVertex, book: ReferenceBook) {
        if (bookVertex.version != book.version) {
            throw RefBookConcurrentModificationException(book.id, "ReferenceBook changed.")
        }
    }

    fun checkRefBookItemAndChildrenVersion(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
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
        val vertexWithSameNameAlreadyExist = parentVertex.children.any { it.value == value && it.id != id }
        if (parentVertex.schemaType.get().name == REFERENCE_BOOK_ITEM_VERTEX && vertexWithSameNameAlreadyExist) {
            throw RefBookChildAlreadyExist(parentVertex.id, value)
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
        var tmpPointer = targetVertex
        while (tmpPointer.parent != null) {
            val parent = tmpPointer.parent!!
            if (tmpPointer.id == sourceVertex.id) {
                throw RefBookItemMoveImpossible(sourceVertex.id, targetVertex.id)
            }
            tmpPointer = parent
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