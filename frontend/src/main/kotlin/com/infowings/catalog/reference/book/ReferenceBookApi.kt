package com.infowings.catalog.reference.book

import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllReferenceBooks(): AspectsList = JSON.parse(get("/api/books/all"))

//suspend fun createReferenceBook(name: String, aspect: String): AspectData = JSON.parse(post("/api/books/create", JSON.stringify(body)))

