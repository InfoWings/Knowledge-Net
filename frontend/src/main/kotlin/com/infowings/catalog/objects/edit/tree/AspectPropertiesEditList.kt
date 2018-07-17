package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueEditModel
import com.infowings.catalog.objects.AspectPropertyValueGroupEditModel
import com.infowings.catalog.objects.edit.tree.format.aspectPropertyCreateLineFormat
import com.infowings.catalog.objects.edit.tree.format.aspectPropertyEditLineFormat
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

fun RBuilder.aspectPropertiesEditList(
    aspect: AspectTree,
    valueGroups: List<AspectPropertyValueGroupEditModel>,
    parentValueId: String,
    onUpdate: (index: Int, block: AspectPropertyValueGroupEditModel.() -> Unit) -> Unit,
    onAddValueGroup: (AspectPropertyValueGroupEditModel) -> Unit,
    onSubmitValue: (ObjectValueData, parentValueId: String, aspectPropertyId: String) -> Unit,
    onRemoveGroup: (id: String) -> Unit
) {
    val groupsMap = valueGroups.associateBy { it.propertyId }
    aspect.properties.forEach { aspectProperty ->
        val valueGroup = groupsMap[aspectProperty.id]

        if (valueGroup == null) { // Не может быть пустой (#isEmpty()), так как мы удаляем всю группу при удалении единственного значения
            aspectPropertyValueCreateNode {
                attrs {
                    this.aspectProperty = aspectProperty
                    this.onCreateValue = { valueData ->
                        onAddValueGroup(
                            AspectPropertyValueGroupEditModel(
                                propertyId = aspectProperty.id,
                                values = mutableListOf(
                                    AspectPropertyValueEditModel(
                                        id = null,
                                        value = valueData
                                    )
                                )
                            )
                        )
                    }
                }
            }
        } else {
            val valueGroupIndex = valueGroups.indexOfFirst { it.propertyId == valueGroup.propertyId }
            valueGroup.values.forEachIndexed { valueIndex, value ->
                aspectPropertyValueEditNode {
                    attrs {
                        this.aspectProperty = aspectProperty
                        this.value = value
                        this.valueCount = valueGroup.values.size
                        this.onUpdate = { block ->
                            onUpdate(valueGroupIndex) {
                                values[valueIndex].block()
                            }
                        }
                        this.onSubmit = if (value.id == null && value.value != null) {
                            { onSubmitValue(value.value ?: error("No value to submit"), parentValueId, aspectProperty.id) }
                        } else null
                        this.onCancel = if (value.id == null) {
                            if (valueGroup.values.size == 1) {
                                { onRemoveGroup(valueGroup.propertyId) }
                            } else {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values.removeAt(valueIndex)
                                    }
                                }
                            }
                        } else null
                        this.onAddValue = when {
                            value.id == null && value.value == ObjectValueData.NullValue -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values[valueIndex].value = aspectProperty.aspect.defaultValue()
                                    }
                                }
                            }
                            valueGroup.values.all { it.id != null } && valueGroup.values.none { it.value == ObjectValueData.NullValue } -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values.add(
                                            AspectPropertyValueEditModel(
                                                id = null,
                                                value = aspectProperty.aspect.defaultValue()
                                            )
                                        )
                                    }
                                }
                            }
                            else -> null
                        }
                        this.onRemoveValue = when {
                            (value.id == null && valueGroup.values.size > 1) -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values.removeAt(valueIndex)
                                    }
                                }
                            }
                            (value.id == null && value.value != ObjectValueData.NullValue) -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values[valueIndex].value = ObjectValueData.NullValue
                                    }
                                }
                            }
                            else -> null
                        }
                        this.onSubmitValueGeneric = onSubmitValue
                    }
                }
            }
        }
    }
}

val aspectPropertyValueCreateNode = rFunction<AspectPropertyValueCreateNodeProps>("AspectPropertyValueCreateNode") { props ->
    controlledTreeNode {
        attrs {
            treeNodeContent = buildElement {
                aspectPropertyCreateLineFormat {
                    attrs {
                        propertyName = props.aspectProperty.name
                        aspectName = props.aspectProperty.aspect.name
                        subjectName = props.aspectProperty.aspect.subjectName
                        cardinality = props.aspectProperty.cardinality
                        onCreateValue = {
                            if (props.aspectProperty.cardinality == PropertyCardinality.ZERO)
                                props.onCreateValue(ObjectValueData.NullValue)
                            else
                                props.onCreateValue(props.aspectProperty.aspect.defaultValue())
                        }
                    }
                }
            }!!
        }
    }
}

interface AspectPropertyValueCreateNodeProps : RProps {
    var aspectProperty: AspectPropertyTree
    var onCreateValue: (ObjectValueData?) -> Unit
}

val aspectPropertyValueEditNode = rFunction<AspectPropertyValueEditNodeProps>("AspectPropertyValueEditNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.value.id != null && props.value.expanded
            onExpanded = {
                props.onUpdate {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                aspectPropertyEditLineFormat {
                    attrs {
                        val aspect = props.aspectProperty.aspect
                        propertyName = props.aspectProperty.name
                        aspectName = aspect.name
                        aspectBaseType = aspect.baseType?.let { BaseType.valueOf(it) } ?: aspect.measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type")
                        aspectReferenceBookId = aspect.refBookId
                        aspectMeasure = aspect.measure?.let { GlobalMeasureMap[it] }
                        subjectName = aspect.subjectName
                        value = props.value.value
                        onChange = {
                            props.onUpdate {
                                value = it
                            }
                        }
                        recommendedCardinality = props.aspectProperty.cardinality
                        conformsToCardinality = when(props.aspectProperty.cardinality) {
                            PropertyCardinality.ZERO -> props.valueCount == 1 && props.value.value == ObjectValueData.NullValue
                            PropertyCardinality.ONE -> props.valueCount == 1 && props.value.value != ObjectValueData.NullValue
                            PropertyCardinality.INFINITY -> props.value.value != ObjectValueData.NullValue
                        }
                        onSubmit = props.onSubmit
                        onCancel = props.onCancel
                        onAddValue = props.onAddValue
                        onRemoveValue = props.onRemoveValue
                    }
                }
            }!!
        }
        val currentValueId = props.value.id
        if (currentValueId != null) {
            aspectPropertiesEditList(
                aspect = props.aspectProperty.aspect,
                valueGroups = props.value.children,
                parentValueId = currentValueId,
                onUpdate = { index, block ->
                    props.onUpdate {
                        children[index].block()
                    }
                },
                onAddValueGroup = { newValueGroup ->
                    props.onUpdate { children.add(newValueGroup) }
                },
                onSubmitValue = props.onSubmitValueGeneric,
                onRemoveGroup = { id ->
                    props.onUpdate {
                        val removeIndex = children.indexOfFirst { it.propertyId == id }
                        children.removeAt(removeIndex)
                    }
                }
            )
        }
    }
}

interface AspectPropertyValueEditNodeProps : RProps {
    var aspectProperty: AspectPropertyTree
    var value: AspectPropertyValueEditModel
    var valueCount: Int
    var onUpdate: (AspectPropertyValueEditModel.() -> Unit) -> Unit
    var onSubmit: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
    var onSubmitValueGeneric: (ObjectValueData, String, String) -> Unit
}

