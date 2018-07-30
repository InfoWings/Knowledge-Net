package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.EditContext
import com.infowings.catalog.objects.edit.EditExistingContextModel
import com.infowings.catalog.objects.edit.EditNewChildContextModel
import com.infowings.catalog.objects.edit.ObjectTreeEditModel
import com.infowings.catalog.objects.edit.tree.format.objectPropertyEditLineFormat
import com.infowings.catalog.objects.edit.tree.format.objectPropertyValueEditLineFormat
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

fun RBuilder.objectPropertiesEditList(
    editContext: EditContext,
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
                    val currentEditContextModel = editContext.currentContext
                    this.property = property
                    onUpdate = if (currentEditContextModel == null) {
                        { block ->
                            editContext.setContext(EditExistingContextModel(property.id ?: error("Property should have id in order to be edited")))
                            updater(propertyIndex, block)
                        }
                    } else {
                        { block ->
                            updater(propertyIndex, block)
                        }
                    }
                    onConfirm = when {
                        property.id == null && property.aspect != null && currentEditContextModel == EditNewChildContextModel -> {
                            {
                                editModel.createProperty(property)
                                editContext.setContext(null)
                            }
                        }
                        property.id != null && apiModelProperty != null && currentEditContextModel is EditExistingContextModel &&
                                currentEditContextModel.identity == property.id &&
                                (apiModelProperty.name != property.name || apiModelProperty.aspectDescriptor.id != property.aspect?.id) -> {
                            {
                                editModel.updateProperty(property)
                                editContext.setContext(null)
                            }
                        }
                        else -> null
                    }
                    onCancel = if (property.id != null && apiModelPropertiesById[property.id]?.name != property.name &&
                        currentEditContextModel is EditExistingContextModel && currentEditContextModel.identity == property.id
                    ) {
                        {
                            updater(propertyIndex) {
                                name = apiModelPropertiesById[property.id]?.name
                            }
                            editContext.setContext(null)
                        }
                    } else null
                    onRemove = if (property.id != null && editContext.currentContext == null) {
                        { editModel.deleteProperty(property) }
                    } else null
                    onAddValue = if (property.id != null && editContext.currentContext == null) {
                        {
                            editContext.setContext(EditNewChildContextModel)
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
                    disabled = property.id != null && currentEditContextModel != null && currentEditContextModel != EditExistingContextModel(property.id)
                }
            }
        } else {
            val apiModelValuesById = apiModelPropertiesById[property.id]?.valueDescriptors?.associateBy { it.id } ?: emptyMap()
            propertyValues.forEachIndexed { valueIndex, value ->
                objectPropertyValueEditNode {
                    attrs {
                        val currentEditContextModel = editContext.currentContext
                        key = value.id ?: valueIndex.toString()
                        this.property = property
                        onPropertyUpdate = {
                            editContext.setContext(EditExistingContextModel(property.id ?: error("Property should have id != null")))
                            updater(propertyIndex, it)
                        }
                        onSaveProperty = if (property.name != apiModelPropertiesById[property.id]?.name) {
                            { editModel.updateProperty(property) }
                        } else null
                        onCancelProperty = if (property.name != apiModelPropertiesById[property.id]?.name) {
                            {
                                updater(propertyIndex) {
                                    name = apiModelPropertiesById[property.id]?.name
                                }
                            }
                        } else null
                        propertyDisabled = currentEditContextModel != null && currentEditContextModel !=
                                EditExistingContextModel(property.id ?: error("Property should have id != null"))
                        this.rootValue = value
                        onValueUpdate = when {
                            value.id != null && currentEditContextModel == null -> {
                                { block ->
                                    editContext.setContext(EditExistingContextModel(value.id))
                                    updater(propertyIndex) {
                                        values?.let {
                                            it[valueIndex].block()
                                        } ?: error("Must not be able to update value if there is no value")
                                    }
                                }
                            }
                            else -> {
                                { block ->
                                    updater(propertyIndex) {
                                        values?.let {
                                            it[valueIndex].block()
                                        } ?: error("Must not be able to update value if there is no value")
                                    }
                                }
                            }
                        }
                        onSaveValue = when {
                            value.id == null && value.value != null && currentEditContextModel == EditNewChildContextModel -> {
                                {
                                    editModel.createValue(
                                        value.value ?: error("value should not be null"),
                                        property.id ?: error("Property should have id != null"),
                                        null,
                                        null
                                    )
                                    editContext.setContext(null)
                                }
                            }
                            value.id != null && value.value != null && value.value != apiModelValuesById[value.id]?.value?.toData() &&
                                    currentEditContextModel is EditExistingContextModel && currentEditContextModel.identity == value.id -> {
                                {
                                    editModel.updateValue(
                                        value.id,
                                        property.id ?: error("Property should have id != null"),
                                        value.value ?: error("Value should not be null")
                                    )
                                    editContext.setContext(null)
                                }
                            }
                            else -> null
                        }
                        val allValues = property.values ?: error("Property should have at least one value")
                        onAddValue = when {
                            allValues.all { it.id != null } && allValues.none { apiModelValuesById[it.id]?.value?.toData() == ObjectValueData.NullValue } && currentEditContextModel == null -> {
                                {
                                    editContext.setContext(EditNewChildContextModel)
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
                            value.value == ObjectValueData.NullValue &&
                                    ((value.id == null && currentEditContextModel == EditNewChildContextModel) || (value.id != null && currentEditContextModel is EditExistingContextModel && currentEditContextModel.identity == value.id)) -> {
                                {
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value"))[valueIndex].value =
                                                property.aspect?.defaultValue()
                                    }
                                }
                            }
                            value.value == ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editContext.setContext(EditExistingContextModel(value.id ?: error("value should have id != null")))
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value"))[valueIndex].value = property.aspect?.defaultValue()
                                    }
                                }
                            }
                            else -> null
                        }
                        onCancelValue = when {
                            value.id == null && currentEditContextModel == EditNewChildContextModel -> {
                                {
                                    updater(propertyIndex) {
                                        (values ?: error("Must not be able to update value if there is no value")).removeAt(valueIndex)
                                    }
                                    editContext.setContext(null)
                                }
                            }
                            value.id != null && currentEditContextModel == EditExistingContextModel(value.id) && value.value != apiModelValuesById[value.id]?.value?.toData() -> {
                                {
                                    updater(propertyIndex) {
                                        val targetValue = (values ?: error("Must not be able to update value if there is no value"))[valueIndex]
                                        targetValue.value = apiModelValuesById[value.id]?.value?.toData()
                                    }
                                    editContext.setContext(null)
                                }
                            }
                            else -> null
                        }
                        onRemoveValue = when {
                            value.id != null && allValues.size > 1 && currentEditContextModel == null -> {
                                {
                                    editModel.deleteValue(value.id, property.id ?: error("Property should have id"))
                                }
                            }
                            value.id != null && value.value != ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.updateValue(value.id, property.id ?: error("Property should have id"), ObjectValueData.NullValue)
                                }
                            }
                            value.id != null && value.value == ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.deleteProperty(property)
                                }
                            }
                            else -> null
                        }
                        valueDisabled = currentEditContextModel != null && value.id != null && currentEditContextModel != EditExistingContextModel(value.id)
                        this.editModel = editModel
                        this.editContext = editContext
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
                        onCancel = props.onCancel
                        onAddValue = props.onAddValue
                        onRemoveProperty = props.onRemove
                        disabled = props.disabled
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
    var onCancel: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemove: (() -> Unit)?
    var disabled: Boolean
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
                        onSaveProperty = props.onSaveProperty
                        onCancelProperty = props.onCancelProperty
                        needRemoveConfirmation = props.rootValue.id != null
                        valueDisabled = props.valueDisabled
                        propertyDisabled = props.propertyDisabled
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
                apiModelValuesById = props.apiModelValuesById,
                editContext = props.editContext
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
    var onSaveProperty: (() -> Unit)?
    var onCancelProperty: (() -> Unit)?
    var editModel: ObjectTreeEditModel
    var apiModelValuesById: Map<String, ValueTruncated>
    var editContext: EditContext
    var valueDisabled: Boolean
    var propertyDisabled: Boolean
}

fun AspectTree.defaultValue(): ObjectValueData? {
    val baseType = baseType?.let { BaseType.valueOf(it) } ?: measure?.let { GlobalMeasureMap[it]?.baseType } ?: throw IllegalStateException("Aspect can not infer its base type: ${this}")
    return baseType.defaultValue()
}