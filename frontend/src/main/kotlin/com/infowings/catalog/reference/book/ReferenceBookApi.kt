package com.infowings.catalog.reference.book

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAllReferenceBooks(): ReferenceBooksList =
    JSON.parse(ReferenceBooksList.serializer(), get("/api/book/all"))

internal suspend fun getReferenceBook(aspectId: String): ReferenceBook =
    JSON.parse(ReferenceBook.serializer(), get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun getReferenceBookById(refBookId: String): ReferenceBook =
    JSON.parse(ReferenceBook.serializer(), get("/api/book/${encodeURIComponent(refBookId)}"))

internal suspend fun createReferenceBook(book: ReferenceBook): ReferenceBook =
    JSON.parse(ReferenceBook.serializer(), post("/api/book/create", JSON.stringify(ReferenceBook.serializer(), book)))

internal suspend fun updateReferenceBook(book: ReferenceBook) {
    post("/api/book/update", JSON.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun deleteReferenceBook(book: ReferenceBook) {
    post("/api/book/remove", JSON.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun forceDeleteReferenceBook(book: ReferenceBook) {
    post("/api/book/forceRemove", JSON.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun createReferenceBookItem(data: ReferenceBookItemData) {
    post("/api/book/item/create", JSON.stringify(ReferenceBookItemData.serializer(), data))
}

internal suspend fun updateReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/update", JSON.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun forceUpdateReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/forceUpdate", JSON.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun deleteReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/remove", JSON.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun forceDeleteReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/forceRemove", JSON.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun getReferenceBookItemPath(itemId: String): ReferenceBookItemPath =
    JSON.parse(ReferenceBookItemPath.serializer(), get("/api/book/item/path?itemId=${encodeURIComponent(itemId)}"))