package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.AspectPropertyValueEditModel
import com.infowings.catalog.objects.AspectPropertyValueGroupEditModel
import com.infowings.catalog.objects.edit.EditContext
import com.infowings.catalog.objects.edit.EditExistingContextModel
import com.infowings.catalog.objects.edit.EditNewChildContextModel
import com.infowings.catalog.objects.edit.ObjectTreeEditModel
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
    onRemoveGroup: (id: String) -> Unit,
    editModel: ObjectTreeEditModel,
    objectPropertyId: String,
    apiModelValuesById: Map<String, ValueTruncated>,
    editContext: EditContext
) {
    val groupsMap = valueGroups.associateBy { it.propertyId }
    aspect.properties.forEach { aspectProperty ->
        val valueGroup = groupsMap[aspectProperty.id]

        if (valueGroup == null) { // Не может быть пустой (#isEmpty()), так как мы удаляем всю группу при удалении единственного значения
            if (!aspectProperty.deleted && !aspectProperty.aspect.deleted) {
                aspectPropertyValueCreateNode {
                    attrs {
                        this.aspectProperty = aspectProperty
                        this.onCreateValue = if (editContext.currentContext == null) {
                            { valueData ->
                                editContext.setContext(EditNewChildContextModel)
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
                        } else null
                    }
                }
            }
        } else {
            val valueGroupIndex = valueGroups.indexOfFirst { it.propertyId == valueGroup.propertyId }
            valueGroup.values.forEachIndexed { valueIndex, value ->
                aspectPropertyValueEditNode {
                    attrs {
                        val currentEditContextModel = editContext.currentContext
                        this.aspectProperty = aspectProperty
                        this.value = value
                        this.valueCount = valueGroup.values.size
                        this.onUpdate = { block ->
                            onUpdate(valueGroupIndex) {
                                values[valueIndex].block()
                            }
                        }
                        this.onSubmit = when {
                            value.id == null && value.value != null && currentEditContextModel == EditNewChildContextModel -> {
                                {
                                    editModel.createValue(
                                        value.value ?: error("No value to submit"),
                                        value.description,
                                        objectPropertyId,
                                        parentValueId,
                                        aspectProperty.id
                                    )
                                    editContext.setContext(null)
                                }
                            }
                            value.id != null && value.value != null && currentEditContextModel == EditExistingContextModel(value.id) -> {
                                {
                                    editModel.updateValue(
                                        value.id,
                                        value.value ?: error("No value to submit"),
                                        value.description,
                                        value.version ?: error("Value with id (${value.id}) has no version")
                                    )
                                    editContext.setContext(null)
                                }
                            }
                            else -> null
                        }
                        this.onCancel = when {
                            value.id == null && valueGroup.values.size == 1 && currentEditContextModel == EditNewChildContextModel -> {
                                {
                                    onRemoveGroup(valueGroup.propertyId)
                                    editContext.setContext(null)
                                }
                            }
                            value.id == null && valueGroup.values.size > 1 && currentEditContextModel == EditNewChildContextModel -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values.removeAt(valueIndex)
                                    }
                                    editContext.setContext(null)
                                }
                            }
                            value.id != null && currentEditContextModel == EditExistingContextModel(value.id) -> {
                                {
                                    onUpdate(valueGroupIndex) {
                                        values[valueIndex].value = apiModelValuesById[value.id]?.value?.toData()
                                    }
                                    editContext.setContext(null)
                                }
                            }
                            else -> null
                        }
                        this.onAddValue = when {
                            value.id != null && value.value == ObjectValueData.NullValue && currentEditContextModel == null && !aspectProperty.deleted && !aspectProperty.aspect.deleted -> {
                                {
                                    editContext.setContext(EditExistingContextModel(value.id))
                                    onUpdate(valueGroupIndex) {
                                        values[valueIndex].value = aspectProperty.aspect.defaultValue()
                                    }
                                }
                            }
                            valueGroup.values.all { it.id != null } && valueGroup.values.none { apiModelValuesById[it.id]?.value?.toData() == ObjectValueData.NullValue } && currentEditContextModel == null && !aspectProperty.deleted && !aspectProperty.aspect.deleted -> {
                                {
                                    editContext.setContext(EditNewChildContextModel)
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
                            value.id != null && valueGroup.values.size > 1 && currentEditContextModel == null -> {
                                {
                                    editModel.deleteValue(value.id, objectPropertyId)
                                }
                            }
                            value.id != null && value.value != ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.updateValue(
                                        value.id,
                                        ObjectValueData.NullValue,
                                        value.description,
                                        value.version ?: error("Value with id (${value.id}) has no version")
                                    )
                                }
                            }
                            value.id != null && value.value == ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.deleteValue(value.id, objectPropertyId)
                                }
                            }
                            else -> null
                        }
                        disabled = (value.id != null && currentEditContextModel != null && currentEditContextModel != EditExistingContextModel(value.id)) ||
                                aspectProperty.deleted || aspectProperty.aspect.deleted
                        this.editModel = editModel
                        this.objectPropertyId = objectPropertyId
                        this.apiModelValuesById = apiModelValuesById
                        this.editContext = editContext
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
                        onCreateValue = props.onCreateValue?.let { onCreateValue ->
                            if (props.aspectProperty.cardinality == PropertyCardinality.ZERO) {
                                { onCreateValue(ObjectValueData.NullValue) }
                            } else {
                                { onCreateValue(props.aspectProperty.aspect.defaultValue()) }
                            }
                        }
                    }
                }
            }!!
        }
    }
}

interface AspectPropertyValueCreateNodeProps : RProps {
    var aspectProperty: AspectPropertyTree
    var onCreateValue: ((ObjectValueData?) -> Unit)?
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
                        aspectBaseType = aspect.baseType?.let { BaseType.valueOf(it) }
                                ?: aspect.measure?.let { GlobalMeasureMap[it]?.baseType }
                                ?: throw IllegalStateException("Aspect can not infer its base type")
                        aspectReferenceBookId = aspect.refBookId
                        aspectMeasure = aspect.measure?.let { GlobalMeasureMap[it] }
                        subjectName = aspect.subjectName
                        value = props.value.value
                        valueDescription = props.value.description
                        onChange = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.value.id ?: error("Value should have id != null in order to be edited")
                                    )
                                )
                                props.onUpdate {
                                    value = it
                                }
                            }
                        } else {
                            {
                                props.onUpdate {
                                    value = it
                                }
                            }
                        }
                        onDescriptionChange = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.value.id ?: error("Value should have id != null in order to be edited")
                                    )
                                )
                                props.onUpdate {
                                    description = it
                                }
                            }
                        } else {
                            {
                                props.onUpdate {
                                    description = it
                                }
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
                        needRemoveConfirmation = props.value.id != null
                        disabled = props.disabled
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
                onRemoveGroup = { id ->
                    props.onUpdate {
                        val removeIndex = children.indexOfFirst { it.propertyId == id }
                        children.removeAt(removeIndex)
                    }
                },
                editModel = props.editModel,
                objectPropertyId = props.objectPropertyId,
                apiModelValuesById = props.apiModelValuesById,
                editContext = props.editContext
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
    var editModel: ObjectTreeEditModel
    var objectPropertyId: String
    var apiModelValuesById: Map<String, ValueTruncated>
    var editContext: EditContext
    var disabled: Boolean
}

