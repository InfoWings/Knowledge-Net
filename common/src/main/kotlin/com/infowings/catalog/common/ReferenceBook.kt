package com.infowings.catalog.common

import kotlinx.serialization.Serializable

/**
 * [root] is a fake component. Just ignore this
 * */
@Serializable
data class ReferenceBook(
    val aspectId: String,
    val name: String,
    val children: List<ReferenceBookItem>,
    val deleted: Boolean,
    val version: Int
)

@Serializable
data class ReferenceBookItem(
    val aspectId: String,
    val parentId: String?,
    val id: String,
    val value: String,
    val children: List<ReferenceBookItem>,
    val deleted: Boolean,
    val version: Int
)

@Serializable
data class ReferenceBooksList(val books: List<ReferenceBook>)