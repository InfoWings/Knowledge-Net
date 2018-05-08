package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.wrappers.blueprint.EditableText
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.aspectPropertyValueLine(
    aspectProperty: AspectPropertyViewModel,
    value: String?,
    onEdit: () -> Unit,
    onUpdate: (String) -> Unit
) =
    div(classes = "object-tree-view__aspect-value") {
        span(classes = "aspect-value__label") {
            +buildString {
                append(aspectProperty.roleName)
                append(" ")
                append(aspectProperty.aspectName)
                append(" ( ")
                append(aspectProperty.domain)
                append(" ) :")

            }
        }
        EditableText {
            attrs {
                this.value = value ?: ""
                placeholder = "Enter property value"
                onCancel = { onUpdate(it) }
                onChange = { onUpdate(it) }
                this.onEdit = onEdit
            }
        }
    }
