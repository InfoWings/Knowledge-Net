package com.infowings.catalog.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * [root] is a fake component. Just ignore this
 * */
@Serializable
data class ReferenceBook(
    val aspectId: String,
    val name: String,
    val root: ReferenceBookItem,
    val deleted: Boolean,
    val version: Int
) {
    val id: String = root.id
    val children: List<ReferenceBookItem> = root.children
    operator fun get(child: String): ReferenceBookItem? = root[child]
}

@Serializable
data class ReferenceBookItem(
    val aspectId: String,
    val parentId: String?,
    val id: String,
    val value: String,
    val children: List<ReferenceBookItem>,
    val deleted: Boolean,
    val version: Int
) {
    @Transient
    private val accessChildrenMap = children.map { it.value to it }.toMap()

    operator fun get(child: String): ReferenceBookItem? = accessChildrenMap[child]
}

@Serializable
data class ReferenceBooksList(val books: List<ReferenceBook>)