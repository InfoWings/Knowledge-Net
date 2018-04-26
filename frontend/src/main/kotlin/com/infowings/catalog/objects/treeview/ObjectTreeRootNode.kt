package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjTreeView
import com.infowings.catalog.objects.ObjectTreeViewModel
import react.*

class ObjectTreeRootNode : RComponent<ObjectTreeRootNode.Props, RState>() {

    override fun RBuilder.render() {
        controlledTreeNode {
            attrs {
                className = "object-tree-view__root"
                expanded = props.objTreeView.expanded
                onExpanded = { props.objTreeView.expanded = it }
                treeNodeContent = buildElement {
                    objectTreeRoot {
                        attrs {
                            objectTreeView = props.objTreeView
                            onUpdate = props.objectTreeViewModel::updateObjTree
                            onStartEdit = { props.objectTreeViewModel.selectObjTree(props.objTreeView) }
                            isSelected = props.objTreeView === props.editedObject
                            onSubmit = if (props.objTreeView === props.editedObject) {
                                { props.objectTreeViewModel.saveObjTree() }
                            } else null
                        }
                    }
                }!!
            }
        }
    }

    interface Props : RProps {
        var objTreeView: ObjTreeView
        var editedObject: ObjTreeView?
        var objectTreeViewModel: ObjectTreeViewModel
    }
}

fun RBuilder.objectTreeRootNode(block: RHandler<RProps>) =
    child(ObjectTreeRootNode::class, block)