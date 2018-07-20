package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
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
                onChange = props.onChange
            )
            props.aspectMeasure?.let {
                span(classes = "aspect-property__property-measure") {
                    +it.symbol
                }
            }
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
    var onChange: (ObjectValueData) -> Unit
    var conformsToCardinality: Boolean
    var onSubmit: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
    var needRemoveConfirmation: Boolean
}
