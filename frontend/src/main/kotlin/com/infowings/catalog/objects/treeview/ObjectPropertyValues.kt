package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyValueViewModel
import com.infowings.catalog.objects.treeview.inputs.propertyValue
import com.infowings.catalog.objects.treeview.utils.constructAspectTree
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.buildElement

fun RBuilder.objectPropertyValues(
    values: MutableList<ObjectPropertyValueViewModel>,
    aspect: AspectData,
    aspectsMap: Map<String, AspectData>,
    onEdit: () -> Unit,
    onUpdate: (Int, ObjectPropertyValueViewModel.() -> Unit) -> Unit,
    onNonSelectedUpdate: (Int, ObjectPropertyValueViewModel.() -> Unit) -> Unit,
    onAddValue: () -> Unit
) {
    values.forEachIndexed { valueIndex, value ->
        controlledTreeNode {
            attrs {
                className = "object-tree-view__object-property-value"
                expanded = value.expanded
                onExpanded = {
                    onNonSelectedUpdate(valueIndex) {
                        expanded = it
                    }
                }
                treeNodeContent = buildElement {
                    propertyValue(
                        value = value.value ?: "",
                        onChange = {
                            onUpdate(valueIndex) {
                                this.value = it
                                if (valueGroups.isEmpty()) {
                                    constructAspectTree(aspect, aspectsMap)
                                }
                            }
                        },
                        onEdit = onEdit,
                        baseType = aspect.baseType ?: error("Aspect must have base type"),
                        refBookName = aspect.refBookName
                    )
                }!!
            }
            aspectPropertyValues(
                groups = value.valueGroups,
                aspectsMap = aspectsMap,
                onEdit = onEdit,
                onUpdate = { index, block ->
                    onUpdate(valueIndex) {
                        value.valueGroups[index].block()
                    }
                },
                onNonSelectedUpdate = { index, block ->
                    onNonSelectedUpdate(valueIndex) {
                        value.valueGroups[index].block()
                    }
                }
            )
        }
    }
    Button {
        // TODO: Handle situation when (selected/not selected), when to draw button or not
        attrs {
            className = "pt-minimal"
            intent = Intent.PRIMARY
            icon = "plus"
            onClick = { onAddValue() }
            text = ("Add value to property " + (aspect.name ?: "")).asReactElement()
        }
    }
}

