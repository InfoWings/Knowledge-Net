package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueGroupViewModel
import com.infowings.catalog.objects.AspectPropertyValueViewModel
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.ObjectPropertyValueViewModel
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.buildElement


fun RBuilder.aspectPropertyValues(
    groups: MutableList<AspectPropertyValueGroupViewModel>,
    onEdit: () -> Unit,
    onUpdate: (ObjectPropertyValueViewModel.() -> Unit) -> Unit,
    onNonSelectedUpdate: (ObjectPropertyValueViewModel.() -> Unit) -> Unit
) {
    groups.forEachIndexed { groupIndex, (property, values) ->
        values.forEachIndexed { valueIndex, value ->
            aspectPropertyValue(
                aspectProperty = property,
                value = value,
                onEdit = onEdit,
                onUpdate = { block ->
                    onUpdate {
                        valueGroups[groupIndex].values[valueIndex].block()
                    }
                },
                onNonSelectedUpdate = { block ->
                    onNonSelectedUpdate {
                        valueGroups[groupIndex].values[valueIndex].block()
                    }
                }
            )
        }
        Button {
            // TODO: conditionaly show this button (when [0..N] or [0..1] but without value)
            attrs {
                className = "pt-minimal"
                intent = Intent.PRIMARY
                icon = "plus"
                text = ("Add property " + (property.roleName ?: "") + " " + property.aspectName).asReactElement()
                onClick = {
                    onEdit()
                    onUpdate {
                        valueGroups[groupIndex].values.add(AspectPropertyValueViewModel())
                    }
                }
            }
        }
    }
}

fun RBuilder.aspectPropertyValue(
    aspectProperty: AspectPropertyViewModel,
    value: AspectPropertyValueViewModel,
    onEdit: () -> Unit,
    onUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit,
    onNonSelectedUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit
) =
    controlledTreeNode {
        attrs {
            className = "object-tree-view__value"
            expanded = value.expanded
            onExpanded = {
                onNonSelectedUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                aspectPropertyValueLine(
                    aspectProperty = aspectProperty,
                    value = value.value,
                    onEdit = onEdit,
                    onUpdate = {
                        onUpdate {
                            this.value = it
                        }
                    }
                )
            }!!
        }
    }
