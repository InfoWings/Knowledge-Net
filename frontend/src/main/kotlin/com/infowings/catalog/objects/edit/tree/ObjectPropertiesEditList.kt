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
    onCreateValue: (ObjectValueData, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?) -> Unit,
    updater: (index: Int, ObjectPropertyEditModel.() -> Unit) -> Unit
) {
    properties.forEachIndexed { propertyIndex, property ->
        val propertyValues = property.values
        if (propertyValues == null || propertyValues.isEmpty()) {
            objectPropertyEditNode {
                attrs {
                    this.property = property
                    onUpdate = { block ->
                        updater(propertyIndex, block)
                    }
                    onCreate = if (property.id == null && property.aspect != null) {
                        { onCreateProperty(property) }
                    } else null
                    onAddValue = if (property.id != null) {
                        {
                            updater(propertyIndex) {
                                val currentValues = this.values

                                when {
                                    currentValues == null -> {
                                        this.values = mutableListOf(ObjectPropertyValueEditModel(
                                            null,
                                            ObjectValueData.NullValue,
                                            false,
                                            mutableListOf()
                                        ))
                                    }
                                    currentValues.isEmpty() -> {
                                        currentValues.add(
                                            ObjectPropertyValueEditModel(
                                                null,
                                                ObjectValueData.NullValue,
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
                        key = value.id ?: valueIndex.toString()
                        this.property = property
                        this.rootValue = value
                        onPropertyUpdate = { updater(propertyIndex, it) }
                        onValueUpdate = { block ->
                            updater(propertyIndex) {
                                values?.let {
                                    it[valueIndex].block()
                                } ?: error("Must not be able to update value if there is no value")
                            }
                        }
                        onSaveValue = if (value.id == null && value.value != null) {
                            { onCreateValue(value.value ?: error("value should not be null"), property.id ?: error("Property should have id != null"), null, null) }
                        } else null
                        val allValues = property.values ?: error("Property should have at least one value")
                        onAddValue = when {
                            allValues.all { it.id != null } && allValues.none { it.value == ObjectValueData.NullValue } -> {
                                {
                                    updater(propertyIndex) {
                                        values?.add(
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
                            value.id == null && value.value == ObjectValueData.NullValue -> {
                                {
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value"))[valueIndex].value = property.aspect?.defaultValue()
                                    }
                                }
                            }
                            else -> null
                        }
                        onCancelValue = if (value.id == null) {
                            {
                                updater(propertyIndex) {
                                    (values ?: error("Must not be able to update value if there is no value")).removeAt(valueIndex)
                                }
                            }
                        } else null
                        onRemoveValue = when {
                            (value.id == null && allValues.size > 1) -> {
                                {
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value")).removeAt(valueIndex)
                                    }
                                }
                            }
                            (value.id == null && value.value != ObjectValueData.NullValue) -> {
                                {
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value"))[valueIndex].value = ObjectValueData.NullValue
                                    }
                                }
                            }
                            else -> null
                        }
                        onSubmitValue = { value, parentValueId, aspectPropertyId ->
                            onCreateValue(value, property.id ?: error("Property should have id != null"), parentValueId, aspectPropertyId)
                        }
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
        }
    }
}

interface ObjectPropertyEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var onUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onCreate: (() -> Unit)?
    var onAddValue: (() -> Unit)?
}

val objectPropertyValueEditNode = rFunction<ObjectPropertyValueEditNodeProps>("ObjectPropertyValueEditNode") { props ->
    controlledTreeNode {
        val aspect = props.property.aspect ?: error("Object Property must have ready-to-use aspect")
        attrs {
            expanded = props.rootValue.id != null && props.rootValue.expanded
            onExpanded = {
                props.onValueUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                objectPropertyValueEditLineFormat {
                    attrs {
                        propertyName = props.property.name
                        aspectName = aspect.name
                        aspectBaseType = aspect.baseType?.let { BaseType.valueOf(it) } ?: aspect.measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type")
                        aspectMeasure = aspect.measure?.let { GlobalMeasureMap[it] }
                        subjectName = aspect.subjectName
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
                        onAddValue = props.onAddValue
                        onCancelValue = props.onCancelValue
                        onRemoveValue = props.onRemoveValue
                    }
                }
            }!!
        }
        val rootValueId = props.rootValue.id
        if (rootValueId != null && aspect.properties.isNotEmpty()) {
            aspectPropertiesEditList(
                aspect = props.property.aspect ?: error("Aspect should be present inside the property"),
                valueGroups = props.rootValue.valueGroups,
                parentValueId = rootValueId,
                onUpdate = { index, block ->
                    props.onValueUpdate {
                        valueGroups[index].block()
                    }
                },
                onAddValueGroup = { valueGroup ->
                    props.onValueUpdate {
                        valueGroups.add(valueGroup)
                    }
                },
                onSubmitValue = props.onSubmitValue,
                onRemoveGroup = { id ->
                    props.onValueUpdate {
                        val groupIndex = valueGroups.indexOfFirst { it.propertyId == id }
                        valueGroups.removeAt(groupIndex)
                    }
                }
            )
        }
    }
}

interface ObjectPropertyValueEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var rootValue: ObjectPropertyValueEditModel
    var onPropertyUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onValueUpdate: (ObjectPropertyValueEditModel.() -> Unit) -> Unit
    var onSaveValue: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onCancelValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
    var onSubmitValue: (ObjectValueData, parentValueId: String?, aspectPropertyId: String?) -> Unit
}

fun AspectTree.defaultValue(): ObjectValueData? {
    val baseType = baseType?.let { BaseType.valueOf(it) } ?: measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type: ${this}")
    return baseType.defaultValue()
}