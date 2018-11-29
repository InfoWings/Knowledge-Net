package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueViewModel
import com.infowings.catalog.objects.AspectPropertyViewModel
import com.infowings.catalog.objects.view.tree.format.aspectPropertyValueLineFormat
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.RouteSuppliedProps
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
                aspectPropertyValueLineFormat {
                    attrs {
                        propertyName = props.aspectProperty.roleName
                        aspectName = props.aspectProperty.aspectName
                        value = props.value.value
                        valueDescription = props.value.description
                        valueGuid = props.value.guid
                        measureSymbol = props.value.measureSymbol
                        subjectName = props.aspectProperty.subjectName
                        history = props.history
                    }
                }
            }!!
            childrenValues(props.value, props.history, props.onUpdate)
        }
    }
}

private fun RBuilder.childrenValues(aspectPropertyValue: AspectPropertyValueViewModel, newHistory: History, onUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit) {
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
                    history = newHistory
                }
            }
        }
    }
}

interface AspectPropertyValueViewNodeProps : RouteSuppliedProps {
    var aspectProperty: AspectPropertyViewModel
    var value: AspectPropertyValueViewModel
    var onUpdate: (AspectPropertyValueViewModel.() -> Unit) -> Unit
}
