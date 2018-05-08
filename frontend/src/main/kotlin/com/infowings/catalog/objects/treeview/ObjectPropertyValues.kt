package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueGroupViewModel
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.ObjectPropertyValueViewModel
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.EditableText
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
    values.forEachIndexed { index, value ->
        controlledTreeNode {
            attrs {
                className = "oblect-tree-view__object-property-value"
                expanded = value.expanded
                onExpanded = {
                    onNonSelectedUpdate(index) {
                        expanded = it
                    }
                }
                treeNodeContent = buildElement {
                    objectPropertyValueLine(
                        value = value.value ?: "",
                        onUpdate = {
                            onUpdate(index) {
                                this.value = it
                            }
                        },
                        onEdit = {
                            onUpdate(index) {
                                if (valueGroups.isEmpty()) {
                                    createGroupsForValue(aspect, aspectsMap)
                                }
                            }
                        }
                    )
                }!!
            }
            aspectPropertyValues(
                groups = value.valueGroups,
                onEdit = onEdit,
                onUpdate = { block ->
                    onUpdate(index) {
                        value.block()
                    }
                },
                onNonSelectedUpdate = { block ->
                    onNonSelectedUpdate(index) {
                        value.block()
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

fun RBuilder.objectPropertyValueLine(
    value: String,
    onUpdate: (String) -> Unit,
    onEdit: () -> Unit
) =
    EditableText {
        attrs {
            this.value = value
            onChange = onUpdate
            this.onEdit = onEdit
            onCancel = onUpdate
        }
    }

fun ObjectPropertyValueViewModel.createGroupsForValue(aspect: AspectData, aspectsMap: Map<String, AspectData>) {
    aspect.properties.forEach { propertyData ->
        val aspectData = aspectsMap[propertyData.aspectId] ?: error("Inconsistent State")
        valueGroups.add(
            AspectPropertyValueGroupViewModel(
                property = AspectPropertyViewModel(
                    propertyId = propertyData.id,
                    aspectId = propertyData.aspectId,
                    cardinality = Cardinality.valueOf(propertyData.cardinality),
                    roleName = propertyData.name,
                    aspectName = aspectData.name ?: error("Inconsistent State"),
                    baseType = aspectData.baseType ?: error("Inconsistent State"),
                    domain = aspectData.domain ?: error("Inconsistent State")
                )
            )
        )
    }
}
