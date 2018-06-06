package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.treeview.inputs.values.booleanInput
import com.infowings.catalog.objects.treeview.inputs.values.decimalInput
import com.infowings.catalog.objects.treeview.inputs.values.integerInput
import com.infowings.catalog.objects.treeview.inputs.values.textInput
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
            when (aspectProperty.baseType) {
                BaseType.Text.name -> textInput(value, onUpdate, onEdit)
                BaseType.Integer.name -> integerInput(value, onUpdate)
                BaseType.Decimal.name -> decimalInput(value, onUpdate)
                BaseType.Boolean.name -> booleanInput(value, onUpdate)
            }
        }
    }
