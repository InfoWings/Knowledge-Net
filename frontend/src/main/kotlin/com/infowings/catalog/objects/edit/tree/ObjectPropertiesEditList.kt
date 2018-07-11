package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.tree.format.objectPropertyEditLineFormat
import com.infowings.catalog.objects.edit.tree.format.objectPropertyValueEditLineFormat
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

fun RBuilder.objectPropertiesEditList(
    properties: List<ObjectPropertyEditModel>,
    onCreateProperty: (ObjectPropertyEditModel) -> Unit,
    onCreateValue: (ObjectPropertyValueEditModel, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?) -> Unit,
    updater: (index: Int, ObjectPropertyEditModel.() -> Unit) -> Unit
) {
    properties.forEachIndexed { index, property ->
        val propertyValues = property.values
        if (propertyValues == null || propertyValues.isEmpty()) {
            objectPropertyEditNode {
                attrs {
                    this.property = property
                    onUpdate = { block ->
                        updater(index, block)
                    }
                    onCreate = if (property.id == null && property.aspect != null) {
                        { onCreateProperty(property) }
                    } else null
                    onCreateNullValue = if (property.id != null && property.values == null) {
                        { /*onCreateValue(ObjectPropertyValueEditModel(null, ObjectValueData.NullValue, false, mutableListOf()), null, null)*/ }
                    } else null
                    onAddValue = if (property.id != null) {
                        {
                            updater(index) {
                                when {
                                    this.values == null -> {
                                        this.values = mutableListOf(ObjectPropertyValueEditModel(
                                            null,
                                            property.aspect?.defaultValue(),
                                            false,
                                            mutableListOf()
                                        ))
                                    }
                                    this.values?.isEmpty() ?: TODO("Should not happen") -> {
                                        this.values?.add(
                                            ObjectPropertyValueEditModel(
                                                null,
                                                property.aspect?.defaultValue(),
                                                false,
                                                mutableListOf()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else null
                }
            }
        } else {
            propertyValues.forEachIndexed { valueIndex, value ->
                objectPropertyValueEditNode {
                    attrs {
                        this.property = property
                        this.rootValue = value
                        onPropertyUpdate = { updater(index, it) }
                        onValueUpdate = { block ->
                            updater(index) {
                                values?.let {
                                    it[valueIndex].block()
                                } ?: TODO("Should never happen")
                            }
                        }
                        onSaveValue = if (value.id == null && value.value != null) {
                            { onCreateValue(value, property.id ?: error("Property should have id != null"), null, null) }
                        } else null
                    }
                }
            }
        }
    }
}

val objectPropertyEditNode = rFunction<ObjectPropertyEditNodeProps>("ObjectPropertyEditNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.property.id != null && props.property.expanded
            onExpanded = {
                props.onUpdate {
                    expanded = it
                }
                if (it) {
                    props.onCreateNullValue?.invoke()
                }
            }
            treeNodeContent = buildElement {
                objectPropertyEditLineFormat {
                    attrs {
                        name = props.property.name
                        aspect = props.property.aspect
                        onNameChanged = {
                            props.onUpdate {
                                name = it
                            }
                        }
                        onAspectChanged = {
                            props.onUpdate {
                                aspect = it
                            }
                        }
                        onConfirmCreate = props.onCreate
                        onAddValue = props.onAddValue
                    }
                }
            }!!
            if (props.property.id != null) {
                +"Loading..."
            }
        }
    }
}

interface ObjectPropertyEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var onUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onCreate: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onCreateNullValue: (() -> Unit)?
}

val objectPropertyValueEditNode = rFunction<ObjectPropertyValueEditNodeProps>("ObjectPropertyValueEditNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.rootValue.id != null && props.rootValue.expanded
            onExpanded = {
                props.onValueUpdate {
                    expanded = false // TODO: fix
                }
            }
            treeNodeContent = buildElement {
                objectPropertyValueEditLineFormat {
                    val aspect = props.property.aspect ?: error("Object Property must have ready-to-use aspect")
                    attrs {
                        propertyName = props.property.name
                        aspectName = aspect.name
                        aspectBaseType = aspect.baseType?.let { BaseType.valueOf(it) } ?: aspect.measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type")
                        referenceBookId = aspect.refBookId
                        value = props.rootValue.value
                        onPropertyNameUpdate = {
                            props.onPropertyUpdate {
                                name = it
                            }
                        }
                        onValueUpdate = {
                            props.onValueUpdate {
                                value = it
                            }
                        }
                        onSaveValue = props.onSaveValue
                    }
                }
            }!!
        }
    }
}

interface ObjectPropertyValueEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var rootValue: ObjectPropertyValueEditModel
    var onPropertyUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onValueUpdate: (ObjectPropertyValueEditModel.() -> Unit) -> Unit
    var onSaveValue: (() -> Unit)?
}

fun TreeAspectResponse.defaultValue(): ObjectValueData? {
    val baseType = baseType?.let { BaseType.valueOf(it) } ?: measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type: ${this}")
    return baseType.defaultValue()
}