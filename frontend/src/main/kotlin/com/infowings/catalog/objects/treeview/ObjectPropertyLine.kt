package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.ObjTreeViewProperty
import com.infowings.catalog.objects.treeview.inputs.propertyAspect
import com.infowings.catalog.objects.treeview.inputs.propertyName
import com.infowings.catalog.objects.treeview.utils.propertyAspectTypeInfo
import react.*
import react.dom.div

class ObjectPropertyLine : RComponent<ObjectPropertyLine.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "object-tree-view__object-property") {
            propertyName(
                value = props.property.name ?: "",
                onEdit = props.onEdit,
                onCancel = {
                    props.onUpdate {
                        name = it
                    }
                },
                onChange = {
                    props.onUpdate {
                        name = it
                    }
                }
            )
            propertyAspect(
                value = props.property.aspect,
                onSelect = {
                    props.onUpdate {
                        aspect = it
                    }
                },
                onOpen = props.onEdit
            )
            propertyAspectTypeInfo(props.property.aspect)
        }
    }

    interface Props : RProps {
        var property: ObjTreeViewProperty
        var onEdit: () -> Unit
        var onUpdate: (ObjTreeViewProperty.() -> Unit) -> Unit
    }
}

fun RBuilder.objectPropertyLine(block: RHandler<ObjectPropertyLine.Props>) = child(ObjectPropertyLine::class, block)

