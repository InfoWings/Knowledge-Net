package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyViewModel2
import com.infowings.catalog.objects.treeview.format.objectPropertyLineFormat
import react.RProps
import react.buildElement
import react.rFunction

val objectPropertyViewNode = rFunction<ObjectPropertyViewNodeProps>("ObjectPropertyViewNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.property.expanded
            onExpanded = {
                props.onUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                objectPropertyLineFormat {
                    attrs {
                        name = props.property.name
                        aspectName = props.property.aspect.name ?: error("Aspect without name")
                        cardinality = props.property.cardinality
                    }
                }
            }!!
        }
    }
}

interface ObjectPropertyViewNodeProps : RProps {
    var property: ObjectPropertyViewModel2
    var onUpdate: (ObjectPropertyViewModel2.() -> Unit) -> Unit
}
