//package com.infowings.catalog.objects.edit.tree
//
//import com.infowings.catalog.common.AspectData
//import com.infowings.catalog.components.treeview.controlledTreeNode
//import com.infowings.catalog.objects.ObjectPropertyValueEditModel
//import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
//import com.infowings.catalog.objects.edit.tree.utils.constructAspectTree
//import com.infowings.catalog.wrappers.blueprint.Button
//import com.infowings.catalog.wrappers.blueprint.Intent
//import com.infowings.catalog.wrappers.react.asReactElement
//import react.RBuilder
//import react.buildElement
//
//fun RBuilder.objectPropertyValues(
//    values: MutableList<ObjectPropertyValueEditModel>,
//    aspect: AspectData,
//    aspectsMap: Map<String, AspectData>,
//    onEdit: () -> Unit,
//    onUpdate: (Int, ObjectPropertyValueEditModel.() -> Unit) -> Unit,
//    onNonSelectedUpdate: (Int, ObjectPropertyValueEditModel.() -> Unit) -> Unit,
//    onAddValue: () -> Unit
//) {
//    values.forEachIndexed { valueIndex, value ->
//        controlledTreeNode {
//            attrs {
//                className = "object-tree-edit__object-property-value"
//                expanded = value.expanded
//                onExpanded = {
//                    onNonSelectedUpdate(valueIndex) {
//                        expanded = it
//                    }
//                }
//                treeNodeContent = buildElement {
//                    propertyValue(
//                        value = value.value ?: "",
//                        onChange = {
//                            onUpdate(valueIndex) {
//                                this.value = it
//                                if (valueGroups.isEmpty()) {
//                                    constructAspectTree(aspect, aspectsMap)
//                                }
//                            }
//                        },
//                        onEdit = onEdit,
//                        baseType = aspect.baseType ?: error("Aspect must have base type"),
//                        aspectRefBookId = if (aspect.refBookName == null) null else aspect.id
//                    )
//                }!!
//            }
//            aspectPropertyValues(
//                groups = value.valueGroups,
//                aspectsMap = aspectsMap,
//                onEdit = onEdit,
//                onUpdate = { index, block ->
//                    onUpdate(valueIndex) {
//                        value.valueGroups[index].block()
//                    }
//                },
//                onNonSelectedUpdate = { index, block ->
//                    onNonSelectedUpdate(valueIndex) {
//                        value.valueGroups[index].block()
//                    }
//                }
//            )
//        }
//    }
//    Button {
//        // TODO: Handle situation when (selected/not selected), when to draw button or not
//        attrs {
//            className = "pt-minimal"
//            intent = Intent.PRIMARY
//            icon = "plus"
//            onClick = { onAddValue() }
//            text = ("Add value to property " + (aspect.name ?: "")).asReactElement()
//        }
//    }
//}

