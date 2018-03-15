package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

internal suspend fun getAll(): Array<ReferenceBook> = JSON.parse(get("/api/book/all"))

internal suspend fun get(name: String): ReferenceBook = JSON.parse(get("/api/book/get/$name"))

internal suspend fun create(referenceBook: ReferenceBook): ReferenceBook =
    JSON.parse(post("/api/book/create", JSON.stringify(referenceBook)))

internal suspend fun update(referenceBook: ReferenceBook): ReferenceBook =
    JSON.parse(post("/api/book/update", JSON.stringify(referenceBook)))
