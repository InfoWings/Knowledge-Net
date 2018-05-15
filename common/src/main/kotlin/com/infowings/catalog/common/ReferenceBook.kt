package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceBook(
    val aspectId: String,
    val id: String,
    val name: String,
    val description: String?,
    val children: List<ReferenceBookItem>,
    val deleted: Boolean,
    val version: Int
)

@Serializable
data class ReferenceBookItem(
    val id: String,
    val value: String,
    val description: String?,
    val children: List<ReferenceBookItem>,
    val deleted: Boolean,
    val version: Int
)

@Serializable
data class ReferenceBookItemData(
    val parentId: String,
    val bookItem: ReferenceBookItem
)

@Serializable
data class ReferenceBooksList(val books: List<ReferenceBook>)