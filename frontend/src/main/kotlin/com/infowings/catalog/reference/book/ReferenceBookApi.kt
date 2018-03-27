package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAllReferenceBooks(): ReferenceBooksList =
    JSON.parse(get("/api/book/all"))

internal suspend fun getReferenceBook(aspectId: String): ReferenceBook =
    JSON.parse(get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun createReferenceBook(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/create", JSON.stringify(bookData)))

internal suspend fun updateReferenceBook(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/update", JSON.stringify(bookData)))

internal suspend fun createReferenceBookItem(bookItemData: ReferenceBookItemData): ReferenceBook =
    JSON.parse(post("/api/book/item/create", JSON.stringify(bookItemData)))

internal suspend fun updateReferenceBookItem(bookItemData: ReferenceBookItemData): ReferenceBook =
    JSON.parse(post("/api/book/item/update", JSON.stringify(bookItemData)))