package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjTreeView
import com.infowings.catalog.objects.ObjTreeViewProperty
import com.infowings.catalog.objects.ObjectTreeViewModel
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
                                    properties.add(ObjTreeViewProperty(null, null, null))
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
                objectPropertyLine {
                    attrs {
                        this.property = property
                        onEdit = { props.objectTreeViewModel.selectObjTree(props.objTreeView) }
                        onUpdate = { block ->
                            props.objectTreeViewModel.updateSelectedObjTree {
                                properties[index].block()
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var objectIndex: Int
        var objTreeView: ObjTreeView
        var editedObject: ObjTreeView?
        var objectTreeViewModel: ObjectTreeViewModel
    }
}

fun RBuilder.objectTreeRootNode(block: RHandler<ObjectTreeRootNode.Props>) =
    child(ObjectTreeRootNode::class, block)