package com.infowings.catalog.objects.edit.tree

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.common.objekt.ValueUpdateRequest
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.*
import com.infowings.catalog.objects.edit.tree.format.objectPropertyEditLineFormat
import com.infowings.catalog.objects.edit.tree.format.objectPropertyValueEditLineFormat
import com.infowings.catalog.objects.edit.tree.utils.InputValidationException
import com.infowings.catalog.objects.edit.tree.utils.transform
import com.infowings.catalog.objects.edit.tree.utils.validate
import com.infowings.catalog.utils.BadRequestException
import react.RBuilder
import react.RProps
import react.buildElement
import react.rFunction

fun modelSaver(editContext: EditContext, value: ObjectPropertyValueEditModel, valueSaver: (ObjectValueData) -> Unit) {
    try {
        value.value?.validate()
        val transformed = (value.value ?: error("value should not be null")).transform()
        valueSaver(transformed)
        editContext.setContext(null)
    } catch (e: InputValidationException) {
        showError(BadRequestException(e.message ?: "invalid input", 400.0))
    }
}

data class ValueSlot(val oPropertyId: String, val oPropertyName: String?, val aPropertyId: String, val pos: Int) {
    companion object {
        val New = ValueSlot(oPropertyId = "", oPropertyName = null, aPropertyId = "", pos = -1)
    }
}

fun List<ObjectPropertyEditModel>.freeSlots(): List<ValueSlot> {
    val oPropsWithAProps = this.filter {
        val aspect = it.aspect
        aspect != null && aspect.properties.isNotEmpty()
    }

    return oPropsWithAProps.flatMap { oProp ->
        oProp.id ?: throw IllegalStateException("object property id is null")
        oProp.aspect ?: throw IllegalStateException("object property aspect is null")
        val aProps = oProp.aspect!!.properties.map { it.id }.toSet()

        oProp.values?.mapIndexed { index, ovalue ->
            val usedAspectProps = ovalue.valueGroups.map { it.propertyId }
            (aProps - usedAspectProps).map { ValueSlot(aPropertyId = it, pos = index, oPropertyId = oProp.id, oPropertyName = oProp.name) }
        }?.flatten() ?: emptyList()
    }
}

