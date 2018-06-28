//package com.infowings.catalog.objects.edit.tree
//
//import com.infowings.catalog.common.AspectData
//import com.infowings.catalog.common.PropertyCardinality
//import com.infowings.catalog.components.treeview.controlledTreeNode
//import com.infowings.catalog.objects.AspectPropertyValueEditModel
//import com.infowings.catalog.objects.AspectPropertyValueGroupEditModel
//import com.infowings.catalog.objects.AspectPropertyViewModel
//import com.infowings.catalog.objects.edit.tree.utils.constructAspectTree
//import com.infowings.catalog.wrappers.blueprint.Button
//import com.infowings.catalog.wrappers.blueprint.Intent
//import com.infowings.catalog.wrappers.react.asReactElement
//import react.RBuilder
//import react.buildElement
//import react.dom.div
//
//
//fun RBuilder.aspectPropertyValues(
//    groups: MutableList<AspectPropertyValueGroupEditModel>,
//    aspectsMap: Map<String, AspectData>,
//    onEdit: () -> Unit,
//    onUpdate: (index: Int, AspectPropertyValueGroupEditModel.() -> Unit) -> Unit,
//    onNonSelectedUpdate: (index: Int, AspectPropertyValueGroupEditModel.() -> Unit) -> Unit
//) {
//    groups.forEachIndexed { groupIndex, (property, values) ->
//        values.forEachIndexed { valueIndex, value ->
//            aspectPropertyValue(
//                aspectProperty = property,
//                aspectsMap = aspectsMap,
//                value = value,
//                onEdit = onEdit,
//                onUpdate = { block ->
//                    onUpdate(groupIndex) {
//                        this.values[valueIndex].block()
//                    }
//                },
//                onNonSelectedUpdate = { block ->
//                    onNonSelectedUpdate(groupIndex) {
//                        this.values[valueIndex].block()
//                    }
//                }
//            )
//        }
//        if (property.cardinality == PropertyCardinality.INFINITY || (property.cardinality == PropertyCardinality.ONE && values.size == 0)) {
//            div {
//                Button {
//                    attrs {
//                        className = "pt-minimal"
//                        intent = Intent.PRIMARY
//                        icon = "plus"
//                        text = ("Add property " + (property.roleName
//                                ?: "") + " " + property.aspectName).asReactElement()
//                        onClick = {
//                            onEdit()
//                            onUpdate(groupIndex) {
//                                this.values.add(AspectPropertyValueEditModel())
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//fun RBuilder.aspectPropertyValue(
//    aspectProperty: AspectPropertyViewModel,
//    aspectsMap: Map<String, AspectData>,
//    value: AspectPropertyValueEditModel,
//    onEdit: () -> Unit,
//    onUpdate: (AspectPropertyValueEditModel.() -> Unit) -> Unit,
//    onNonSelectedUpdate: (AspectPropertyValueEditModel.() -> Unit) -> Unit
//) =
//    controlledTreeNode {
//        attrs {
//            className = "object-tree-edit__value"
//            expanded = value.expanded
//            onExpanded = {
//                onNonSelectedUpdate {
//                    expanded = it
//                }
//            }
//            treeNodeContent = buildElement {
//                aspectPropertyValueLine(
//                    aspectProperty = aspectProperty,
//                    value = value.value,
//                    onEdit = onEdit,
//                    onUpdate = {
//                        onUpdate {
//                            this.value = it
//                            constructAspectTree(aspectProperty, aspectsMap)
//                        }
//                    }
//                )
//            }!!
//        }
//        aspectPropertyValues(
//            groups = value.children,
//            aspectsMap = aspectsMap,
//            onEdit = onEdit,
//            onUpdate = { index, block ->
//                onUpdate {
//                    children[index].block()
//                }
//            },
//            onNonSelectedUpdate = { index, block ->
//                onNonSelectedUpdate {
//                    children[index].block()
//                }
//            }
//        )
//    }
