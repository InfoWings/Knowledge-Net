package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookItem

data class ReferenceBookData(
    val id: String?,
    val name: String?,
    val aspectId: String,
    val children: List<ReferenceBookItem> = emptyList()
)