package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.MeasureMeasureGroupMap
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.name
import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
import com.infowings.catalog.objects.edit.tree.inputs.valueMeasureSelect
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyValueEditLineFormat = rFunction<ObjectPropertyValueEditLineFormatProps>("ObjectPropertyValueEditLineFormat") { props ->
    div(classes = "object-tree-edit__property-value") {
        name(
            className = "property-value__property-name",
            value = props.propertyName ?: "",
            onChange = props.onPropertyNameUpdate,
            onCancel = props.onPropertyNameUpdate,
            disabled = props.propertyDisabled
        )
        span(classes = "property-value__aspect") {
            +props.aspectName
        }
        span(classes = "property-value__aspect-subject") {
            +"("
            +(props.subjectName ?: "Global")
            +")"
        }
        if (props.propertyDisabled) {
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
            submitButtonComponent(it, "pt-small")
        }
        props.onCancelProperty?.let {
            cancelButtonComponent(it, "pt-small")
        }
        val value = props.value
        if (value != ObjectValueData.NullValue) {
            propertyValue(
                baseType = props.aspectBaseType,
                referenceBookId = props.referenceBookId,
                value = value,
                onChange = props.onValueUpdate,
                disabled = props.valueDisabled
            )
            props.aspectMeasure?.let {
                val measureGroup = MeasureMeasureGroupMap[it.name] ?: error("No measure group for measure ${it.name}")
                if (measureGroup.measureList.size == 1) {
                    span(classes = "property-value__aspect-measure") {
                        +it.symbol
                    }
                } else {
                    valueMeasureSelect(
                        measureGroup = measureGroup,
                        stringValueRepresentation = (value as? ObjectValueData.DecimalValue)?.valueRepr
                                ?: error("Value has non-decimal type and has non-null measure"),
                        currentMeasure = props.valueMeasure ?: error("Value has no assigned measure"),
                        onMeasureSelected = { measure, stringValueRepresentation ->
                            props.onValueMeasureNameChanged(measure.name, ObjectValueData.DecimalValue(stringValueRepresentation))
                        },
                        disabled = props.valueDisabled
                    )
                }
            }
        }
        if (props.valueDisabled) {
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
        props.onAddValue?.let {
            plusButtonComponent(it, "pt-small")
        }
        props.onRemoveValue?.let {
            minusButtonComponent(it, props.needRemoveConfirmation, "pt-small")
        }
        props.onSaveValue?.let {
            submitButtonComponent(it, "pt-small")
        }
        props.onCancelValue?.let {
            cancelButtonComponent(it, "pt-small")
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
    var value: ObjectValueData?
    var valueMeasure: Measure<*>?
    var valueDescription: String?
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
}
