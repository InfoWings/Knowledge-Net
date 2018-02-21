package com.infowings.catalog.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

enum class ReferenceBookType { VERTEX, LEAF }

@Serializable
data class ReferenceBook(@Optional val id: String? = null, val name: String, val description: String, @Optional val children: List<ReferenceBookItem> = emptyList()) {
    constructor(id: String? = null, name: String, description: String) : this(id, name, description, emptyList())
}

abstract class ReferenceBookItem(val id: String? = null, val type: ReferenceBookType, val name: String, val description: String? = null)

class ReferenceBookVertex(id: String? = null,
                          name: String,
                          description: String?,
                          val children: List<ReferenceBookItem>) : ReferenceBookItem(id, ReferenceBookType.VERTEX, name, description)

class ReferenceBookLeaf(id: String? = null, name: String, description: String) : ReferenceBookItem(id, ReferenceBookType.LEAF, name, description)