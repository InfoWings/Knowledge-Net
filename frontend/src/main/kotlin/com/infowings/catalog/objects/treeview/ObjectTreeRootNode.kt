package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyViewModel
import com.infowings.catalog.objects.ObjectTreeViewModel
import com.infowings.catalog.objects.ObjectViewModel
import react.*

class ObjectTreeRootNode : RComponent<ObjectTreeRootNode.Props, RState>() {

    override fun RBuilder.render() {
        controlledTreeNode {
            attrs {
                className = "object-tree-view__root"
                expanded = props.objTreeView.expanded
                onExpanded = {
                    props.objectTreeViewModel.updateObjTree(props.objectIndex) {
                        expanded = it
                    }
                }
                treeNodeContent = buildElement {
                    objectTreeRoot {
                        attrs {
                            objectTreeView = props.objTreeView
                            onUpdate = props.objectTreeViewModel::updateSelectedObjTree
                            onStartEdit = { props.objectTreeViewModel.selectObjTree(props.objTreeView) }
                            onAddProperty = {
                                // TODO: Handle situation when there is already empty property
                                props.objectTreeViewModel.updateSelectedObjTree {
                                    properties.add(ObjectPropertyViewModel())
                                    expanded = true
                                }
                            }
                            isSelected = props.objTreeView === props.editedObject
                            onSubmit = if (props.objTreeView === props.editedObject) {
                                { props.objectTreeViewModel.saveObjTree() }
                            } else null
                        }
                    }
                }!!
            }
            props.objTreeView.properties.forEachIndexed { index, property ->
                objectPropertyNode(
                    property = property,
                    aspectsMap = props.aspectsMap,
                    onEdit = { props.objectTreeViewModel.selectObjTree(props.objTreeView) },
                    onUpdate = { block ->
                        props.objectTreeViewModel.updateSelectedObjTree {
                            properties[index].block()
                        }
                    },
                    onUpdateWithoutSelect = { block ->
                        props.objectTreeViewModel.updateObjTree(props.objectIndex) {
                            properties[index].block()
                        }
                    }
                )
            }
        }
    }

    interface Props : RProps {
        var objectIndex: Int
        var objTreeView: ObjectViewModel
        var editedObject: ObjectViewModel?
        var objectTreeViewModel: ObjectTreeViewModel
        var aspectsMap: Map<String, AspectData>
    }
}

fun RBuilder.objectTreeRootNode(block: RHandler<ObjectTreeRootNode.Props>) =
    child(ObjectTreeRootNode::class, block)