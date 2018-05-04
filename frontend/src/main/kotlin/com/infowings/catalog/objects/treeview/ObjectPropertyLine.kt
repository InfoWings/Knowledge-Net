package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.ObjTreeViewProperty
import com.infowings.catalog.objects.ObjectPropertyValueView
import com.infowings.catalog.objects.treeview.inputs.propertyAspect
import com.infowings.catalog.objects.treeview.inputs.propertyCardinality
import com.infowings.catalog.objects.treeview.inputs.propertyName
import com.infowings.catalog.objects.treeview.inputs.propertyValue
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
            propertyCardinality(
                value = props.property.cardinality,
                onChange = {
                    props.onUpdate {
                        cardinality = it
                    }
                }
            )
            propertyAspectTypeInfo(props.property.aspect)
            if (props.property.aspect != null && (props.property.cardinality == Cardinality.ONE || props.property.cardinality == Cardinality.INFINITY)) {
                val aspect = props.property.aspect
                if (aspect != null && aspect.properties.isEmpty()) {
                    propertyValue(
                        value = props.property.value?.value ?: "",
                        onEdit = {
                            props.onEdit()
                            if (props.property.value == null) {
                                props.onUpdate {
                                    value = ObjectPropertyValueView(null, null)
                                }
                            }
                        },
                        onChange = {
                            props.onUpdate {
                                val value = this.value
                                if (value != null) {
                                    value.value = it
                                }
                            }
                        },
                        onCancel = {
                            props.onUpdate {
                                val value = this.value
                                if (value != null) {
                                    value.value = it
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    interface Props : RProps {
        var property: ObjTreeViewProperty
        var onEdit: () -> Unit
        var onUpdate: (ObjTreeViewProperty.() -> Unit) -> Unit
    }
}

fun RBuilder.objectPropertyLine(block: RHandler<ObjectPropertyLine.Props>) = child(ObjectPropertyLine::class, block)

