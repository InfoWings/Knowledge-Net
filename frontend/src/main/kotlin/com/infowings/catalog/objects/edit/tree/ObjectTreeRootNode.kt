package com.infowings.catalog.objects.edit.tree

//import com.infowings.catalog.common.AspectData
//import com.infowings.catalog.components.treeview.controlledTreeNode
//import com.infowings.catalog.objects.ObjectEditModel
//import com.infowings.catalog.objects.ObjectPropertyEditModel
//import com.infowings.catalog.objects.edit.ObjectTreeEditModel
//import react.*
//
//class ObjectTreeRootNode : RComponent<ObjectTreeRootNode.Props, RState>() {
//
//    override fun RBuilder.render() {
//        controlledTreeNode {
//            attrs {
//                className = "object-tree-edit__root"
//                expanded = props.objTreeEdit.expanded
//                onExpanded = {
//                    props.objectTreeViewModel.updateObject(props.objectIndex) {
//                        expanded = it
//                    }
//                }
//                treeNodeContent = buildElement {
//                    objectTreeRoot {
//                        attrs {
//                            objectTreeEdit = props.objTreeEdit
//                            onUpdate = props.objectTreeViewModel::updateSelectedObject
//                            onStartEdit = { props.objectTreeViewModel.selectObject(props.objTreeEdit) }
//                            onAddProperty = {
//                                // TODO: Handle situation when there is already empty property
//                                props.objectTreeViewModel.updateSelectedObject {
//                                    properties.add(ObjectPropertyEditModel())
//                                    expanded = true
//                                }
//                            }
//                            isSelected = props.objTreeEdit === props.editedObject
//                            onSubmit = if (props.objTreeEdit === props.editedObject) {
//                                { props.objectTreeViewModel.saveObject() }
//                            } else null
//                        }
//                    }
//                }!!
//            }
//            props.objTreeEdit.properties.forEachIndexed { index, property ->
//                objectPropertyNode(
//                    property = property,
//                    aspectsMap = props.aspectsMap,
//                    onEdit = { props.objectTreeViewModel.selectObject(props.objTreeEdit) },
//                    onUpdate = { block ->
//                        props.objectTreeViewModel.updateSelectedObject {
//                            properties[index].block()
//                        }
//                    },
//                    onUpdateWithoutSelect = { block ->
//                        props.objectTreeViewModel.updateObject(props.objectIndex) {
//                            properties[index].block()
//                        }
//                    }
//                )
//            }
//        }
//    }
//
//    interface Props : RProps {
//        var objectIndex: Int
//        var objTreeEdit: ObjectEditModel
//        var editedObject: ObjectEditModel?
//        var objectTreeViewModel: ObjectTreeEditModel
//        var aspectsMap: Map<String, AspectData>
//    }
//}
//
//fun RBuilder.objectTreeRootNode(block: RHandler<ObjectTreeRootNode.Props>) =
//    child(ObjectTreeRootNode::class, block)