package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.*
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
import com.infowings.catalog.objects.edit.tree.inputs.valueMeasureSelect
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val aspectPropertyEditLineFormat = rFunction<AspectPropertyEditLineFormatProps>("AspectPropertyEditLineFormat") { props ->
    div("object-tree-edit__aspect-property") {
        val propertyName = props.propertyName
        if (!propertyName.isNullOrBlank()) {
            span(classes = "aspect-property__property-name text-bold text-italic") {
                +propertyName!!
            }
        }
        span(classes = "aspect-property__aspect-name text-bold") {
            +props.aspectName
        }
        span(classes = "aspect-property__aspect-subject") {
            +"("
            +(props.subjectName ?: "Global")
            +")"
        }
        span(classes = "aspect-property__property-cardinality--${if (props.conformsToCardinality) "conforms" else "conforms-not"}") {
            +"["
            +props.recommendedCardinality.label
            +"]"
        }
        val value = props.value
        if (value != ObjectValueData.NullValue) {
            propertyValue(
                baseType = props.aspectBaseType,
                referenceBookId = props.aspectReferenceBookId,
                value = value,
                onChange = props.onChange,
                disabled = props.disabled
            )
            props.aspectMeasure?.let {
                val measureGroup = MeasureMeasureGroupMap[it.name] ?: error("No measure group for measure ${it.name}")
                if (measureGroup.measureList.size == 1) {
                    span(classes = "aspect-property__property-measure") {
                        +it.symbol
                    }
                } else {
                    valueMeasureSelect(
                        measureGroup = measureGroup,
                        stringValueRepresentation = (props.value as ObjectValueData.DecimalValue).valueRepr,
                        currentMeasure = props.valueMeasure ?: error("Value has no assigned measure"),
                        onMeasureSelected = { measure, stringValueRepresentation ->
                            props.onMeasureNameChanged(measure.name, ObjectValueData.DecimalValue(stringValueRepresentation))
                        },
                        disabled = props.disabled
                    )
                }
            }
        }
        if (props.disabled) {
            descriptionComponent(
                className = "object-input-description",
                description = props.valueDescription
            )
        } else {
            descriptionComponent(
                className = "object-input-description",
                description = props.valueDescription,
                onNewDescriptionConfirmed = props.onDescriptionChange,
                onEditStarted = null
            )
        }
        props.onSubmit?.let {
            submitButtonComponent(it, "pt-small")
        }
        props.onCancel?.let {
            cancelButtonComponent(it, "pt-small")
        }
        props.onAddValue?.let {
            plusButtonComponent(it, "pt-small")
        }
        props.onRemoveValue?.let {
            minusButtonComponent(it, props.needRemoveConfirmation, "pt-small")
        }
    }
}

interface AspectPropertyEditLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var aspectBaseType: BaseType
    var aspectReferenceBookId: String?
    var aspectMeasure: Measure<*>?
    var subjectName: String?
    var recommendedCardinality: PropertyCardinality
    var value: ObjectValueData?
    var valueMeasure: Measure<*>?
    var onChange: (ObjectValueData) -> Unit
    var onMeasureNameChanged: (String?, ObjectValueData) -> Unit
    var valueDescription: String?
    var onDescriptionChange: (String) -> Unit
    var conformsToCardinality: Boolean
    var onSubmit: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
    var needRemoveConfirmation: Boolean
    var disabled: Boolean
}
