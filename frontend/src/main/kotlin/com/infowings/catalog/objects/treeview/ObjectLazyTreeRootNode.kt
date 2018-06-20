package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.ObjectsLazyModel
import com.infowings.catalog.objects.treeedit.objectPropertyNode
import com.infowings.catalog.objects.treeview.format.loadingStub
import com.infowings.catalog.objects.treeview.format.objectLineFormat
import react.*

class ObjectLazyTreeRootNode : RComponent<ObjectLazyTreeRootNode.Props, RState>() {

    override fun RBuilder.render() {
        controlledTreeNode {
            attrs {
                className = "object-tree-view__root"
                expanded = props.objectView.expanded
                onExpanded = {
                    if (props.objectView.objectProperties == null) {
                        props.objectTreeModel.requestDetailed(props.objectView.id)
                    }
                    props.objectTreeModel.updateObject(props.objectIndex) {
                        expanded = it
                    }
                }
                treeNodeContent = buildElement {
                    objectLineFormat {
                        attrs {
                            objectName = props.objectView.name
                            objectDescription = props.objectView.description
                            subjectName = props.objectView.subjectName
                            subjectDescription = props.objectView.subjectDescription
                        }
                    }
                }!!
            }
            val objectProperties = props.objectView.objectProperties
            when {
                objectProperties == null && props.objectView.objectPropertiesCount > 0 -> loadingStub {}
                objectProperties != null -> objectProperties.forEachIndexed { index, property ->
                    objectPropertyNode(
                        property = property,
                        aspectsMap = emptyMap(),
                        onEdit = {},
                        onUpdate = {},
                        onUpdateWithoutSelect = { block ->
                            props.objectTreeModel.updateObject(props.objectIndex) {
                                val properties = this.objectProperties ?: error("Properties should be available on update")
                                properties[index].block()
                            }
                        }
                    )
                }
            }
        }
    }

    interface Props : RProps {
        var objectIndex: Int
        var objectView: ObjectLazyViewModel
        var objectTreeModel: ObjectsLazyModel
    }
}

fun RBuilder.objectLazyTreeRootNode(handler: RHandler<ObjectLazyTreeRootNode.Props>) = child(ObjectLazyTreeRootNode::class, handler)