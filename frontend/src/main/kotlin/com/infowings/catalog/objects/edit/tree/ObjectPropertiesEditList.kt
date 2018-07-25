package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.ObjectTreeEditModel
import com.infowings.catalog.objects.edit.tree.format.objectPropertyEditLineFormat
import com.infowings.catalog.objects.edit.tree.format.objectPropertyValueEditLineFormat
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

fun RBuilder.objectPropertiesEditList(
    properties: List<ObjectPropertyEditModel>,
    editModel: ObjectTreeEditModel,
    apiModelProperties: List<ObjectPropertyEditDetailsResponse>,
    updater: (index: Int, ObjectPropertyEditModel.() -> Unit) -> Unit
) {
    val apiModelPropertiesById = apiModelProperties.associateBy { it.id }
    properties.forEachIndexed { propertyIndex, property ->
        val propertyValues = property.values
        if (propertyValues == null || propertyValues.isEmpty()) {
            objectPropertyEditNode {
                val apiModelProperty = property.id?.let { apiModelPropertiesById[property.id] }
                attrs {
                    this.property = property
                    onUpdate = { block ->
                        updater(propertyIndex, block)
                    }
                    onConfirm = when {
                        property.id == null && property.aspect != null -> {
                            { editModel.createProperty(property) }
                        }
                        property.id != null && apiModelProperty != null && (apiModelProperty.name != property.name || apiModelProperty.aspectDescriptor.id != property.aspect?.id) -> {
                            { editModel.updateProperty(property) }
                        }
                        else -> null
                    }
                    onRemove = if (property.id != null) {
                        { editModel.deleteProperty(property) }
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
            val apiModelValuesById = apiModelPropertiesById[property.id]?.valueDescriptors?.associateBy { it.id } ?: emptyMap()
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
                        onSaveValue = when {
                            value.id == null && value.value != null -> {
                                { editModel.createValue(value.value ?: error("value should not be null"), property.id ?: error("Property should have id != null"), null, null) }
                            }
                            value.id != null && value.value != null && value.value != apiModelValuesById[value.id]?.value?.toData() -> {
                                { editModel.updateValue(value.id, property.id ?: error("Property should have id != null"), value.value ?: error("Value should not be null"))}
                            }
                            else -> null
                        }
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
                            (value.id != null && allValues.size > 1) -> {
                                {
                                    editModel.deleteValue(value.id, property.id ?: error("Property should have id"))
                                }
                            }
                            (value.id != null && value.value != ObjectValueData.NullValue) -> {
                                {
                                    editModel.updateValue(value.id, property.id ?: error("Property should have id"), ObjectValueData.NullValue)
                                }
                            }
                            (value.id != null && value.value == ObjectValueData.NullValue) -> {
                                {
                                    editModel.deleteProperty(property)
                                }
                            }
                            else -> null
                        }
                        this.editModel = editModel
                        this.apiModelValuesById = apiModelValuesById
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
                        onConfirmCreate = props.onConfirm
                        onAddValue = props.onAddValue
                        onRemoveProperty = props.onRemove
                    }
                }
            }!!
        }
    }
}

interface ObjectPropertyEditNodeProps : RProps {
    var property: ObjectPropertyEditModel
    var onUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
    var onConfirm: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemove: (() -> Unit)?
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
                        needRemoveConfirmation = props.rootValue.id != null
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
                onRemoveGroup = { id ->
                    props.onValueUpdate {
                        val groupIndex = valueGroups.indexOfFirst { it.propertyId == id }
                        valueGroups.removeAt(groupIndex)
                    }
                },
                editModel = props.editModel,
                objectPropertyId = props.property.id ?: error("Object property should exist when editing values"),
                apiModelValuesById = props.apiModelValuesById
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
    var editModel: ObjectTreeEditModel
    var apiModelValuesById: Map<String, ValueTruncated>
}

fun AspectTree.defaultValue(): ObjectValueData? {
    val baseType = baseType?.let { BaseType.valueOf(it) } ?: measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type: ${this}")
    return baseType.defaultValue()
}