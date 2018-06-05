package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueGroupViewModel
import com.infowings.catalog.objects.AspectPropertyValueViewModel
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.treeview.utils.constructAspectTree
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.buildElement
import react.dom.div


fun RBuilder.aspectPropertyValues(
    groups: MutableList<AspectPropertyValueGroupViewModel>,
    aspectsMap: Map<String, AspectData>,
    onEdit: () -> Unit,
    onUpdate: (index: Int, AspectPropertyValueGroupViewModel.() -> Unit) -> Unit,
    onNonSelectedUpdate: (index: Int, AspectPropertyValueGroupViewModel.() -> Unit) -> Unit
) {
    groups.forEachIndexed { groupIndex, (property, values) ->
        values.forEachIndexed { valueIndex, value ->
            aspectPropertyValue(
                aspectProperty = property,
                aspectsMap = aspectsMap,
                value = value,
                onEdit = onEdit,
                onUpdate = { block ->
                    onUpdate(groupIndex) {
                        this.values[valueIndex].block()
                    }
                },
                onNonSelectedUpdate = { block ->
                    onNonSelectedUpdate(groupIndex) {
                        this.values[valueIndex].block()
                    }
                }
            )
        }
        if (property.cardinality == Cardinality.INFINITY || (property.cardinality == Cardinality.ONE && values.size == 0)) {
            div {
                Button {
                    attrs {
                        className = "pt-minimal"
                        intent = Intent.PRIMARY
                        icon = "plus"
                        text = ("Add property " + (property.roleName
                                ?: "") + " " + property.aspectName).asReactElement()
                        onClick = {
                            onEdit()
                            onUpdate(groupIndex) {
                                this.values.add(AspectPropertyValueViewModel())
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.aspectPropertyValue(
    aspectProperty: AspectPropertyViewModel,
    aspectsMap: Map<String, AspectData>,
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
                            constructAspectTree(aspectProperty, aspectsMap)
                        }
                    }
                )
            }!!
        }
        aspectPropertyValues(
            groups = value.children,
            aspectsMap = aspectsMap,
            onEdit = onEdit,
            onUpdate = { index, block ->
                onUpdate {
                    children[index].block()
                }
            },
            onNonSelectedUpdate = { index, block ->
                onNonSelectedUpdate {
                    children[index].block()
                }
            }
        )
    }
