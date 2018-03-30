package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAllReferenceBooks(): ReferenceBooksList =
    JSON.parse(get("/api/book/all"))

internal suspend fun getReferenceBook(aspectId: String): ReferenceBook =
    JSON.parse(get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun createReferenceBook(book: ReferenceBook): ReferenceBook =
    JSON.parse(post("/api/book/create", JSON.stringify(book)))

internal suspend fun updateReferenceBook(book: ReferenceBook): ReferenceBook =
    JSON.parse(post("/api/book/update", JSON.stringify(book)))

internal suspend fun createReferenceBookItem(bookItem: ReferenceBookItem): ReferenceBook =
    JSON.parse(post("/api/book/item/create", JSON.stringify(bookItem)))

internal suspend fun updateReferenceBookItem(bookItem: ReferenceBookItem): ReferenceBook =
    JSON.parse(post("/api/book/item/update", JSON.stringify(bookItem)))