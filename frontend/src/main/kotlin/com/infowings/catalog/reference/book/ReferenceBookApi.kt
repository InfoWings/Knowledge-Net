package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAllBooks(): ReferenceBooksList =
    JSON.parse(get("/api/book/all"))

internal suspend fun getBookByAspectId(aspectId: String): ReferenceBooksList =
    JSON.parse(get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun getBookByName(name: String): ReferenceBook =
    JSON.parse(get("/api/book/get/$name"))

internal suspend fun createBook(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/create", JSON.stringify(bookData)))

internal suspend fun updateBook(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/update", JSON.stringify(bookData)))
