package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

private external fun encodeURIComponent(component: String): String = definedExternally

internal suspend fun getAll(): ReferenceBooksList =
    JSON.parse(get("/api/book/all"))

internal suspend fun getByAspectId(aspectId: String): ReferenceBooksList =
    JSON.parse(get("/api/book/get?aspectId=${encodeURIComponent(aspectId)}"))

internal suspend fun getByName(name: String): ReferenceBook =
    JSON.parse(get("/api/book/get/$name"))

internal suspend fun create(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/create", JSON.stringify(bookData)))

internal suspend fun update(bookData: ReferenceBookData): ReferenceBook =
    JSON.parse(post("/api/book/update", JSON.stringify(bookData)))
