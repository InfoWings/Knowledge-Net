package com.infowings.catalog.reference.book

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.Json

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAllReferenceBooks(): ReferenceBooksList =
    Json.parse(ReferenceBooksList.serializer(), get("/api/book/all"))

internal suspend fun getReferenceBook(aspectId: String): ReferenceBook =
    Json.parse(ReferenceBook.serializer(), get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun getReferenceBookById(refBookId: String): ReferenceBook =
    Json.parse(ReferenceBook.serializer(), get("/api/book/${encodeURIComponent(refBookId)}"))

internal suspend fun createReferenceBook(book: ReferenceBook): ReferenceBook =
    Json.parse(ReferenceBook.serializer(), post("/api/book/create", Json.stringify(ReferenceBook.serializer(), book)))

internal suspend fun updateReferenceBook(book: ReferenceBook) {
    post("/api/book/update", Json.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun deleteReferenceBook(book: ReferenceBook) {
    post("/api/book/remove", Json.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun forceDeleteReferenceBook(book: ReferenceBook) {
    post("/api/book/forceRemove", Json.stringify(ReferenceBook.serializer(), book))
}

internal suspend fun createReferenceBookItem(data: ReferenceBookItemData) {
    post("/api/book/item/create", Json.stringify(ReferenceBookItemData.serializer(), data))
}

internal suspend fun updateReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/update", Json.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun forceUpdateReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/forceUpdate", Json.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun deleteReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/remove", Json.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun forceDeleteReferenceBookItem(bookItem: ReferenceBookItem) {
    post("/api/book/item/forceRemove", Json.stringify(ReferenceBookItem.serializer(), bookItem))
}

internal suspend fun getReferenceBookItemPath(itemId: String): ReferenceBookItemPath =
    Json.parse(ReferenceBookItemPath.serializer(), get("/api/book/item/path?itemId=${encodeURIComponent(itemId)}"))