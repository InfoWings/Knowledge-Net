package com.infowings.catalog.common

import kotlinx.serialization.Serializable

/**
 * [root] is a fake component. Just ignore this
 * */
@Serializable
data class ReferenceBook(val name: String, val aspectId: String, val root: ReferenceBookItem?) {
    val id: String? = root?.id
    val children: List<ReferenceBookItem> = root?.children ?: emptyList()
    operator fun get(child: String): ReferenceBookItem? = root?.get(child)
}

@Serializable
data class ReferenceBookItem(val id: String, val value: String, val children: List<ReferenceBookItem> = emptyList()) {
    private val accessChildrenMap = children.map { it.value to it }.toMap()

    operator fun get(child: String): ReferenceBookItem? = accessChildrenMap[child]
}
