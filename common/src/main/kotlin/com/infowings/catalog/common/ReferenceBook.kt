package com.infowings.catalog.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * [root] is a fake component. Just ignore this
 * */
@Serializable
data class ReferenceBook(val name: String, val aspectId: String, val root: ReferenceBookItem) {
    val id: String = root.id
    val children: List<ReferenceBookItem> = root.children
    operator fun get(child: String): ReferenceBookItem? = root[child]
}

@Serializable
data class ReferenceBookItem(val id: String, val value: String, val children: List<ReferenceBookItem> = emptyList()) {
    @Transient
    private val accessChildrenMap = children.map { it.value to it }.toMap()

    operator fun get(child: String): ReferenceBookItem? = accessChildrenMap[child]
}

@Serializable
data class ReferenceBooksList(val books: List<ReferenceBook>)

@Serializable
data class ReferenceBookData(val id: String?, val name: String, val aspectId: String)