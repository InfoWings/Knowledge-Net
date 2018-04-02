package com.infowings.catalog.data.reference.book


import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.storage.id

class ReferenceBookValidator(private val dao: ReferenceBookDao) {
    fun checkRefBookItemAndChildrenVersion(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
        checkRefBookItemVersion(bookItemVertex, bookItem)
        checkRefBookChildrenVersions(bookItemVertex, bookItem)
    }

    fun checkRefBookItemVersion(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
        if (bookItemVertex.version != bookItem.version) {
            throw RefBookItemConcurrentModificationException(bookItem.id, "ReferenceBookItem changed.")
        }
    }

    private fun checkRefBookChildrenVersions(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) {
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