fun RBuilder.objectPropertiesEditList(
    editContext: EditContext,
    properties: List<ObjectPropertyEditModel>,
    editModel: ObjectTreeEditModel,
    apiModelProperties: List<ObjectPropertyEditDetailsResponse>,
    updater: (index: Int, ObjectPropertyEditModel.() -> Unit) -> Unit,
    remover: (index: Int) -> Unit,
    newEditMode: Boolean,
    newHighlightedGuid: String?
) {
    val apiModelPropertiesById = apiModelProperties.associateBy { it.id }

    val freeSlots = properties.freeSlots() //.groupBy { it.aPropertyId }

    properties.forEachIndexed { propertyIndex, property ->
        val propertyValues = property.values

        if (propertyValues == null || propertyValues.isEmpty()) {
            objectPropertyEditNode {
                val apiModelProperty = property.id?.let { apiModelPropertiesById[it] }
                attrs {
                    val currentEditContextModel = editContext.currentContext
                    this.property = property
                    onUpdate = { block -> updater(propertyIndex, block) }
                    onConfirm = when {
                        property.id == null && property.aspect != null && currentEditContextModel == EditNewChildContextModel -> {
                            {
                                val selected = property.selectedProp
                                if (selected != null) {
                                    val selectedSlot = property.selectedSlot
                                    if (property.selectedSlot == ValueSlot.New || selectedSlot == null) {
                                        editModel.createProperty(property, selected.id)
                                    } else {
                                        val oProperty = properties.filter { it.id == selectedSlot.oPropertyId }.single()
                                        val parentIds = oProperty.values?.map { it.id }

                                        if (parentIds != null) {
                                            editModel.createValue(
                                                ValueCreateRequest(
                                                    value = ObjectValueData.NullValue,
                                                    description = "",
                                                    objectPropertyId = selectedSlot.oPropertyId,
                                                    measureName = null,
                                                    aspectPropertyId = selectedSlot.aPropertyId,
                                                    parentValueId = parentIds[selectedSlot.pos]
                                                )
                                            )
                                        }

                                    }
                                } else {
                                    editModel.createProperty(property, null)
                                }

                                editContext.setContext(null)
                            }
                        }
                        property.id != null && apiModelProperty != null && currentEditContextModel == EditExistingContextModel(property.id) -> {
                            {
                                editModel.updateProperty(property)
                                editContext.setContext(null)
                            }
                        }
                        else -> null
                    }
                    onCancel = if (property.id != null && currentEditContextModel == EditExistingContextModel(property.id)) {
                        {
                            updater(propertyIndex) {
                                name = apiModelPropertiesById[property.id]?.name
                            }
                            editContext.setContext(null)
                        }
                    } else null
                    onCancelProperty = if (property.id == null) {
                        {
                            editContext.setContext(null)
                            remover(propertyIndex)
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
                                        this.values = mutableListOf(ObjectPropertyValueEditModel.nullValue())
                                    }
                                    currentValues.isEmpty() -> {
                                        currentValues.add(ObjectPropertyValueEditModel.nullValue())
                                    }
                                }
                            }
                        }
                    } else null
                    this.editContext = editContext
                    disabled = !newEditMode ||
                            (property.id != null && currentEditContextModel != null && currentEditContextModel != EditExistingContextModel(property.id))
                    editMode = newEditMode
                    freeValueSlots = freeSlots
                }
            }
        } else {
            val apiModelValuesById = apiModelPropertiesById[property.id]?.valueDescriptors?.associateBy { it.id } ?: emptyMap()

            propertyValues.forEachIndexed { valueIndex, value ->
                objectPropertyValueEditNode {
                    attrs {
                        val currentEditContextModel = editContext.currentContext
                        val propertyId = property.id ?: error("Property should not have id != null")
                        key = value.id ?: valueIndex.toString()
                        this.property = property
                        onPropertyUpdate = { updater(propertyIndex, it) }
                        onSaveProperty = if (currentEditContextModel == EditExistingContextModel(propertyId)) {
                            {
                                editModel.updateProperty(property)
                                editContext.setContext(null)
                            }
                        } else null
                        onCancelProperty = if (currentEditContextModel == EditExistingContextModel(propertyId)) {
                            {
                                updater(propertyIndex) {
                                    name = apiModelPropertiesById[property.id]?.name
                                }
                                editContext.setContext(null)
                            }
                        } else null
                        propertyDisabled = currentEditContextModel != null && currentEditContextModel != EditExistingContextModel(propertyId)
                        this.rootValue = value
                        onValueUpdate = { block ->
                            updater(propertyIndex) {
                                values?.let {
                                    it[valueIndex].block()
                                } ?: error("Must not be able to update value if there is no value")
                            }
                        }

                        onSaveValue = when {
                            value.id == null && value.value != null && (currentEditContextModel == EditNewChildContextModel) -> {
                                {
                                    modelSaver(editContext, value) { dataValue ->
                                        editModel.createValue(ValueCreateRequest(dataValue, value.description, propertyId, value.measureName))
                                    }
                                }
                            }
                            value.id != null && value.value != null && currentEditContextModel == EditExistingContextModel(value.id) -> {
                                {
                                    modelSaver(editContext, value) { dataValue ->
                                        editModel.updateValue(
                                            ValueUpdateRequest(
                                                value.id, dataValue, value.measureName, value.description,
                                                value.version ?: error("Value with id (${value.id}) should have non null version")
                                            )
                                        )

                                    }
                                }
                            }
                            else -> null
                        }

                        val allValues = property.values ?: error("Property should have at least one value")
                        onAddValue = when {
                            allValues.all { it.id != null } && allValues.none { apiModelValuesById[it.id]?.value?.toData() == ObjectValueData.NullValue } &&
                                    currentEditContextModel == null && !(property.aspect?.deleted ?: true) -> {
                                {
                                    editContext.setContext(EditNewChildContextModel)
                                    updater(propertyIndex) {
                                        values?.add(
                                            ObjectPropertyValueEditModel(
                                                null,
                                                null,
                                                property.aspect?.defaultValue(),
                                                property.aspect?.measure,
                                                null,
                                                null,
                                                false,
                                                mutableListOf()
                                            )
                                        )
                                    }
                                }
                            }
                            value.value == ObjectValueData.NullValue && !(property.aspect?.deleted ?: true) &&
                                    isValueBeingEdited(value.id, currentEditContextModel) -> {
                                {
                                    updater(propertyIndex) {
                                        val targetValue = (values ?: error("Must not be able to update value if there is no value"))[valueIndex]
                                        targetValue.value = property.aspect?.defaultValue()
                                        targetValue.measureName = property.aspect?.measure
                                    }
                                }
                            }
                            value.value == ObjectValueData.NullValue && currentEditContextModel == null && !(property.aspect?.deleted ?: true) -> {
                                {
                                    editContext.setContext(EditExistingContextModel(value.id ?: error("value should have id != null")))
                                    updater(propertyIndex) {
                                        val targetValue = (values ?: error("Must not be able to update value if there is no value"))[valueIndex]
                                        targetValue.value = property.aspect?.defaultValue()
                                        targetValue.measureName = property.aspect?.measure
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
                            value.id != null && currentEditContextModel == EditExistingContextModel(value.id) -> {
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
                                    editModel.deleteValue(value.id)
                                }
                            }
                            value.id != null && value.value != ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.deleteValue(value.id)
                                }
                            }
                            value.id != null && value.value == ObjectValueData.NullValue && currentEditContextModel == null -> {
                                {
                                    editModel.deleteProperty(property)
                                }
                            }
                            else -> null
                        }
                        valueDisabled =
                            (currentEditContextModel != null && value.id != null && currentEditContextModel != EditExistingContextModel(value.id)) ||
                                    property.aspect?.deleted ?: true
                        this.editModel = editModel
                        this.editContext = editContext
                        this.apiModelValuesById = apiModelValuesById
                        editMode = newEditMode
                        highlightedGuid = newHighlightedGuid
                    }
                }
            }
        }
    }
}

