package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.treeview.inputs.propertyValue
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
                append("${aspectProperty.roleName} ${aspectProperty.aspectName}")
                if (aspectProperty.cardinality != Cardinality.ZERO) {
                    append(" ( ${aspectProperty.domain} )")
                    aspectProperty.measure?.let { append(" ($it)") }
                    append(" :")
                }
            }
        }
        if (aspectProperty.cardinality != Cardinality.ZERO) {
            propertyValue(value, aspectProperty.baseType, onEdit, onUpdate, aspectProperty.refBookName)
        }
    }
