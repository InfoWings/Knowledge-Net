package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.view.ObjectsLazyModel
import com.infowings.catalog.objects.view.tree.format.loadingStub
import com.infowings.catalog.objects.view.tree.format.objectLineFormat
import react.RProps
import react.buildElement
import react.rFunction

val objectLazyTreeRootNode = rFunction<ObjectLazyTreeRootNodeProps>("ObjectLazyTreeRootNode") { props ->
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
                        objectId = props.objectView.id
                        objectName = props.objectView.name
                        objectGuid = props.objectView.guid
                        objectDescription = props.objectView.description
                        subjectName = props.objectView.subjectName
                        expandTree = {
                            if (props.objectView.objectProperties == null) {
                                props.objectTreeModel.requestDetailed(props.objectView.id)
                            }
                            props.objectTreeModel.updateObject(props.objectIndex) {
                                expanded = true
                                if (objectProperties == null) {
                                    expandAllFlag = true
                                } else {
                                    expandAll()
                                }
                            }
                        }
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

interface ObjectLazyTreeRootNodeProps : RProps {
    var objectIndex: Int
    var objectView: ObjectLazyViewModel
    var objectTreeModel: ObjectsLazyModel
}

