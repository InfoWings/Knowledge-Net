package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.ObjectsLazyModel
import react.*

class ObjectLazyTreeRootNode : RComponent<ObjectLazyTreeRootNode.Props, RState>() {

    override fun RBuilder.render() {
        controlledTreeNode {
            attrs {
                className = "object-tree-view__root"
                expanded = props.objectView.expanded
                onExpanded = {
                    if (props.objectView.objectProperties == null) {
                        props.objectTreeModel.expandObject(props.objectView.id)
                    }
                    props.objectTreeModel.updateObject(props.objectIndex) {
                        expanded = it
                    }
                }
                treeNodeContent = buildElement {
                    objectLazyTreeRoot {
                        attrs {
                            objectTreeView = props.objectView
                        }
                    }
                }!!
            }
            +"Loading..."
        }
    }

    interface Props : RProps {
        var objectIndex: Int
        var objectView: ObjectLazyViewModel
        var objectTreeModel: ObjectsLazyModel
    }
}

fun RBuilder.objectLazyTreeRootNode(handler: RHandler<ObjectLazyTreeRootNode.Props>) = child(ObjectLazyTreeRootNode::class, handler)