private fun isValueBeingEdited(valueId: String?, editContextModel: EditContextModel?) =
    valueId == null && editContextModel == EditNewChildContextModel || valueId != null && editContextModel == EditExistingContextModel(valueId)

val objectPropertyEditNode = rFunction<ObjectPropertyEditNodeProps>("ObjectPropertyEditNode") { props ->
    controlledTreeNode {
        attrs {
            expanded = props.property.id != null && props.property.expanded
            onExpanded = {
                props.onUpdate { expanded = it }
            }
            treeNodeContent = buildElement {
                objectPropertyEditLineFormat {
                    attrs {
                        name = props.property.name // "${props.property.name}${props.property.propId ?.let { ":" + it } ?: "" }"
                        aspect = props.property.aspect
                        selectedProp = props.property.selectedProp
                        selectedSlot = props.property.selectedSlot
                        valueSlots = props.freeValueSlots
                        description = props.property.description
                        onNameChanged = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.property.id ?: error("Property should have id != null in order to edit")
                                    )
                                )
                                props.onUpdate {
                                    name = it
                                }
                            }
                        } else {
                            {
                                props.onUpdate {
                                    name = it
                                }
                            }
                        }
                        onAspectChanged = if (props.editContext.currentContext == null) {
                            { aspectTree, selected ->
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.property.id ?: error("Property should have id != null in order to edit")
                                    )
                                )
                                props.onUpdate {
                                    aspect = aspectTree
                                    this.selectedProp = selected
                                }
                            }
                        } else {
                            { aspectTree, selected ->
                                props.onUpdate {
                                    aspect = aspectTree
                                    this.selectedProp = selected
                                }
                            }
                        }
                        onSlotChanged = {
                            //   var onSlotChanged: (valueSlot: ValueSlot?) -> Unit
                            props.onUpdate {
                                selectedSlot = it
                            }
                        }

                        onDescriptionChanged = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.property.id ?: error("Property should have id != null in order to edit")
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
                        onReselectProperty = {
                            props.onUpdate {
                                selectedProp = null
                                selectedSlot = null
                                aspect = null
                            }
                        }
                        onConfirmCreate = props.onConfirm
                        onCancel = props.onCancel
                        onCancelProperty = props.onCancelProperty
                        onAddValue = props.onAddValue
                        onRemoveProperty = props.onRemove
                        disabled = props.disabled
                        editMode = props.editMode
                        valueSlots = props.freeValueSlots
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
    var onCancelProperty: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemove: (() -> Unit)?
    var disabled: Boolean
    var editContext: EditContext
    var editMode: Boolean
    var freeValueSlots: List<ValueSlot>
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
                        propertyDescription = props.property.description
                        aspectName = aspect.name
                        aspectBaseType = aspect.baseType?.let { BaseType.valueOf(it) } ?: aspect.measure?.let { GlobalMeasureMap[it]?.baseType }
                                ?: throw IllegalStateException("Aspect can not infer its base type")
                        aspectMeasure = aspect.measure?.let { GlobalMeasureMap[it] }
                        subjectName = aspect.subjectName
                        referenceBookId = aspect.refBookId
                        referenceBookNameSoft = aspect.refBookNameSoft
                        value = props.rootValue.value
                        valueMeasure = props.rootValue.measureName?.let { GlobalMeasureMap[it] }
                        valueDescription = props.rootValue.description
                        valueGuid = props.rootValue.guid
                        onPropertyNameUpdate = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.property.id ?: error("Property should have id != null in order to edit")
                                    )
                                )
                                props.onPropertyUpdate {
                                    name = it
                                }
                            }
                        } else {
                            {
                                props.onPropertyUpdate {
                                    name = it
                                }
                            }
                        }
                        onPropertyDescriptionChanged = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.property.id ?: error("Property should have id != null in order to edit")
                                    )
                                )
                                props.onPropertyUpdate {
                                    description = it
                                }
                            }
                        } else {
                            {
                                props.onPropertyUpdate {
                                    description = it
                                }
                            }
                        }
                        onValueUpdate = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.rootValue.id ?: error("Root value should have id != null in order to edit")
                                    )
                                )
                                props.onValueUpdate {
                                    value = it
                                }
                            }
                        } else {
                            {
                                props.onValueUpdate {
                                    value = it
                                }
                            }
                        }
                        onValueMeasureNameChanged = if (props.editContext.currentContext == null) {
                            { newMeasureName, objectValueData ->

                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.rootValue.id ?: error("Root value should have id != null in order to edit")
                                    )
                                )
                                props.onValueUpdate {
                                    measureName = newMeasureName
                                    value = objectValueData
                                }
                            }
                        } else {
                            { newMeasureName, objectValueData ->
                                props.onValueUpdate {
                                    measureName = newMeasureName
                                    value = objectValueData
                                }
                            }
                        }
                        onValueDescriptionChanged = if (props.editContext.currentContext == null) {
                            {
                                props.editContext.setContext(
                                    EditExistingContextModel(
                                        props.rootValue.id ?: error("Root value should have id != null in order to edit")
                                    )
                                )
                                props.onValueUpdate {
                                    description = it
                                }
                            }
                        } else {
                            {
                                props.onValueUpdate {
                                    description = it
                                }
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
                        editMode = props.editMode
                        highlightedGuid = props.highlightedGuid
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
                editContext = props.editContext,
                editMode = props.editMode
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
    var editMode: Boolean
    var highlightedGuid: String?
}

fun AspectTree.defaultValue(): ObjectValueData? {
    val baseType = baseType?.let { BaseType.valueOf(it) } ?: measure?.let { GlobalMeasureMap[it]?.baseType }
    ?: throw IllegalStateException("Aspect can not infer its base type: ${this}")
    return baseType.defaultValue()
}