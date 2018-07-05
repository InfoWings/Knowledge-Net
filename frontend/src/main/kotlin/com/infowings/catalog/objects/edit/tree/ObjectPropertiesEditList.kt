package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.tree.format.objectPropertyEditLineFormat
import react.RProps
import react.buildElement
import react.rFunction

val objectPropertiesEditList = rFunction<ObjectPropertiesEditListProps>("ObjectPropertiesEditList") { props ->
    props.properties.forEachIndexed { index, property ->
        objectPropertyEditNode {
            attrs {
                this.property = property
                onUpdate = { block ->
                    props.updater(index, block)
                }
                onCreate = if (property.id == null && property.cardinality != null && property.aspect != null) {
                    { props.onCreateProperty(property) }
                } else null
            }
        }
    }
}

interface ObjectPropertiesEditListProps : RProps {
    var properties: List<ObjectPropertyEditModel>
    var onCreateProperty: (ObjectPropertyEditModel) -> Unit
    var updater: (index: Int, ObjectPropertyEditModel.() -> Unit) -> Unit
}

val objectPropertyEditNode = rFunction<ObjectPropertyEditNodeProps>("ObjectPropertyEditNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.property.id != null && props.property.expanded
            onExpanded = {
                props.onUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                objectPropertyEditLineFormat {
                    attrs {
                        name = props.property.name
                        cardinality = props.property.cardinality
                        aspect = props.property.aspect
                        onNameChanged = {
                            props.onUpdate {
                                name = it
                            }
                        }
                        onCardinalityChanged = {
                            props.onUpdate {
                                cardinality = it
                            }
                        }
                        onAspectChanged = {
                            props.onUpdate {
                                aspect = it
                            }
                        }
                        onConfirmCreate = props.onCreate
                    }
                }
            }!!
        }
    }
}

interface ObjectPropertyEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var onUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onCreate: (() -> Unit)?
}