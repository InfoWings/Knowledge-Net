package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.*
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.guid.copyGuidButton
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
                referenceBookNameSoft = "",
                value = value,
                onChange = props.onChange,
                disabled = !props.editMode || props.disabled
            )
            props.aspectMeasure?.let {
                val measureGroup = MeasureMeasureGroupMap[it.name] ?: error("No measure group for measure ${it.name}")
                if (measureGroup.measureList.size == 1) {
                    span(classes = "aspect-property__property-measure") {
                        +it.symbol
                    }
                } else {
                    val decimal = value as? ObjectValueData.DecimalValue ?: error("Value has non-decimal type and has non-null measure")

                    valueMeasureSelect(
                        measureGroup = measureGroup,
                        value = decimal,
                        currentMeasure = props.valueMeasure ?: error("Value has no assigned measure"),
                        onMeasureSelected = { measure, recalculated ->
                            props.onMeasureNameChanged(measure.name, recalculated)
                        },
                        disabled = props.disabled || !props.editMode
                    )
                }
            }
        }
        if (!props.editMode || props.disabled) {
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
        copyGuidButton(props.valueGuid)
        props.onSubmit?.let {
            submitButtonComponent(it, "pt-small")
        }
        props.onCancel?.let {
            cancelButtonComponent(it, "pt-small")
        }
        props.onAddValue?.let {
            if (props.editMode) {
                plusButtonComponent(it, "pt-small")
            }
        }
        props.onRemoveValue?.let {
            if (props.editMode) {
                minusButtonComponent(it, props.needRemoveConfirmation, "pt-small")
            }
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
    var valueGuid: String?
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
    var editMode: Boolean
}
