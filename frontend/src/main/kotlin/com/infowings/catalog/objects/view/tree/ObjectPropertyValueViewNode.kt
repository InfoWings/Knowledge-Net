package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyValueViewModel
import com.infowings.catalog.objects.ObjectPropertyViewModel
import com.infowings.catalog.objects.view.tree.format.objectPropertyValueLineFormat
import react.RProps
import react.buildElement
import react.rFunction

val objectPropertyValueViewNode = rFunction<ObjectPropertyValueViewNodeProps>("ObjectPropertyValueViewNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.value.expanded
            onExpanded = {
                props.onUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                objectPropertyValueLineFormat {
                    attrs {
                        propertyName = props.property.name
                        aspectName = props.property.aspect.name
                        propertyDescription = props.property.description
                        value = props.value.value
                        valueDescription = props.value.description
                        measure = props.property.aspect.measure
                    }
                }
            }!!
        }
        props.value.valueGroups.forEachIndexed { valueGroupIndex, (aspectProperty, groupValues) ->
            groupValues.forEachIndexed { valueIndex, value ->
                aspectPropertyValueViewNode {
                    attrs {
                        this.aspectProperty = aspectProperty
                        this.value = value
                        onUpdate = { block ->
                            props.onUpdate {
                                this.valueGroups[valueGroupIndex].values[valueIndex].block()
                            }
                        }
                    }
                }
            }
        }
    }
}

interface ObjectPropertyValueViewNodeProps : RProps {
    var property: ObjectPropertyViewModel
    var value: ObjectPropertyValueViewModel
    var onUpdate: (ObjectPropertyValueViewModel.() -> Unit) -> Unit
}
