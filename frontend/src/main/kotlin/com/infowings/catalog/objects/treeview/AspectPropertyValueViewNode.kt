package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueViewModel
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.treeview.format.objectPropertyValueLineFormat
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

val aspectPropertyValueViewNode = rFunction<AspectPropertyValueViewNodeProps>("AspectPropertyValueViewNode") { props ->
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
                        propertyName = props.aspectProperty.roleName
                        aspectName = props.aspectProperty.aspectName
                        value = props.value.value
                        measure = props.aspectProperty.measure
                    }
                }
            }!!
            childrenValues(props.value, props.onUpdate)
        }
    }
}

private fun RBuilder.childrenValues(aspectPropertyValue: AspectPropertyValueViewModel, onUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit) {
    aspectPropertyValue.children.forEachIndexed { valueGroupIndex, (aspectProperty, groupValues) ->
        groupValues.forEachIndexed { valueIndex, value ->
            aspectPropertyValueViewNode {
                attrs {
                    this.aspectProperty = aspectProperty
                    this.value = value
                    this.onUpdate = { block ->
                        onUpdate {
                            this.children[valueGroupIndex].values[valueIndex].block()
                        }
                    }
                }
            }
        }
    }
}

interface AspectPropertyValueViewNodeProps : RProps {
    var aspectProperty: AspectPropertyViewModel
    var value: AspectPropertyValueViewModel
    var onUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit
}
