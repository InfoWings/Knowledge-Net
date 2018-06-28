package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.view.ObjectsLazyModel
import com.infowings.catalog.objects.view.tree.format.loadingStub
import com.infowings.catalog.objects.view.tree.format.objectLineFormat
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
                        }
                    }
                }!!
            }
            val objectProperties = props.objectView.objectProperties
            when {
                objectProperties == null && props.objectView.objectPropertiesCount > 0 -> loadingStub {}
                objectProperties != null -> objectProperties.forEachIndexed { propertyIndex, property ->
                    property.values.forEachIndexed { valueIndex, value ->
                        objectPropertyValueViewNode {
                            attrs {
                                this.property = property
                                this.value = value
                                onUpdate = { block ->
                                    props.objectTreeModel.updateObject(props.objectIndex) {
                                        val properties = this.objectProperties ?: error("Properties should be available on update")
                                        properties[propertyIndex].values[valueIndex].block()
                                    }
                                }
                            }
                        }
                    }
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