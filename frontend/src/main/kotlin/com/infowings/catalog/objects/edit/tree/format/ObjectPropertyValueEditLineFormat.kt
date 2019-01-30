package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.MeasureMeasureGroupMap
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.guid.copyGuidButton
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
import com.infowings.catalog.objects.edit.tree.inputs.valueMeasureSelect
import com.infowings.catalog.wrappers.blueprint.Icon
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyValueEditLineFormat = rFunction<ObjectPropertyValueEditLineFormatProps>("ObjectPropertyValueEditLineFormat") { props ->

    val toMark = props.highlightedGuid == props.valueGuid && props.valueGuid != null && !props.editMode

    if(toMark) {
        span(classes = "object-line__edit-link bp3-button pt-intent-primary bp3-minimal bp3-small") {
            +""
            Icon {
                attrs {
                    icon = "fast-forward"
                }
            }
        }
    }

    div(classes = "object-tree-edit__property-value") {
        name(
            className = "property-value__property-name",
            value = props.propertyName ?: "",
            onChange = props.onPropertyNameUpdate,
            onCancel = props.onPropertyNameUpdate,
            disabled = !props.editMode || props.propertyDisabled
        )
        span(classes = "property-value__aspect") {
            +props.aspectName
        }
        span(classes = "property-value__aspect-subject") {
            +"("
            +(props.subjectName ?: "Global")
            +")"
        }
        if (!props.editMode || props.propertyDisabled) {
            descriptionComponent(
                className = "object-input-description",
                description = props.propertyDescription
            )
        } else {
            descriptionComponent(
                className = "object-input-description",
                description = props.propertyDescription,
                onNewDescriptionConfirmed = props.onPropertyDescriptionChanged,
                onEditStarted = null
            )
        }
        props.onSaveProperty?.let {
            submitButtonComponent(it, "bp3-small")
        }
        props.onCancelProperty?.let {
            cancelButtonComponent(it, "bp3-small")
        }
        val value = props.value
        if (value != ObjectValueData.NullValue) {
            propertyValue(
                baseType = props.aspectBaseType,
                referenceBookId = props.referenceBookId,
                referenceBookNameSoft = props.referenceBookNameSoft,
                value = value,
                onChange = props.onValueUpdate,
                disabled = !props.editMode || props.valueDisabled
            )
            props.aspectMeasure?.let {
                val measureGroup = MeasureMeasureGroupMap[it.name] ?: error("No measure group for measure ${it.name}")
                if (measureGroup.measureList.size == 1) {
                    span(classes = "property-value__aspect-measure") {
                        +it.symbol
                    }
                } else {
                    val decimal = value as? ObjectValueData.DecimalValue ?: error("Value has non-decimal type and has non-null measure")
                    valueMeasureSelect(
                        measureGroup = measureGroup,
                        value = decimal,
                        currentMeasure = props.valueMeasure ?: error("Value has no assigned measure"),
                        onMeasureSelected = { measure, recalculated ->
                            props.onValueMeasureNameChanged(measure.name, recalculated)
                        },
                        disabled = !props.editMode || props.valueDisabled
                    )
                }
            }
        }
        if (!props.editMode || props.valueDisabled) {
            descriptionComponent(
                className = "object-input-description",
                description = props.valueDescription
            )
        } else {
            descriptionComponent(
                className = "object-input-description",
                description = props.valueDescription,
                onNewDescriptionConfirmed = props.onValueDescriptionChanged,
                onEditStarted = null
            )
        }
        copyGuidButton(props.valueGuid)

        if(toMark) {
            span(classes = "object-line__edit-link bp3-button pt-intent-primary bp3-minimal bp3-small") {
                +""
                Icon {
                    attrs {
                        icon = "fast-backward"
                    }
                }
            }
        }

        props.onAddValue?.let {
            if (props.editMode) {
                plusButtonComponent(it, "bp3-small")
            }
        }
        props.onRemoveValue?.let {
            if (props.editMode) {
                minusButtonComponent(it, props.needRemoveConfirmation, "bp3-small")
            }
        }
        props.onSaveValue?.let {
            submitButtonComponent(it, "bp3-small")
        }
        props.onCancelValue?.let {
            cancelButtonComponent(it, "bp3-small")
        }
    }
}

interface ObjectPropertyValueEditLineFormatProps : RProps {
    var propertyName: String?
    var propertyDescription: String?
    var onPropertyDescriptionChanged: (String) -> Unit
    var aspectName: String
    var aspectBaseType: BaseType
    var aspectMeasure: Measure<*>?
    var subjectName: String?
    var referenceBookId: String?
    var referenceBookNameSoft: String?
    var value: ObjectValueData?
    var valueMeasure: Measure<*>?
    var valueDescription: String?
    var valueGuid: String?
    var onValueDescriptionChanged: (String) -> Unit
    var onPropertyNameUpdate: (String) -> Unit
    var onValueUpdate: (ObjectValueData) -> Unit
    var onValueMeasureNameChanged: (String, ObjectValueData) -> Unit
    var onSaveValue: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onCancelValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
    var needRemoveConfirmation: Boolean
    var onSaveProperty: (() -> Unit)?
    var onCancelProperty: (() -> Unit)?
    var propertyDisabled: Boolean
    var valueDisabled: Boolean
    var editMode: Boolean
    var highlightedGuid: String?
}
