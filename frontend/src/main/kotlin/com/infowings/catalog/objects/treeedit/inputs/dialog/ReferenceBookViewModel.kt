package com.infowings.catalog.objects.treeedit.inputs.dialog

import com.infowings.catalog.common.RefBookNodeDescriptor
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem

class ReferenceBookViewModel(
    val name: String,
    val description: String?,
    val items: List<ReferenceBookItemViewModel>
) {
    fun expandPath(path: List<RefBookNodeDescriptor>): ReferenceBookViewModel {
        items.expandPath(path)
        return this
    }
}

class ReferenceBookItemViewModel(
    val id: String,
    val value: String,
    val description: String?,
    val children: List<ReferenceBookItemViewModel>,
    var isExpanded: Boolean = false
)

fun List<ReferenceBookItemViewModel>.expandPath(path: List<RefBookNodeDescriptor>) {
    if (path.isNotEmpty()) {
        val first = path.first()
        val item = this.find { first.id == it.id } ?: error("")
        item.isExpanded = true
        item.children.expandPath(path.drop(1))
    }
}

fun ReferenceBook.toSelectViewModel() = ReferenceBookViewModel(
    name = this.name,
    description = this.description,
    items = this.children.map { it.toSelectViewModel() }
)

fun ReferenceBookItem.toSelectViewModel(): ReferenceBookItemViewModel = ReferenceBookItemViewModel(
    id = this.id,
    value = this.value,
    description = this.description,
    children = this.children.map { it.toSelectViewModel() }
)